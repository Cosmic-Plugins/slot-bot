package me.randomhashtags.slotbot.addon;

import me.randomhashtags.slotbot.addon.util.Identifiable;
import me.randomhashtags.slotbot.addon.util.Itemable;

import java.util.List;

public interface CustomItem extends Identifiable, Itemable {
    List<String> getCommands();
}
