package gyurix.bungeelib;


import gyurix.bungeelib.command.plugin.CommandMatcher;
import gyurix.bungeelib.configfile.DefaultSerializers;
import gyurix.bungeelib.json.JsonAPI;
import gyurix.bungeelib.protocol.ProtocolAPI;
import gyurix.bungeelib.utils.BU;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.event.EventHandler;

import javax.script.ScriptEngineManager;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;

import static gyurix.bungeelib.utils.BU.*;

public class BungeeLib extends Plugin implements Listener {
  public static GlobalLangFile.PluginLang lang;

  @Override
  public void onLoad() {
    bl = this;
    saveResources(this, "config.yml", "lang.yml");
    lang = GlobalLangFile.loadLF("bungeelib", getDataFolder() + File.separator + "lang.yml");
    js = new ScriptEngineManager().getEngineByName("JavaScript");
    bc = BungeeCord.getInstance();
    cs = ConsoleCommandSender.getInstance();
    pm = bc.getPluginManager();
    pm.registerListener(this, this);
    pa = new ProtocolAPI();
    sch = bc.getScheduler();
    DefaultSerializers.init();
    CommandMatcher.registerCustomMatchers();
  }

  @Override
  public void onDisable() {

  }

  @EventHandler
  public void onPluginMessage(PluginMessageEvent e) {
    if (!(e.getReceiver() instanceof ProxiedPlayer))
      return;
    if (e.getTag().equals("BungeeCord")) {
      try {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
        String sub = in.readUTF();
        switch (sub) {
          case "BungeeCommand": {
            String[] pls = in.readUTF().split(",");
            String[] cmds = JsonAPI.deserialize(in.readUTF(), String[].class);
            for (String pn : pls) {
              ProxiedPlayer p = bc.getPlayer(pn);
              if (p == null)
                continue;
              for (String c : cmds) {
                if (c.startsWith("CHAT:"))
                  p.chat(c.substring(5));
              }
            }
            e.setCancelled(true);
          }
        }
      } catch (Throwable err) {
        BU.error(cs, err, "BungeeLib", "gyurix");
      }
    }
  }

  public void onPreLogin(PreLoginEvent e) {

  }
}
