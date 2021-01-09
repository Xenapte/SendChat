package info.bcrc.mc.sendchat;

import org.bukkit.plugin.java.JavaPlugin;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class SendChat extends JavaPlugin implements Listener, TabCompleter {
  private String rawUrl, rawJoin, rawQuit, rawChat, rawAdvancement, rawSay, rawMe, rawPost,
  version = this.getDescription().getVersion(), method,
  msgPH = "\\{message\\}", playerPH = "\\{player\\}", chatPH = "\\{chat\\}",
  verPH = "\\{version\\}", advancementPH = "\\{advancement\\}";
  private FileConfiguration config;
  
  private String escapeJson(String text) {
    text = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\\b")
        .replace("\f", "\\\f")
        .replace("\n", "\\\n")
        .replace("\r", "\\\r")
        .replace("\t", "\\\t");
    return text;
  }
  private final HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();
  private void postChat(String rawText) {
    try {
      String urlText = URLEncoder.encode(rawText, "UTF-8");
      HttpRequest request;
      if (method.equalsIgnoreCase("post-form")) {
        request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(rawPost.replaceAll(msgPH, urlText)))
            .uri(URI.create(rawUrl))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .build();
      } else if (method.equalsIgnoreCase("post-json")) {
          request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(rawPost.replaceAll(msgPH, escapeJson(rawText).replace("\\", "\\\\"))))
            .uri(URI.create(rawUrl))
            .header("Content-Type", "application/json; charset=UTF-8")
            .build();
      } else { // GET
        request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(rawUrl.replaceAll(msgPH, urlText)))
            .build();
      };
      try {
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      };
    } catch (IOException uee) {
      uee.printStackTrace();
    };
  }


  private void disableSendChat() {
    this.getServer().getPluginManager().disablePlugin(this);
  }
  
  private void saveSCConfig() {
    // default
    config.addDefault("destination.url", "");
    config.addDefault("destination.method", "get");
    config.addDefault("destination.post-data", "");
    config.addDefault("server-start", "Server started with SendChat v{version}");
    config.addDefault("server-stop", "Server stopped");
    config.addDefault("plugin-reload", "SendChat v{version} reloaded");
    config.addDefault("plugin-shutdown", "SendChat v{version} shut down");
    config.addDefault("join", "{player} joined the game");
    config.addDefault("quit", "{player} left the game");
    config.addDefault("chat", "<{player}> {chat}");
    config.addDefault("advancement", "{player} has made the advancement [{advancement}]");
    config.addDefault("say-command", "[{player}] {chat}");
    config.addDefault("me-command", "* {player} {chat}");
    
    config.options().copyDefaults(true);
    saveDefaultConfig();
    saveConfig();
  }
  
  private void loadSCConfig() {
    rawJoin = config.getString("join").trim();
    rawQuit = config.getString("quit").trim();
    rawChat = config.getString("chat").trim();
    rawAdvancement = config.getString("advancement").trim();
    rawSay = config.getString("say-command").trim();
    rawMe = config.getString("me-command").trim();
    method = config.getString("destination.method").trim();
    if (method.equalsIgnoreCase("post-form") || method.equalsIgnoreCase("post-json")) {
      rawPost = config.getString("destination.post-data").trim();
    };
  }
  
  private boolean processUrl() {
    rawUrl = config.getString("destination.url").trim();
    // disable the plugin when url is blank
    if (rawUrl == "") {
      getLogger().warning("URL for message posting is invalid. SendChat will be disabled until you provide a valid link and restart/reload the server!");
      disableSendChat();
      return false;
    };
    return true;
  }
  
  
  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    config = getConfig();
    saveSCConfig();
    loadSCConfig();
    if (processUrl()) {
      getLogger().info("SendChat v" + version + " enabled successfully!");
      postChat(config.getString("server-start").trim().replaceAll(verPH, version));
    } else {
      return;
    };
  }
  
  @Override
  public void onDisable() {
    postChat(config.getString("server-stop").trim().replaceAll(verPH, version));
  }
  
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (cmd.getName().equalsIgnoreCase("sendchat")) {
      if (args.length > 0) {
        if (args[0].equalsIgnoreCase("reload")) {
          reloadConfig();
          saveSCConfig();
          config = getConfig();
          loadSCConfig();
          if (processUrl()) {
            sender.sendMessage("SendChat v" + version + " reloaded successfully!");
            postChat(config.getString("plugin-reload").trim().replaceAll(verPH, version));
          };
        } else if (args[0].equalsIgnoreCase("shutdown")) {
          if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("SendChat v" + version + " shutting down ...");
            postChat(config.getString("plugin-shutdown").trim().replaceAll(verPH, version));
            disableSendChat();
          } else {
            sender.sendMessage("Note once shut down the plugin cannot be reactivated unless you reload the server!");
            sender.sendMessage("Use /sendchat shutdown confirm to confirm shutting it down.");
          };
        } else {
          return false;
        };
        return true;
      };
    };
    return false;
  }
  
  // Tab completion
  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (command.getName().equalsIgnoreCase("sendchat")) {
      if (args.length > 0 && args[0].equalsIgnoreCase("shutdown")) {
        return Arrays.asList("confirm");
      };
      return Arrays.asList("reload", "shutdown");
    };
    return null;
  }
  
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    postChat(rawJoin.replaceAll(playerPH, event.getPlayer().getName().replace("\\", "\\\\")));
  }
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    postChat(rawQuit.replaceAll(playerPH, event.getPlayer().getName().replace("\\", "\\\\")));
  }
  
  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    postChat(rawChat.replaceAll(playerPH, event.getPlayer().getName()).replaceAll(chatPH, event.getMessage().replace("\\", "\\\\")));
  }
  
  @EventHandler
  public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
    postChat(rawAdvancement.replaceAll(playerPH, event.getPlayer().getName()).replaceAll(advancementPH, event.getAdvancement().getKey().getKey()));
  }

  @EventHandler
  public void onPlayerBroadcast(PlayerCommandPreprocessEvent event) {
    if (event.getMessage().substring(0, 5).equalsIgnoreCase("/say ")) {
      postChat(rawSay.replaceAll(playerPH, event.getPlayer().getName()).replaceAll(chatPH, event.getMessage().substring(5).replace("\\", "\\\\")));
    } else if (event.getMessage().substring(0, 4).equalsIgnoreCase("/me ")) {
      postChat(rawMe.replaceAll(playerPH, event.getPlayer().getName()).replaceAll(chatPH, event.getMessage().substring(4).replace("\\", "\\\\")));
    };
  }
  @EventHandler
  public void onServerBroadcast(ServerCommandEvent event) {
    if (event.getCommand().substring(0, 4).equalsIgnoreCase("say ")) {
      postChat(rawSay.replaceAll(playerPH, event.getSender().getName()).replaceAll(chatPH, event.getCommand().substring(4).replace("\\", "\\\\")));
    } else if (event.getCommand().substring(0, 3).equalsIgnoreCase("me ")) {
      postChat(rawMe.replaceAll(playerPH, event.getSender().getName()).replaceAll(chatPH, event.getCommand().substring(3).replace("\\", "\\\\")));
    };
  }
}
