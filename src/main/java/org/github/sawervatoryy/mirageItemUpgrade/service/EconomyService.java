package org.github.sawervatoryy.mirageItemUpgrade.service;

import org.github.sawervatoryy.mirageItemUpgrade.util.Numbers;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

public final class EconomyService {

    public Optional<Economy> provider() {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration == null ? Optional.empty() : Optional.of(registration.getProvider());
    }

    public boolean available() {
        return provider().isPresent();
    }

    public boolean has(OfflinePlayer player, double amount) {
        return provider().map(economy -> economy.has(player, amount)).orElse(false);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return provider().map(economy -> economy.withdrawPlayer(player, amount).transactionSuccess()).orElse(false);
    }

    public String format(double amount) {
        return provider().map(economy -> economy.format(amount)).orElseGet(() -> Numbers.plain(amount));
    }
}
