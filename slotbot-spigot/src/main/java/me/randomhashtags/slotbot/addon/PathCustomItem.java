package me.randomhashtags.slotbot.addon;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PathCustomItem implements CustomItem {
    private String path;
    private ItemStack item;
    private List<String> commands;

    public PathCustomItem(String path, ItemStack item, List<String> commands) {
        this.path = path;
        this.item = item;
        this.commands = commands;
    }

    public String getIdentifier() {
        return path;
    }
    public ItemStack getItem() {
        return item;
    }
    public List<String> getCommands() {
        return commands;
    }
}
