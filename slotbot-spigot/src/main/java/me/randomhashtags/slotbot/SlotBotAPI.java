package me.randomhashtags.slotbot;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import me.randomhashtags.slotbot.addon.CustomItem;
import me.randomhashtags.slotbot.addon.PathCustomItem;
import me.randomhashtags.slotbot.universal.UInventory;
import me.randomhashtags.slotbot.universal.USound;
import me.randomhashtags.slotbot.util.ChatUtils;
import me.randomhashtags.slotbot.util.CustomSound;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public enum SlotBotAPI implements Listener, CommandExecutor, ChatUtils {
    INSTANCE;

    private boolean isEnabled;
    private UInventory gui, preview;
    public ItemStack ticket;

    private YamlConfiguration SLOT_BOT_CONFIG;

    private ItemStack item;
    private ItemMeta itemMeta;
    private List<String> lore;
    
    private ItemStack ticketLocked, ticketUnlocked, spinnerMissingTickets, spinnerReadyToSpin, rewardSlot, withdrawTickets;
    private ItemStack randomizedLootPlaceholder, randomizedLootReadyToRoll, previewRewards, background;
    private int withdrawTicketsSlot, spinnerSlot, previewRewardsSlot;
    private List<Integer> ticketSlots;
    private List<String> rewards;

    private HashMap<Player, HashMap<Integer, List<Integer>>> rollingTasks;
    private HashMap<Player, List<Integer>> pendingRewardSlots, unrolledTickets;
    private HashMap<Integer, List<Integer>> slots;

    private HashMap<String, CustomSound> sounds;
    private HashMap<SlotBotSetting, Boolean> settings;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        final boolean isPlayer = sender instanceof Player;
        final int length = args.length;
        if(length >= 1) {
            switch (args[0]) {
                case "reload":
                    if(sender.hasPermission("SlotBot.reload")) {
                        SLOT_BOT.reload();
                        sender.sendMessage(colorize("&6[SlotBot] &aSuccessfully reloaded"));
                    } else if(isPlayer) {
                        view((Player) sender);
                    }
                    break;
                case "give":
                    if(sender.hasPermission("SlotBot.give")) {
                        if(length >= 3) {
                            final String target = args[1];
                            final Player player = SERVER.getPlayer(target);
                            if(player != null) {
                                final ItemStack is = getClone(ticket);
                                is.setAmount(Integer.parseInt(args[2]));
                                giveItem(player, is);
                            }
                        }
                    } else if(isPlayer) {
                        view((Player) sender);
                    }
                    break;
                default:
                    if(isPlayer) {
                        view((Player) sender);
                    }
                    break;
            }
        } else if(isPlayer) {
            view((Player) sender);
        }
        return true;
    }

    public void load() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        final long started = System.currentTimeMillis();

        SLOT_BOT_CONFIG = YamlConfiguration.loadConfiguration(new File(SLOT_BOT.getDataFolder() + File.separator, "config.yml"));

        PLUGIN_MANAGER.registerEvents(this, SLOT_BOT);
        item = new ItemStack(Material.APPLE);
        itemMeta = item.getItemMeta();
        lore = new ArrayList<>();

        sounds = new HashMap<>();
        final String[] soundStrings = new String[] {
                "cancelled",
                "withdraw tickets",
                "insert ticket",
                "started spinning",
                "spinning",
                "finished spinning",
        };
        for(String s : soundStrings) {
            final String target = SLOT_BOT_CONFIG.getString("sounds." + s);
            if(target != null) {
                sounds.put(s, new CustomSound(target));
            }
        }

        settings = new HashMap<>();
        for(SlotBotSetting setting : SlotBotSetting.values()) {
            settings.put(setting, SLOT_BOT_CONFIG.getBoolean("settings." + setting.name().toLowerCase().replace("_", " ")));
        }

        ticket = createItemStack(SLOT_BOT_CONFIG, "items.ticket");
        final CustomItem ticketItem = new PathCustomItem("slotbotticket", ticket, null);
        CUSTOM_ITEMS.put("slotbotticket", ticketItem);

        ticketLocked = createItemStack(SLOT_BOT_CONFIG, "items.ticket locked");
        ticketUnlocked = createItemStack(SLOT_BOT_CONFIG, "items.ticket unlocked");
        rewardSlot = createItemStack(SLOT_BOT_CONFIG, "items.reward slot");
        withdrawTickets = createItemStack(SLOT_BOT_CONFIG, "items.withdraw tickets");
        spinnerMissingTickets = createItemStack(SLOT_BOT_CONFIG, "items.spinner missing ticket");
        spinnerReadyToSpin = createItemStack(SLOT_BOT_CONFIG, "items.spinner ready to spin");
        spinnerSlot = SLOT_BOT_CONFIG.getInt("items.spinner missing ticket.slot");

        preview = new UInventory(null, SLOT_BOT_CONFIG.getInt("preview rewards.size"), colorize(SLOT_BOT_CONFIG.getString("preview rewards.title")));
        gui = new UInventory(null, SLOT_BOT_CONFIG.getInt("gui.size"), colorize(SLOT_BOT_CONFIG.getString("gui.title")));
        randomizedLootPlaceholder = createItemStack(SLOT_BOT_CONFIG, "items.randomized loot placeholder");
        randomizedLootReadyToRoll = createItemStack(SLOT_BOT_CONFIG, "items.randomized loot ready to roll");
        background = createItemStack(SLOT_BOT_CONFIG, "gui.background");

        final Inventory inv = gui.getInventory();
        inv.setItem(spinnerSlot, spinnerMissingTickets);

        slots = new HashMap<>();
        ticketSlots = new ArrayList<>();
        int ticketAmount = 0;
        for(String s : SLOT_BOT_CONFIG.getStringList("items.ticket.slots")) {
            ticketAmount++;
            final int slot = Integer.parseInt(s);
            inv.setItem(slot, getTicketLocked(ticketAmount));
            ticketSlots.add(slot);
        }

        for(String key : getConfigurationSectionKeys(SLOT_BOT_CONFIG, "gui.reward slots", false)) {
            final int slot = Integer.parseInt(key);
            inv.setItem(slot, rewardSlot);
            final List<Integer> rewardSlots = new ArrayList<>();
            for(String s : SLOT_BOT_CONFIG.getStringList("gui.reward slots." + key)) {
                final int rewardSlot = Integer.parseInt(s);
                inv.setItem(rewardSlot, randomizedLootPlaceholder);
                rewardSlots.add(rewardSlot);
            }
            slots.put(slot, rewardSlots);
        }
        for(String key : getConfigurationSectionKeys(SLOT_BOT_CONFIG, "gui", false, "title", "size", "background", "reward slots", "visual placeholder slots")) {
            final String path = "gui." + key;
            final String item = SLOT_BOT_CONFIG.getString(path + ".item");
            final int slot = SLOT_BOT_CONFIG.getInt(path + ".slot");
            final boolean isWithdraw = "WITHDRAW_TICKETS".equals(item);
            if(isWithdraw) {
                withdrawTicketsSlot = slot;
            }
            inv.setItem(slot, isWithdraw ? null : createItemStack(SLOT_BOT_CONFIG, path));
        }

        final ItemStack visualPlaceholder = createItemStack(SLOT_BOT_CONFIG, "items.visual placeholder");
        for(String s : SLOT_BOT_CONFIG.getStringList("gui.visual placeholder slots")) {
            inv.setItem(Integer.parseInt(s), visualPlaceholder);
        }

        for(int i = 0; i < inv.getSize(); i++) {
            if(inv.getItem(i) == null) {
                inv.setItem(i, background);
            }
        }

        rollingTasks = new HashMap<>();
        pendingRewardSlots = new HashMap<>();
        unrolledTickets = new HashMap<>();
        rewards = SLOT_BOT_CONFIG.getStringList("rewards");

        previewRewardsSlot = SLOT_BOT_CONFIG.getInt("items.preview rewards.slot");
        previewRewards = createItemStack(SLOT_BOT_CONFIG, "items.preview rewards");
        itemMeta = previewRewards.getItemMeta(); lore.clear();

        final List<ItemStack> previewRewardList = new ArrayList<>();
        final List<String> actualRewards = new ArrayList<>();
        for(String s : itemMeta.getLore()) {
            if(s.contains("{AMOUNT}") && s.contains("{ITEM}")) {
                for(String reward : rewards) {
                    final ItemStack is = createItemStack(null, reward);
                    if(is != null) {
                        actualRewards.add(reward);
                        ItemMeta meta = is.getItemMeta();
                        lore.add(s.replace("{AMOUNT}", Integer.toString(is.getAmount())).replace("{ITEM}", meta.hasDisplayName() ? meta.getDisplayName() : is.getType().name()));
                        previewRewardList.add(is);
                    }
                }
            } else {
                lore.add(s);
            }
        }
        itemMeta.setLore(lore); lore.clear();
        previewRewards.setItemMeta(itemMeta);
        inv.setItem(previewRewardsSlot, previewRewards);
        rewards = actualRewards;

        final Inventory previewInv = preview.getInventory();
        for(ItemStack is : previewRewardList) {
            previewInv.setItem(previewInv.firstEmpty(), is);
        }
        sendConsoleDidLoadFeature("SlotBotAPI", started);
    }
    public void unload() {
        if(isEnabled) {
            isEnabled = false;
            for(Player player : new ArrayList<>(pendingRewardSlots.keySet())) {
                player.closeInventory();
            }
            HandlerList.unregisterAll(this);
        }
    }

    private void playSound(Player player, String identifier) {
        final CustomSound sound = sounds.getOrDefault(identifier, null);
        if(sound != null) {
            final USound usound = sound.getUSound();
            final Sound realSound = usound.getSound();
            if(realSound != null) {
                usound.playSound(player, sound.getVolume(), sound.getPitch());
            }
        }
    }
    public boolean isSlotBotSettingEnabled(@NotNull SlotBotSetting setting) {
        return settings.getOrDefault(setting, false);
    }


    private ItemStack getTicketLocked(int ticketAmount) {
        item = ticketLocked.clone();
        itemMeta = item.getItemMeta(); lore.clear();
        for(String string : itemMeta.getLore()) {
            lore.add(string.replace("{AMOUNT}", Integer.toString(ticketAmount)));
        }
        itemMeta.setLore(lore); lore.clear();
        item.setItemMeta(itemMeta);
        item.setAmount(ticketAmount);
        return item;
    }

    public void view(@NotNull Player player) {
        player.closeInventory();
        player.openInventory(Bukkit.createInventory(player, gui.getSize(), gui.getTitle()));
        player.getOpenInventory().getTopInventory().setContents(gui.getInventory().getContents());
        player.updateInventory();
    }
    public void viewPreview(@NotNull Player player) {
        player.closeInventory();
        player.openInventory(Bukkit.createInventory(player, preview.getSize(), preview.getTitle()));
        player.getOpenInventory().getTopInventory().setContents(preview.getInventory().getContents());
        player.updateInventory();
    }
    public void tryWithdrawingTickets(@NotNull Player player) {
        final Inventory top = player.getOpenInventory().getTopInventory();
        int ticketAmount = 0;
        final boolean isUnrolled = unrolledTickets.containsKey(player);
        final List<Integer> unrolledSlots = isUnrolled ? unrolledTickets.get(player) : null;
        final List<Integer> keySet = new ArrayList<>(slots.keySet());
        for(int i : ticketSlots) {
            final int rewardSlot = keySet.get(ticketAmount);
            ticketAmount++;
            final boolean isActuallyUnrolled = isUnrolled && unrolledSlots.contains(rewardSlot);
            if(ticketUnlocked.isSimilar(top.getItem(i)) && isActuallyUnrolled) {
                giveItem(player, ticket);
                top.setItem(i, getTicketLocked(keySet.indexOf(rewardSlot)+1));
                for(int slot : slots.get(rewardSlot)) {
                    top.setItem(slot, randomizedLootPlaceholder);
                }
            }
        }
        top.setItem(withdrawTicketsSlot, background);
        top.setItem(spinnerSlot, spinnerMissingTickets);
        player.updateInventory();
        playSound(player, "withdraw tickets");
        unrolledTickets.remove(player);
    }
    public int getInsertedTickets(@NotNull Player player) {
        final Inventory top = player.getOpenInventory().getTopInventory();
        int total = 0;
        for(int slot : ticketSlots) {
            if(ticketUnlocked.isSimilar(top.getItem(slot))) {
                total++;
            }
        }
        return total;
    }
    public void tryInsertingTicket(@NotNull Player player) {
        final Inventory top = player.getOpenInventory().getTopInventory();
        final int inserted = getInsertedTickets(player), maxAllowed = ticketSlots.size();
        if(inserted < maxAllowed) {
            final ItemStack withdraw = top.getItem(withdrawTicketsSlot), spin = top.getItem(spinnerSlot);
            if(!withdrawTickets.isSimilar(withdraw)) {
                top.setItem(withdrawTicketsSlot, withdrawTickets);
            }
            if(!spinnerReadyToSpin.isSimilar(spin)) {
                top.setItem(spinnerSlot, spinnerReadyToSpin);
            }
            for(int slot : slots.get(slots.keySet().toArray()[inserted])) {
                top.setItem(slot, randomizedLootReadyToRoll);
            }
            removeItem(player, ticket, 1);
            final int slot = ticketSlots.get(inserted);
            item = getClone(ticketUnlocked);
            item.setAmount(inserted+1);
            top.setItem(slot, item);
            player.updateInventory();
            playSound(player, "insert ticket");

            if(!unrolledTickets.containsKey(player)) {
                unrolledTickets.put(player, new ArrayList<>());
            }
            unrolledTickets.get(player).add(slots.keySet().toArray(new Integer[slots.size()])[inserted]);
        }
    }
    public boolean trySpinning(@NotNull Player player, int slot, @NotNull ItemStack targetItem) {
        if(spinnerReadyToSpin.isSimilar(targetItem)) {
            final Inventory top = player.getOpenInventory().getTopInventory();
            top.setItem(withdrawTicketsSlot, background);
            List<Integer> insertedTickets = new ArrayList<>();
            final boolean isUnrolled = unrolledTickets.containsKey(player);
            final List<Integer> unrolledSlots = isUnrolled ? unrolledTickets.get(player) : null;
            int ticket = 0;
            for(int ticketSlot : ticketSlots) {
                if(ticketUnlocked.isSimilar(top.getItem(ticketSlot))) {
                    final int rewardSlot = (int) slots.keySet().toArray()[ticket];
                    if(isUnrolled && unrolledSlots.contains(rewardSlot)) {
                        insertedTickets.add(rewardSlot);
                    }
                    ticket++;
                }
            }
            unrolledTickets.remove(player);
            if(!insertedTickets.isEmpty()) {
                for(int rewardSlot : insertedTickets) {
                    startRolling(player, top, rewardSlot);
                }
                playSound(player, "started spinning");
                return true;
            }
        } else if(ticketSlots.contains(slot) && !ticketUnlocked.isSimilar(targetItem)) {
            final HashMap<String, String> replacements = new HashMap<>();
            final int index = ticketSlots.indexOf(slot);
            replacements.put("{AMOUNT}", Integer.toString(index+1));
            sendStringListMessage(player, getStringList(SLOT_BOT_CONFIG, "messages.slot requires ticket"), replacements);
            playSound(player, "cancelled");
        }
        return false;
    }
    private ItemStack getRandomReward(int size) {
        return createItemStack(null, rewards.get(RANDOM.nextInt(size)));
    }
    private void updateRandomLoot(Player player, Inventory top, int rewardSlot, boolean isRandom, boolean playSound) {
        if(playSound) {
            playSound(player, "spinning");
        }
        final int size = rewards.size();

        final List<Integer> slots = new ArrayList<>(this.slots.get(rewardSlot));
        slots.add(rewardSlot);
        Collections.sort(slots);
        final List<ItemStack> previousRewards = new ArrayList<>();
        previousRewards.add(null);

        if(!isRandom) {
            for(int slot : slots) {
                previousRewards.add(top.getItem(slot));
            }
        }
        top.setItem(slots.get(0), getRandomReward(size));
        int index = 0;
        for(int slot : slots) {
            ItemStack target = isRandom ? getRandomReward(size) : previousRewards.get(index);
            if(target == null) {
                target = getRandomReward(size);
            }
            top.setItem(slot, target);
            index++;
        }
        player.updateInventory();
    }
    private void startRolling(Player player, Inventory top, int rewardSlot) {
        if(!rollingTasks.containsKey(player)) {
            rollingTasks.put(player, new HashMap<>());
        }
        if(!pendingRewardSlots.containsKey(player)) {
            pendingRewardSlots.put(player, new ArrayList<>());
        }
        final HashMap<Integer, List<Integer>> slotTasks = rollingTasks.get(player);
        if(!slotTasks.containsKey(rewardSlot)) {
            slotTasks.put(rewardSlot, new ArrayList<>());
        }

        final List<Integer> tasks = slotTasks.get(rewardSlot);
        pendingRewardSlots.get(player).add(rewardSlot);

        final boolean isRandom = isSlotBotSettingEnabled(SlotBotSetting.ALWAYS_RANDOM_LOOT);

        updateRandomLoot(player, top, rewardSlot, true, false);
        for(int i = 1; i <= 10; i++) {
            tasks.add(SCHEDULER.scheduleSyncDelayedTask(SLOT_BOT, () -> {
                updateRandomLoot(player, top, rewardSlot, isRandom, true);
            }, i*5));
        }
        for(int i = 1; i <= 10; i++) {
            final int I = i;
            tasks.add(SCHEDULER.scheduleSyncDelayedTask(SLOT_BOT, () -> {
                updateRandomLoot(player, top, rewardSlot, isRandom, true);
                if(I == 10) {
                    stopRolling(player, rewardSlot, true);
                }
            }, 50+(i*10)));
        }
    }
    public void stopRolling(@NotNull Player player) {
        for(int rewardSlot : slots.keySet()) {
            stopRolling(player, rewardSlot, false);
        }
        playSound(player, "finished spinning");
    }
    public void stopRolling(@NotNull Player player, int rewardSlot, boolean playSound) {
        final List<Integer> pendingSlots = pendingRewardSlots.getOrDefault(player, null);
        if(pendingSlots != null && pendingSlots.contains(rewardSlot) && rollingTasks.containsKey(player)) {
            final HashMap<Integer, List<Integer>> tasks = rollingTasks.get(player);
            if(tasks.containsKey(rewardSlot)) {
                for(int task : tasks.get(rewardSlot)) {
                    SCHEDULER.cancelTask(task);
                }
                final Inventory top = player.getOpenInventory().getTopInventory();
                for(int slot : slots.get(rewardSlot)) {
                    top.setItem(slot, randomizedLootPlaceholder);
                }
                tasks.remove(rewardSlot);
                player.updateInventory();
                if(playSound) {
                    playSound(player, "finished spinning");
                }
            }
        }
    }
    public boolean isCustomItemThatInstantlyExecutesCommands(@Nullable ItemStack is) {
        return isCustomItemThatInstantlyExecutesCommands(CustomItemsAPI.INSTANCE.valueOf(is));
    }
    public boolean isCustomItemThatInstantlyExecutesCommands(@Nullable CustomItem customItem) {
        return customItem != null && isSlotBotSettingEnabled(SlotBotSetting.INSTANT_CUSTOM_ITEM_COMMAND_EXECUTION) && customItem.doesExecuteCommands();
    }
    private void giveLoot(Player player) {
        final List<ItemStack> items = new ArrayList<>();
        final List<CustomItem> executeCustomItemCommands = new ArrayList<>();
        final int slotsSize = slots.size();
        int tickets = 0;
        if(pendingRewardSlots.containsKey(player)) {
            final Inventory top = player.getOpenInventory().getTopInventory();
            int ticketsInserted = getInsertedTickets(player);
            tickets += ticketsInserted;

            for(int i : slots.keySet()) {
                item = top.getItem(i);
                if(item != null && ticketsInserted > 0) {
                    if(!rewardSlot.isSimilar(item)) {
                        items.add(item);
                        ticketsInserted -= 1;
                    } else {
                        tickets -= 1;
                    }
                }
            }
            pendingRewardSlots.remove(player);
        }
        if(unrolledTickets.containsKey(player)) {
            final List<Integer> unrolledPlayerTickets = unrolledTickets.get(player);
            final int size = unrolledPlayerTickets.size();
            tickets += size;
            for(int i = 1; i <= size; i++) {
                items.add(getRandomReward(slotsSize));
            }
            unrolledTickets.remove(player);
        }

        if(!items.isEmpty()) {
            for(ItemStack is : items) {
                final CustomItem customItem = CustomItemsAPI.INSTANCE.valueOf(is);
                if(isCustomItemThatInstantlyExecutesCommands(customItem)) {
                    executeCustomItemCommands.add(customItem);
                } else {
                    giveItem(player, is);
                }
            }
            final String playerName = player.getName(), ticketsInserted = Integer.toString(tickets);
            final boolean isCentered = SLOT_BOT_CONFIG.getBoolean("messages.loot.centered");
            for(String s : getStringList(SLOT_BOT_CONFIG, "messages.loot.msg")) {
                s = s.replace("{PLAYER}", playerName).replace("{TICKETS}", ticketsInserted);
                if(s.contains("{AMOUNT}") && s.contains("{ITEM}")) {
                    for(ItemStack is : items) {
                        itemMeta = is.getItemMeta();
                        final String name = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : is.getType().name();
                        final String target = s.replace("{AMOUNT}", Integer.toString(is.getAmount())).replace("{ITEM}", name);
                        final String message = isCentered ? center(target, 70) : target;
                        final TextComponent hover = getHoverMessage(message, is);
                        sendHoverMessage(null, hover, true);
                    }
                } else {
                    Bukkit.broadcastMessage(isCentered ? center(s, 70) : s);
                }
            }
            for(CustomItem customItem : executeCustomItemCommands) {
                customItem.executeCommands(player);
            }
            player.updateInventory();
        }
    }

    @EventHandler
    private void inventoryCloseEvent(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        final boolean isSpinning = rollingTasks.containsKey(player);
        final boolean isInSlotBot = isSpinning || unrolledTickets.containsKey(player);
        if(isInSlotBot) {
            if(isSpinning) {
                final int size = rollingTasks.get(player).size();
                if(size > 0) {
                    if(isSlotBotSettingEnabled(SlotBotSetting.INVENTORY_IS_CLOSEABLE_WHEN_SPINNING)) {
                        stopRolling(player);
                        rollingTasks.remove(player);
                    } else {
                        final Inventory inv = event.getInventory();
                        SCHEDULER.scheduleSyncDelayedTask(SLOT_BOT, () -> {
                            player.openInventory(inv);
                        }, 0);
                        return;
                    }
                }
            }
            giveLoot(player);
        }
    }
    @EventHandler
    private void playerInteractEvent(PlayerInteractEvent event) {
        final ItemStack is = event.getItem();
        if(ticket.isSimilar(is)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
    @EventHandler
    private void playerQuitEvent(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        stopRolling(player);
        giveLoot(player);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void inventoryClickEvent(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final Inventory top = player.getOpenInventory().getTopInventory();
        if(player.equals(top.getHolder())) {
            final String title = event.getView().getTitle();
            final boolean isGUI = title.equals(gui.getTitle());
            if(isGUI || title.equals(preview.getTitle())) {
                event.setCancelled(true);
                player.updateInventory();
                final ItemStack current = event.getCurrentItem();
                if(current == null || !isGUI) {
                    return;
                }
                final int slot = event.getRawSlot();

                if(slot >= top.getSize()) {
                    if(current.isSimilar(ticket)) {
                        tryInsertingTicket(player);
                    }
                } else if(slot == previewRewardsSlot) {
                    if(!rollingTasks.containsKey(player) || rollingTasks.get(player).size() == 0) {
                        viewPreview(player);
                    }
                } else if(current.isSimilar(spinnerMissingTickets)) {
                    sendStringListMessage(player, getStringList(SLOT_BOT_CONFIG, "messages.missing tickets"), null);
                } else if(current.isSimilar(spinnerReadyToSpin)) {
                    for(int i : ticketSlots) {
                        trySpinning(player, i, spinnerReadyToSpin);
                    }
                } else if(current.isSimilar(withdrawTickets)) {
                    tryWithdrawingTickets(player);
                } else if(ticketSlots.contains(slot)) {
                    trySpinning(player, slot, current);
                }
            }
        }
    }
}
