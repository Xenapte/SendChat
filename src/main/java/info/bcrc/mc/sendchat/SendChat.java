package info.bcrc.mc.sendchat;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException; 
import java.net.HttpURLConnection; 
import java.net.URL; 
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public final class SendChat extends JavaPlugin {
  private String rawUrl, rawPost,
      version = this.getDescription().getVersion(), method,
      msgPH = "\\{message\\}", verPH = "\\{version\\}";
  private Boolean enabledServerStatus, enabledPluginStatus;
  private Boolean errorStatus = false;
  private FileConfiguration config;

  ChatListener cl;
  
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
  protected void postChat(String rawText) {
    try {
      HttpURLConnection connection;
      if (method.equalsIgnoreCase("post-form")) {
        String urlText = URLEncoder.encode(rawText, "UTF-8");
        URL url = new URL(rawUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setDoOutput(true);
        byte[] input = rawPost.replaceAll(msgPH, urlText).getBytes("utf-8");
        connection.getOutputStream().write(input, 0, input.length);
      } else if (method.equalsIgnoreCase("post-json")) {
        URL url = new URL(rawUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        byte[] input = rawPost.replaceAll(msgPH, escapeJson(rawText).replace("\\", "\\\\")).getBytes("utf-8");
        connection.getOutputStream().write(input, 0, input.length);
      } else { // GET
        String urlText = URLEncoder.encode(rawText, "UTF-8");
        URL url = new URL(rawUrl.replaceAll(msgPH, urlText));
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
      };
      connection.setInstanceFollowRedirects(true);
      connection.getInputStream();
      connection.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    };
  }


  private void disableSendChat() {
    errorStatus = true;
    this.getServer().getPluginManager().disablePlugin(this);
  }
  
  private void saveSCConfig() {
    config.addDefault("destination.url", "");
    config.addDefault("destination.method", "get");
    config.addDefault("destination.post-data", "");
    config.addDefault("server-status.server-start", "Server started with SendChat v{version}");
    config.addDefault("server-status.server-stop", "Server stopped");
    config.addDefault("plugin-status.reload", "SendChat v{version} reloaded");
    config.addDefault("plugin-status.shutdown", "SendChat v{version} shut down");
    config.addDefault("player-log.join", "{player} joined the game");
    config.addDefault("player-log.quit", "{player} left the game");
    config.addDefault("public-message.chat", "<{player}> {chat}");
    config.addDefault("public-message.say-command", "[{player}] {chat}");
    config.addDefault("public-message.me-command", "* {player} {chat}");
    config.addDefault("player-interaction.advancement", "{player} has made the advancement [{advancement}]");
    config.addDefault("player-status.death", "{chat}");

    config.addDefault("server-status.enabled", true);
    config.addDefault("player-log.enabled", true);
    config.addDefault("public-message.enabled", true);
    config.addDefault("player-interaction.enabled", true);
    config.addDefault("player-status.enabled", true);
    config.addDefault("plugin-status.enabled", true);
    config.addDefault("player-interaction.treat-recipes-as-advancements", false);
    
    config.options().copyDefaults(true);
    saveDefaultConfig();
    saveConfig();
  }
  
  private void loadSCConfig() {
    cl.rawJoin = config.getString("player-log.join").trim();
    cl.rawQuit = config.getString("player-log.quit").trim();
    cl.rawChat = config.getString("public-message.chat").trim();
    cl.rawSay = config.getString("public-message.say-command").trim();
    cl.rawMe = config.getString("public-message.me-command").trim();
    cl.rawAdvancement = config.getString("player-interaction.advancement").trim();
    cl.rawDeath = config.getString("player-status.death").trim();
    
    enabledServerStatus = config.getBoolean("server-status.enabled");
    cl.enabledPlayerLog = config.getBoolean("player-log.enabled");
    cl.enabledPublicMsg = config.getBoolean("public-message.enabled");
    cl.enabledPlayerInteract = config.getBoolean("player-interaction.enabled");
    cl.enabledPlayerStatus = config.getBoolean("player-status.enabled");
    enabledPluginStatus = config.getBoolean("plugin-status.enabled");
    cl.recipeAdvancement = config.getBoolean("player-interaction.treat-recipes-as-advancements");
    
    method = config.getString("destination.method").trim();
    if (method.equalsIgnoreCase("post-form") || method.equalsIgnoreCase("post-json")) {
      rawPost = config.getString("destination.post-data").trim();
    };
  }
  
  private boolean processUrl() {
    rawUrl = config.getString("destination.url").trim();
    if (rawUrl == "") {
      getLogger().warning("URL for message posting is invalid. ");
      getLogger().warning("SendChat will be disabled until you provide a valid link and restart/reload the server!");
      disableSendChat();
      return false;
    };
    return true;
  }
  
  
  @Override
  public void onEnable() {
    cl = new ChatListener(this);
    getServer().getPluginManager().registerEvents(cl, this);
    config = getConfig();
    saveSCConfig();
    loadSCConfig();
    if (processUrl()) {
      if (!errorStatus) {
        getLogger().info("SendChat v" + version + " enabled successfully!");
        if (enabledServerStatus) {
          postChat(config.getString("server-status.server-start").trim().replaceAll(verPH, version));
        };
      };
    } else {
      return;
    };
  }
  
  @Override
  public void onDisable() {
    if (enabledServerStatus && !errorStatus) {
      postChat(config.getString("server-status.server-stop").trim().replaceAll(verPH, version));
    };
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
            sender.sendMessage(ChatColor.GREEN + "SendChat v" + version + " reloaded successfully!");
            if (enabledPluginStatus) {
              postChat(config.getString("plugin-status.reload").trim().replaceAll(verPH, version));
            };
          };
        } else if (args[0].equalsIgnoreCase("shutdown")) {
          if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("SendChat v" + version + " shutting down ...");
            if (enabledPluginStatus) {
              postChat(config.getString("plugin-status.shutdown").trim().replaceAll(verPH, version));
            };
            disableSendChat();
          } else {
            sender.sendMessage(ChatColor.RED + "Note once shut down the plugin cannot be reactivated unless you reload the server!");
            sender.sendMessage(ChatColor.RED + "Use /sendchat shutdown confirm to confirm shutting it down.");
          };
        } else {
          return false;
        };
        return true;
      };
    };
    return false;
  }

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
}
