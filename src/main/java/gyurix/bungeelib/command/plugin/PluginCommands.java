package gyurix.bungeelib.command.plugin;

import com.google.common.collect.ImmutableList;
import gyurix.bungeelib.utils.BU;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import static gyurix.bungeelib.BungeeLib.lang;
import static gyurix.bungeelib.utils.BU.error;

public class PluginCommands {
  private static final List<String> EMPTY_LIST = ImmutableList.of();
  private static final String[] emptyStringArray = new String[0];

  public PluginCommands(Plugin pl, Object executor) {
    String pln = pl.getDescription().getName();
    HashMap<String, CommandMatcher> executors = new HashMap<>();
    SubOf subOfAnnotation = executor.getClass().getAnnotation(SubOf.class);
    String subOf = subOfAnnotation == null ? null : subOfAnnotation.value();
    for (Method m : executor.getClass().getMethods()) {
      String mn = m.getName();
      if (!mn.startsWith("cmd"))
        continue;
      String[] cmd = mn.substring(3).toLowerCase().split("_");
      CommandMatcher cm = executors.computeIfAbsent(cmd[0], (s) -> new CommandMatcher(pln, cmd[0]));
      if (cmd.length == 1) {
        cm.addMatcher(new CommandMatcher(pln, cmd[0], subOf, executor, m));
        continue;
      }
      CommandMatcher cur = cm;
      for (int i = 1; i < cmd.length; ++i) {
        cur = cur.getOrAddChild(pln, cmd[i]);
      }
      cur.addMatcher(new CommandMatcher(pln, cmd[cmd.length - 1], null, executor, m));
    }
    if (subOf == null) {
      executors.forEach((cmd, exec) -> BU.pm.registerCommand(pl, createExecutor(pl, cmd, exec)));
      return;
    }
    HashMap<String, ExtendedCommandExecutor> mapping = new HashMap<>();
    executors.forEach((cmd, exec) -> {
      ExtendedCommandExecutor ee = createExecutor(pl, cmd, exec);
      mapping.put(cmd, ee);
      for (String a : ee.getAliases())
        mapping.put(a, ee);
    });
    BU.pm.registerCommand(pl, new ExtendedCommandExecutor(subOf) {
      @Override
      public void execute(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        ExtendedCommandExecutor exec = mapping.get(sub);
        if (args.length == 0 && exec == null)
          exec = mapping.get("help");
        if (exec == null) {
          lang.msg("", sender, "command.wrongsub");
          return;
        }
        String[] subArgs = args.length < 2 ? emptyStringArray : new String[args.length - 1];
        if (args.length > 1)
          System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        exec.execute(sender, subArgs);
      }

      @Override
      public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        ArrayList<String> out = new ArrayList<>();
        if (args.length == 1) {
          for (String sub : mapping.keySet()) {
            if (sender.hasPermission(pln.toLowerCase() + ".command." + sub))
              out.add(sub);
          }
          return BU.filterStart(out, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (!sender.hasPermission(pln.toLowerCase() + ".command." + sub))
          return EMPTY_LIST;
        ExtendedCommandExecutor exec = mapping.get(sub);
        if (exec == null)
          return EMPTY_LIST;
        String[] subArgs = args.length < 2 ? emptyStringArray : new String[args.length - 1];
        if (args.length > 1)
          System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        return exec.onTabComplete(sender, subArgs);
      }
    });
  }

  @SneakyThrows
  public static void registerCommands(Plugin pl, Class<?>... classes) {
    for (Class<?> c : classes) {
      new PluginCommands(pl, c.newInstance());
    }
  }

  private ExtendedCommandExecutor createExecutor(Plugin pl, String cmd, CommandMatcher m) {
    TreeSet<String> al = m.getAliases();
    String permission = (pl.getDescription().getName() + ".command." + cmd).toLowerCase();
    String[] ala = new String[al.size()];
    return new ExtendedCommandExecutor(cmd, permission, al.toArray(ala)) {
      @Override
      public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(permission)) {
          lang.msg("", sender, "command.noperm");
          return;
        }
        BU.sch.runAsync(pl, () -> executeNow(sender, args));
      }

      public void executeNow(CommandSender sender, String[] args) {
        try {
          if (m.checkParameters(sender, args)) {
            m.execute(sender, args);
            return;
          }
          lang.msg("", sender, "command.usage");
          for (String s : new TreeSet<>(m.getUsage(sender, args)))
            sender.sendMessage(s);
        } catch (Throwable e) {
          error(sender, e, "BungeeLib", "gyurix");
        }
      }

      @Override
      public List<String> onTabComplete(CommandSender sender, String[] args) {
        return m.tabComplete(sender, args);
      }
    };
  }

  private abstract static class ExtendedCommandExecutor extends Command implements TabExecutor {
    public ExtendedCommandExecutor(String name) {
      super(name);
    }

    public ExtendedCommandExecutor(String name, String permission, String... aliases) {
      super(name, permission, aliases);
    }
  }
}
