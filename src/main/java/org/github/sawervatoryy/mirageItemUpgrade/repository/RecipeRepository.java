package org.github.sawervatoryy.mirageItemUpgrade.repository;

import org.github.sawervatoryy.mirageItemUpgrade.model.UpgradeRecipe;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class RecipeRepository {

    private static final Comparator<String> ID_ORDER =
            Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());

    private final ComponentLogger log;
    private final Path file;
    private final ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    private final ReentrantLock fileLock = new ReentrantLock();
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private final Map<String, UpgradeRecipe> recipes = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean loaded = new AtomicBoolean();

    public RecipeRepository(JavaPlugin plugin) {
        this.log = plugin.getComponentLogger();
        this.file = plugin.getDataFolder().toPath().resolve("recipes.yml");
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public CompletableFuture<Void> load() {
        return track(CompletableFuture.runAsync(this::readFile, io));
    }

    public List<UpgradeRecipe> sorted() {
        List<UpgradeRecipe> list = new ArrayList<>(recipes.values());
        list.sort(Comparator.comparing(UpgradeRecipe::id, ID_ORDER));
        return list;
    }

    public Optional<UpgradeRecipe> byId(String id) {
        return Optional.ofNullable(recipes.get(id));
    }

    public Optional<UpgradeRecipe> find(ItemStack item) {
        List<UpgradeRecipe> sorted = sorted();
        for (UpgradeRecipe recipe : sorted) {
            if (recipe.strict() && recipe.accepts(item)) {
                return Optional.of(recipe);
            }
        }
        for (UpgradeRecipe recipe : sorted) {
            if (!recipe.strict() && recipe.accepts(item)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public String nextId() {
        return "r" + counter.incrementAndGet();
    }

    public void put(UpgradeRecipe recipe) {
        recipes.put(recipe.id(), recipe);
        saveAsync();
    }

    public void remove(String id) {
        if (recipes.remove(id) != null) {
            saveAsync();
        }
    }

    public void close() {
        try {
            CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ex) {
            log.error("Не удалось дождаться завершения записи рецептов", ex);
        }
        io.shutdown();
    }

    private void saveAsync() {
        track(CompletableFuture.runAsync(this::writeFile, io));
    }

    private <T> CompletableFuture<T> track(CompletableFuture<T> future) {
        pending.add(future);
        future.whenComplete((result, error) -> {
            pending.remove(future);
            if (error != null) {
                log.error("Ошибка при работе с recipes.yml", error);
            }
        });
        return future;
    }

    private void readFile() {
        fileLock.lock();
        try {
            Map<String, UpgradeRecipe> read = new HashMap<>();
            if (Files.exists(file)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
                ConfigurationSection root = yaml.getConfigurationSection("recipes");
                if (root != null) {
                    for (String id : root.getKeys(false)) {
                        ConfigurationSection section = root.getConfigurationSection(id);
                        if (section == null) {
                            continue;
                        }
                        try {
                            read.put(id, parse(id, section));
                        } catch (RuntimeException ex) {
                            log.error("Рецепт {} повреждён и пропущен", id, ex);
                        }
                    }
                }
            }
            recipes.keySet().retainAll(read.keySet());
            recipes.putAll(read);
            counter.set(read.keySet().stream().mapToInt(RecipeRepository::numericId).max().orElse(0));
            loaded.set(true);
        } finally {
            fileLock.unlock();
        }
    }

    private void writeFile() {
        fileLock.lock();
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            for (UpgradeRecipe recipe : sorted()) {
                String path = "recipes." + recipe.id();
                yaml.set(path + ".input", encode(recipe.input()));
                yaml.set(path + ".result", encode(recipe.result()));
                yaml.set(path + ".coins", recipe.coins());
                yaml.set(path + ".enzymes", recipe.enzymes());
                yaml.set(path + ".chance", recipe.chance());
                yaml.set(path + ".bonus-per-extra", recipe.bonusPerExtra());
                yaml.set(path + ".lose-on-fail", recipe.loseOnFail());
                yaml.set(path + ".strict", recipe.strict());
            }
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("recipes.yml.tmp");
            Files.writeString(temp, yaml.saveToString(), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            log.error("Не удалось сохранить recipes.yml", ex);
        } finally {
            fileLock.unlock();
        }
    }

    private static UpgradeRecipe parse(String id, ConfigurationSection section) {
        ItemStack input = decode(section.getString("input"));
        ItemStack result = decode(section.getString("result"));
        return new UpgradeRecipe(id, input, result,
                section.getDouble("coins"),
                section.getInt("enzymes"),
                section.getDouble("chance"),
                section.getDouble("bonus-per-extra"),
                section.getBoolean("lose-on-fail", true),
                section.getBoolean("strict", true));
    }

    private static String encode(ItemStack stack) {
        return Base64.getEncoder().encodeToString(stack.serializeAsBytes());
    }

    private static ItemStack decode(String data) {
        if (data == null) {
            throw new IllegalStateException("отсутствует предмет");
        }
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
    }

    private static int numericId(String id) {
        try {
            return Integer.parseInt(id.substring(1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
            return 0;
        }
    }
}
