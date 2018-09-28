package gyurix.bungeelib.utils;

import gyurix.bungeelib.BungeeLib;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.scheduler.BungeeScheduler;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

/**
 * SpigotLib utilities class
 */
public final class BU {
    /**
     * The main instance of the BungeeCord object.
     */
    public static BungeeCord bc;
    /**
     * The main instance of the BungeeLib plugin
     */
    public static BungeeLib bl;
    /**
     * The main instance of the ConsoleCommandSender object.
     */
    public static ConsoleCommandSender cs;
    public static ScriptEngine js;
    /**
     * The main instance of the PluginManager object.
     */
    public static PluginManager pm;
    /**
     * An instance of the Random number generator
     */
    public static Random rand = new Random();
    /**
     * The main instance of the BukkitScheduler object.
     */
    public static BungeeScheduler sch;
    public static Charset utf8 = Charset.forName("UTF-8");

    /**
     * Sends an error report to the given sender and to console. The report only includes the stack trace parts, which
     * contains the authors name
     *
     * @param sender - The CommandSender who should receive the error report
     * @param err    - The error
     * @param plugin - The plugin where the error appeared
     * @param author - The author name, which will be searched in the error report
     */
    public static void error(CommandSender sender, Throwable err, String plugin, String author) {
        StringBuilder report = new StringBuilder();
        report.append("§4§l").append(plugin).append(" - ERROR REPORT - ")
                .append(err.getClass().getSimpleName());
        if (err.getMessage() != null)
            report.append('\n').append(err.getMessage());
        int i = 0;
        boolean startrep = true;
        for (StackTraceElement el : err.getStackTrace()) {
            boolean force = el.getClassName() != null && el.getClassName().contains(author);
            if (force)
                startrep = false;
            if (startrep || force)
                report.append("\n§c #").append(++i)
                        .append(": §eLINE §a").append(el.getLineNumber())
                        .append("§e in FILE §6").append(el.getFileName())
                        .append("§e (§7").append(el.getClassName())
                        .append("§e.§b").append(el.getMethodName())
                        .append("§e)");
        }
        String rep = report.toString();
        cs.sendMessage(rep);
        if (sender != null && sender != cs)
            sender.sendMessage(rep);
    }

    /**
     * Fills variables in a String
     *
     * @param s    - The String
     * @param vars - The variables and their values, which should be filled
     * @return The variable filled String
     */
    public static String fillVariables(String s, Object... vars) {
        String last = null;
        for (Object v : vars) {
            if (last == null)
                last = (String) v;
            else {
                s = s.replace('<' + last + '>', String.valueOf(v));
                last = null;
            }
        }
        return s;
    }

    /**
     * Fills variables in a String
     *
     * @param s    - The String
     * @param vars - The variables and their values, which should be filled
     * @return The variable filled String
     */
    public static String fillVariables(String s, HashMap<String, Object> vars) {
        for (Entry<String, Object> v : vars.entrySet())
            s = s.replace('<' + v.getKey() + '>', String.valueOf(v.getValue()));
        return s;
    }

    /**
     * Filters the startings of the given data
     *
     * @param data  - The data to be filtered
     * @param start - Filter every string which starts with this one
     * @return The filtered Strings
     */
    public static ArrayList<String> filterStart(String[] data, String start) {
        start = start.toLowerCase();
        ArrayList<String> ld = new ArrayList<>();
        for (String s : data) {
            if (s.toLowerCase().startsWith(start))
                ld.add(s);
        }
        Collections.sort(ld);
        return ld;
    }

    /**
     * Filters the startings of the given data
     *
     * @param data  - The data to be filtered
     * @param start - Filter every string which starts with this one
     * @return The filtered Strings
     */
    public static ArrayList<String> filterStart(Iterable<String> data, String start) {
        start = start.toLowerCase();
        ArrayList<String> ld = new ArrayList<>();
        for (String s : data) {
            if (s.toLowerCase().startsWith(start))
                ld.add(s);
        }
        Collections.sort(ld);
        return ld;
    }

    /**
     * Logs messages from the given plugin. You can use color codes in the msg.
     *
     * @param pl  - The plugin who wants to log the message
     * @param msg - The message which should be logged
     */
    public static void log(Plugin pl, Iterable<Object>... msg) {
        cs.sendMessage('[' + pl.getDescription().getName() + "] " + StringUtils.join(msg, ", "));
    }

    /**
     * Optimizes color and formatting code usage in a string by removing redundant color/formatting codes
     *
     * @param in input message containing color and formatting codes
     * @return The color and formatting code optimized string
     */

    /**
     * Logs messages from the given plugin. You can use color codes in the msg.
     *
     * @param pl  - The plugin who wants to log the message
     * @param msg - The message which should be logged
     */
    public static void log(Plugin pl, Object... msg) {
        cs.sendMessage('[' + pl.getDescription().getName() + "] " + StringUtils.join(msg, ", "));
    }

    /**
     * Optimizes color and formatting code usage in a string by removing redundant color/formatting codes
     *
     * @param in input message containing color and formatting codes
     * @return The color and formatting code optimized string
     */
    public static String optimizeColorCodes(String in) {
        StringBuilder out = new StringBuilder();
        StringBuilder oldFormat = new StringBuilder("§r");
        StringBuilder newFormat = new StringBuilder("§r");
        StringBuilder formatChange = new StringBuilder();
        String formatArchive = "";
        boolean color = false;
        for (char c : in.toCharArray()) {
            if (color) {
                color = false;
                if (c >= 'k' && c <= 'o') {
                    int max = newFormat.length();
                    boolean add = true;
                    for (int i = 1; i < max; i += 2) {
                        if (newFormat.charAt(i) == c) {
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        newFormat.append('§').append(c);
                        formatChange.append('§').append(c);
                    }
                    continue;
                }
                if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                    c = 'f';
                newFormat.setLength(0);
                newFormat.append('§').append(c);
                formatChange.setLength(0);
                formatChange.append('§').append(c);
            } else if (c == '§')
                color = true;
            else if (c == '\u7777') {
                formatArchive = newFormat.toString();
            } else if (c == '\u7778') {
                oldFormat.setLength(0);
                newFormat.setLength(0);
                newFormat.append(formatArchive);
                formatChange.setLength(0);
                formatChange.append(formatArchive);
            } else {
                if (!newFormat.toString().equals(oldFormat.toString())) {
                    out.append(formatChange);
                    formatChange.setLength(0);
                    oldFormat.setLength(0);
                    oldFormat.append(newFormat);
                }
                out.append(c);
                if (c == '\n') {
                    formatChange.insert(0, oldFormat);
                    oldFormat.setLength(0);
                    newFormat.append(formatChange.toString());
                }
            }
        }
        return out.toString();
    }

    /**
     * Save files from the given plugins jar file to its subfolder in the plugins folder. The files will only be saved
     * if they doesn't exists in the plugins subfolder.
     *
     * @param pl        instane of the plugin
     * @param fileNames names of the saveable files
     */
    public static void saveResources(Plugin pl, String... fileNames) {
        Logger log = pl.getLogger();
        File df = pl.getDataFolder();
        ClassLoader cl = pl.getClass().getClassLoader();
        for (String fn : fileNames) {
            try {
                File f = new File(df + File.separator + fn);
                if (!f.exists()) {
                    InputStream is = cl.getResourceAsStream(fn);
                    if (is == null) {
                        log(pl, "Error, the requested file (" + fn + ") is missing from the plugins jar file.");
                    } else {
                        StreamUtils.toFile(is, f);
                    }
                }
            } catch (Throwable e) {
                log.severe("Error, on copying file (" + fn + "): ");
                e.printStackTrace();
            }
        }
    }

    /**
     * Set maximum length of a String by cutting the redundant characters off from it
     *
     * @param in  input String
     * @param len maximum length
     * @return The cutted String, which will maximally len characters.
     */
    public static String setLength(String in, int len) {
        return in.length() > len ? in.substring(0, len) : in;
    }
}