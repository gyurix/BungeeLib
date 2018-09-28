package gyurix.bungeelib.command;

import net.md_5.bungee.api.CommandSender;

public interface CommandExecutor {
    boolean execute(CommandSender plr, String text);

    default boolean isAsync() {
        return false;
    }
}
