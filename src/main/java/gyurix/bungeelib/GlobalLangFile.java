package gyurix.bungeelib;

import gyurix.bungeelib.chat.ChatTag;
import gyurix.bungeelib.configfile.ConfigData;
import gyurix.bungeelib.utils.BU;
import net.md_5.bungee.api.CommandSender;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;

import static gyurix.bungeelib.Config.defaultLang;

public class GlobalLangFile {
    public static final HashMap<String, String> map = new HashMap<>();

    public static String get(String lang, String adr) {
        String s = map.get(lang + '.' + adr);
        if (s == null)
            s = map.get(defaultLang + '.' + adr);
        return s == null ? "§cMISSING:§e " + lang + '.' + adr : s;
    }

    private static void load(String[] data) {
        String adr = ".en";
        StringBuilder cs = new StringBuilder();
        int lvl = 0;
        int line = 0;
        for (String s : data) {
            int blockLvl = 0;
            ++line;
            while (s.charAt(blockLvl) == ' ') {
                ++blockLvl;
            }
            String[] d = ((s = s.substring(blockLvl)) + " ").split(" *: +", 2);
            if (d.length == 1) {
                s = ConfigData.unescape(s);
                if (cs.length() != 0) {
                    cs.append('\n');
                }
                cs.append(s);
                continue;
            }
            put(adr.substring(1), cs.toString());
            cs.setLength(0);
            if (blockLvl == lvl + 2) {
                adr = adr + '.' + d[0];
                lvl += 2;
            } else if (blockLvl == lvl) {
                adr = adr.substring(0, adr.lastIndexOf('.') + 1) + d[0];
            } else if (blockLvl < lvl && blockLvl % 2 == 0) {
                while (blockLvl != lvl) {
                    lvl -= 2;
                    adr = adr.substring(0, adr.lastIndexOf('.'));
                }
                adr = adr.substring(0, adr.lastIndexOf('.') + 1) + d[0];
            } else {
                throw new RuntimeException("Block leveling error in line " + line + "!");
            }
            if (d[1].isEmpty()) continue;
            cs.append(d[1].substring(0, d[1].length() - 1));
        }
        put(adr.substring(1), cs.toString());
    }

    public static PluginLang loadLF(String pn, InputStream stream, String fn) {
        try {
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            load(new String(bytes, "UTF-8").replaceAll("&([0-9a-fk-or])", "§$1").split("\r?\n"));
            load(new String(Files.readAllBytes(new File(fn).toPath()), "UTF-8")
                    .replaceAll("&([0-9a-fk-or])", "§$1").split("\r?\n"));
            return new PluginLang(pn);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PluginLang loadLF(String pn, String fn) {
        try {
            load(new String(Files.readAllBytes(new File(fn).toPath()), "UTF-8")
                    .replaceAll("&([0-9a-fk-or])", "§$1").split("\r?\n"));
            return new PluginLang(pn);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void put(String adr, String value) {
        map.put(adr, value);
    }

    public static class PluginLang {
        public final String pluginName;

        PluginLang(String plugin) {
            pluginName = plugin;
        }

        public String get(CommandSender sender, String adr, Object... repl) {
            if (sender == null)
                sender = BU.cs;
            String msg = GlobalLangFile.get("en", pluginName + '.' + adr);
            Object key = null;
            for (Object o : repl) {
                if (key == null) {
                    key = o;
                    continue;
                }
                msg = msg.replace("<" + key + '>', String.valueOf(o));
                key = null;
            }
            return msg;
        }

        public void msg(String prefix, CommandSender sender, String msg, Object... repl) {
            msg = prefix + get(sender, msg, repl);
            sender.sendMessage(ChatTag.fromExtraText(msg).toBaseComponent());
        }

        public void msg(CommandSender sender, String msg, Object... repl) {
            msg(get(sender, "prefix"), sender, msg, repl);
        }
    }
}

