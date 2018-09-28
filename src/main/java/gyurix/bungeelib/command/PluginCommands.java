package gyurix.bungeelib.command;

import gyurix.bungeelib.utils.BU;
import gyurix.bungeelib.utils.StringUtils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static gyurix.bungeelib.BungeeLib.lang;

public class PluginCommands {
    private static final String[] noAliases = new String[0];

    public PluginCommands(Plugin pl, Object executor) {
        String pln = pl.getDescription().getName();
        HashMap<String, ArrayList<CommandMatcher>> executors = new HashMap<>();
        for (Method m : executor.getClass().getMethods()) {
            String mn = m.getName();
            if (!mn.startsWith("cmd"))
                continue;
            String cmd = mn.substring(3).toLowerCase();
            ArrayList<CommandMatcher> exc = executors.computeIfAbsent(cmd, (s) -> new ArrayList<>());
            exc.add(new CommandMatcher(pln, executor, m));
        }
        executors.forEach((cmd, exec) -> register(pl, cmd, exec));
    }

    private void register(Plugin pl, String cmd, ArrayList<CommandMatcher> matchers) {
        matchers.sort(Comparator.comparing(CommandMatcher::getParameterCount));
        String[] aliases = noAliases;
        for (CommandMatcher m : matchers) {
            String[] a = m.getAliases();
            if (a != null) {
                aliases = a;
                break;
            }
        }
        BU.pm.registerCommand(pl, new Command(cmd,
                pl.getDescription().getName().toLowerCase() + "." + cmd,
                aliases) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                boolean async = false;
                for (CommandMatcher m : matchers) {
                    if (m.isAsync()) {
                        async = true;
                        break;
                    }
                }
                if (async)
                    BU.sch.runAsync(pl, () -> executeNow(sender, args));
                else
                    executeNow(sender, args);
            }

            public void executeNow(CommandSender sender, String[] args) {
                try {
                    CommandMatcher last = matchers.get(matchers.size() - 1);
                    int argsLen = args.length;
                    int lastParCount = last.getParameterCount();
                    if (lastParCount < args.length) {
                        if (lastParCount > 0)
                            args[lastParCount - 1] = StringUtils.join(args, ' ', lastParCount - 1, argsLen);
                        argsLen = lastParCount;
                    }
                    for (CommandMatcher m : matchers) {
                        if (m.getParameterCount() != argsLen)
                            continue;
                        if (!m.senderMatch(sender))
                            continue;
                        int pars = m.getParameterCount();
                        boolean allParsValid = true;
                        for (int i = 0; i < pars; ++i) {
                            if (!m.isValidParameter(i, args[i])) {
                                allParsValid = false;
                                break;
                            }
                        }
                        if (allParsValid) {
                            m.execute(sender, args);
                            return;
                        }
                    }
                    for (CommandMatcher m : matchers) {
                        if (m.getParameterCount() != argsLen)
                            continue;
                        m.execute(sender, args);
                        return;
                    }
                    for (CommandMatcher m : matchers) {
                        if (!m.senderMatch(sender))
                            continue;
                        if (m.getParameterCount() > argsLen) {
                            lang.msg("", sender, "command.usage", "usage", m.getUsage(args));
                            return;
                        }
                    }
                    lang.msg("", sender, "command.noconsole");
                    throw new RuntimeException("This can not happen :)");
                } catch (Throwable e) {
                    BU.error(sender, e, "BanManager", "gyurix");
                }
            }

        });
    }
}
