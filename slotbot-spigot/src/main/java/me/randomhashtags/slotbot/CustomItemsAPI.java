package me.randomhashtags.slotbot;

import me.randomhashtags.slotbot.universal.UVersionable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Collection;

public enum CustomItemsAPI implements Listener, UVersionable {
    INSTANCE;

    private boolean isEnabled;
    public YamlConfiguration config;
    private Collection<ItemStack> values;

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
                CUSTOM_ITEMS.put(identifier, is);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void playerInteractEvent(PlayerInteractEvent event) {
        final ItemStack is = event.getItem();
        if(values.contains(is)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
}
