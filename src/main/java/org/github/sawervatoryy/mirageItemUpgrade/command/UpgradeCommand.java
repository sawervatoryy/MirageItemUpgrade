package org.github.sawervatoryy.mirageItemUpgrade.command;

import org.github.sawervatoryy.mirageItemUpgrade.MirageItemUpgrade;
import org.github.sawervatoryy.mirageItemUpgrade.config.Lang;
import org.github.sawervatoryy.mirageItemUpgrade.gui.MenuManager;
import org.github.sawervatoryy.mirageItemUpgrade.service.EnzymeService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class UpgradeCommand {

    private static final String USE_PERMISSION = "mirageitemupgrade.use";
    private static final String ADMIN_PERMISSION = "mirageitemupgrade.admin";

    private final MirageItemUpgrade plugin;
    private final Lang lang;
    private final MenuManager menus;
    private final EnzymeService enzymes;

    public UpgradeCommand(MirageItemUpgrade plugin, Lang lang, MenuManager menus, EnzymeService enzymes) {
        this.plugin = plugin;
        this.lang = lang;
        this.menus = menus;
        this.enzymes = enzymes;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("upgrade")
                .requires(source -> source.getSender().hasPermission(USE_PERMISSION))
                .executes(this::openUpgrade)
                .then(Commands.literal("admin")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(this::openAdmin))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(this::reload))
                .then(Commands.literal("enzymes")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                        .executes(this::giveEnzymes))))
                .build();
    }

    private int openUpgrade(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.player-only");
            return 0;
        }
        if (!plugin.recipes().isLoaded()) {
            lang.send(sender, "command.loading");
            return 0;
        }
        menus.openUpgrade(player);
        return Command.SINGLE_SUCCESS;
    }

    private int openAdmin(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.player-only");
            return 0;
        }
        if (!plugin.recipes().isLoaded()) {
            lang.send(sender, "command.loading");
            return 0;
        }
        menus.openList(player, 0);
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        plugin.reload().whenComplete((result, error) ->
                Bukkit.getGlobalRegionScheduler().run(plugin,
                        task -> lang.send(sender, error == null ? "command.reloaded" : "command.reload-failed")));
        return Command.SINGLE_SUCCESS;
    }

    private int giveEnzymes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();
        List<Player> targets = context.getArgument("player", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (targets.isEmpty()) {
            lang.send(sender, "command.no-players");
            return 0;
        }
        for (Player target : targets) {
            enzymes.give(target, amount);
            lang.send(target, "command.enzymes-received", Placeholder.unparsed("amount", String.valueOf(amount)));
            lang.send(sender, "command.enzymes-given",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("player", target.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
