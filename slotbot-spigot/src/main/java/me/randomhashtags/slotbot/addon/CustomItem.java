package me.randomhashtags.slotbot.addon;

import com.sun.istack.internal.NotNull;
import me.randomhashtags.slotbot.addon.util.Identifiable;
import me.randomhashtags.slotbot.addon.util.Itemable;
import org.bukkit.entity.Player;

import java.util.List;

import static me.randomhashtags.slotbot.universal.UVersionable.CONSOLE;
import static me.randomhashtags.slotbot.universal.UVersionable.SERVER;

public interface CustomItem extends Identifiable, Itemable {
    List<String> getCommands();
    default boolean doesExecuteCommands() {
        final List<String> commands = getCommands();
        return commands != null && !commands.isEmpty();
    }
    default boolean executeCommands(@NotNull Player player) {
        final List<String> commands = getCommands();
        if(player != null && doesExecuteCommands()) {
            final String playerName = player.getName();
            for(String cmd : commands) {
                SERVER.dispatchCommand(CONSOLE, cmd.replace("%player%", playerName));
            }
            return true;
        }
        return false;
    }
}
