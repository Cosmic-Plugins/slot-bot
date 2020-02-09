package me.randomhashtags.slotbot;

import com.sun.istack.internal.NotNull;
import me.randomhashtags.slotbot.addon.CustomItem;
import me.randomhashtags.slotbot.addon.PathCustomItem;
import me.randomhashtags.slotbot.universal.UVersionable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Collection;
import java.util.List;

public enum CustomItemsAPI implements Listener, UVersionable {
    INSTANCE;

    private boolean isEnabled;
    public YamlConfiguration config;
    private Collection<CustomItem> values;

    public void load() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        final long started = System.currentTimeMillis();
        PLUGIN_MANAGER.registerEvents(this, SLOT_BOT);
        save(null, "custom items.yml");
        config = YamlConfiguration.loadConfiguration(new File(SLOT_BOT.getDataFolder() + File.separator, "custom items.yml"));
        for(String identifier : getConfigurationSectionKeys(config, "", false)) {
            final ItemStack is = createItemStack(config, identifier);
            if(is != null) {
                final CustomItem customItem = new PathCustomItem(identifier, is, config.getStringList(identifier + ".commands"));
                CUSTOM_ITEMS.put(identifier, customItem);
            }
        }
        values = CUSTOM_ITEMS.values();
        sendConsoleDidLoadFeature(CUSTOM_ITEMS.size() + " Custom Items", started);
    }
    public void unload() {
        if(isEnabled) {
            isEnabled = false;
            CUSTOM_ITEMS.clear();
            HandlerList.unregisterAll(this);
        }
    }

    public CustomItem valueOf(@NotNull ItemStack is) {
        for(CustomItem item : values) {
            if(is.isSimilar(item.getItem())) {
                return item;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void playerInteractEvent(PlayerInteractEvent event) {
        final ItemStack is = event.getItem();
        final CustomItem item = valueOf(is);
        if(item != null) {
            final Player player = event.getPlayer();
            event.setCancelled(true);
            player.updateInventory();
            final List<String> commands = item.getCommands();
            if(commands != null) {
                final String playerName = player.getName();
                for(String cmd : commands) {
                    SERVER.dispatchCommand(CONSOLE, cmd.replace("%player%", playerName));
                }
                removeItem(player, is, 1);
            }
        }
    }
}
