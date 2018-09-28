package gyurix.bungeelib.command;

import gyurix.bungeelib.api.VariableAPI;
import gyurix.bungeelib.configfile.ConfigSerialization.StringSerializable;
import gyurix.bungeelib.utils.BU;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static gyurix.bungeelib.utils.BU.bc;
import static gyurix.bungeelib.utils.BU.bl;

public class Command implements StringSerializable {
    private static HashMap<String, CommandExecutor> executors = new HashMap<>();

    static {
        registerExecutor((sender, text) -> {
            sender.sendMessage(text);
            return true;
        }, "MSG");
        registerExecutor((sender, text) -> BU.pm.dispatchCommand(BU.cs, text), "CONSOLE");
        registerExecutor((sender, text) -> {
            if (sender instanceof ProxiedPlayer) {
                ((ProxiedPlayer) sender).chat(text);
                return true;
            }
            return false;
        }, "NORMAL");

        registerExecutor((sender, text) -> {
            if (sender instanceof ProxiedPlayer) {
                ServerInfo si = bc.getServerInfo(text);
                if (si == null)
                    return false;
                ((ProxiedPlayer) sender).connect(si);
                return true;
            }
            return false;
        }, "SERVER");

        registerExecutor((sender, text) -> true, "NOCMD");
    }

    private long delay;
    private String msg;
    private String type;

    public Command(String in) {
        String[] d = in.split(":", 2);
        if (d[0].startsWith("{")) {
            int len = d[0].indexOf("}");
            delay = Long.valueOf(d[0].substring(1, len));
            d[0] = d[0].substring(len + 1);
        }
        type = d[0].toUpperCase();
        msg = d[1];
    }

    public static void executeAll(Iterable<Command> cmds, ProxiedPlayer plr) {
        if (cmds == null)
            return;
        Iterator<Command> it = cmds.iterator();
        AtomicInteger cmdId = new AtomicInteger();
    }

    private static void executeAll(Iterator<Command> cmds, CommandSender sender) {
        while (cmds.hasNext()) {
            Command cmd = cmds.next();
            CommandExecutor exec = cmd.getExecutor();
            if (exec == null)
                return;
            if (cmd.delay > 0) {
                BU.sch.schedule(bl, () -> {
                    if (exec.isAsync()) {
                        BU.sch.runAsync(bl, () -> {
                            if (cmd.executeNow(sender))
                                executeAll(cmds, sender);
                        });
                        return;
                    }
                    if (cmd.executeNow(sender))
                        executeAll(cmds, sender);
                }, cmd.delay, TimeUnit.MILLISECONDS);
                return;
            }
            if (exec.isAsync()) {
                BU.sch.runAsync(bl, () -> {
                    if (cmd.executeNow(sender))
                        executeAll(cmds, sender);
                });
                return;
            }
        }
    }

    public static void registerExecutor(CommandExecutor exec, String... commands) {
        for (String s : commands)
            executors.put(s.toUpperCase(), exec);
    }

    public static void unregisterExecutors(String... commands) {
        for (String s : commands)
            executors.remove(s.toUpperCase());
    }

    public boolean executeNow(CommandSender sender) {
        String text = sender instanceof ProxiedPlayer ? VariableAPI.fillVariables(msg, (ProxiedPlayer) sender) : msg;
        CommandExecutor exec = getExecutor();
        return exec != null && exec.execute(sender, text);
    }

    private CommandExecutor getExecutor() {
        return executors.get(type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (delay > 0)
            sb.append('{').append(delay).append('}');
        sb.append(type).append(':').append(msg);
        return sb.toString();
    }
}
