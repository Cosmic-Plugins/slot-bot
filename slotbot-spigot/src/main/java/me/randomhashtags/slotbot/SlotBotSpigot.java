package me.randomhashtags.slotbot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlotBotSpigot extends JavaPlugin {

    public static SlotBotSpigot getPlugin;
    public boolean placeholderapi;

    @Override
    public void onEnable() {
        getPlugin = this;
        saveSettings();
        getCommand("slotbot").setExecutor(SlotBotAPI.INSTANCE);
        enable();
    }

    @Override
    public void onDisable() {
        disable();
    }

    private void saveSettings() {
        saveDefaultConfig();
    }

    public void enable() {
        saveSettings();
        placeholderapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        CustomItemsAPI.INSTANCE.load();
        SlotBotAPI.INSTANCE.load();
    }
    public void disable() {
        placeholderapi = false;
        CustomItemsAPI.INSTANCE.unload();
        SlotBotAPI.INSTANCE.unload();
    }

    public void reload() {
        disable();
        enable();
    }
}
