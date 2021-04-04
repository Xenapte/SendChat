package info.bcrc.mc.sendchat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ChatListener implements Listener {
  protected Boolean enabledPlayerLog, enabledPublicMsg,
      enabledPlayerInteract, enabledPlayerStatus, recipeAdvancement;
  protected String rawJoin, rawQuit, rawChat, rawAdvancement, rawSay, rawMe, rawDeath,
      playerPH = "\\{player\\}", chatPH = "\\{chat\\}", advancementPH = "\\{advancement\\}";
  protected SendChat sc;

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (enabledPlayerLog) {
      sc.postChat(rawJoin.replaceAll(playerPH, event.getPlayer().getName().replace("\\", "\\\\")));
    };
  }
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (enabledPlayerLog) {
      sc.postChat(rawQuit.replaceAll(playerPH, event.getPlayer().getName().replace("\\", "\\\\")));
    };
  }
  
  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    if (enabledPublicMsg) {
      sc.postChat(rawChat.replaceAll(playerPH, event.getPlayer().getName())
          .replaceAll(chatPH, event.getMessage().replace("\\", "\\\\")));
    };
  }
  
  @EventHandler
  public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
    if (enabledPlayerInteract) {
      String advancementName = event.getAdvancement().getKey().getKey();
      // prevent new recipes also being sent
      if (!event.getAdvancement().getKey().getKey().startsWith("recipes") || recipeAdvancement) {
        sc.postChat(rawAdvancement.replaceAll(playerPH, event.getPlayer().getName())
            .replaceAll(advancementPH, advancementName));
      };
    };
  }
  
  
  private void sendPublicMsg(String command, String senderName) {
    if (enabledPublicMsg) {
      if (command.length() > 4 && command.substring(0, 4).equalsIgnoreCase("say ")) {
        sc.postChat(rawSay.replaceAll(playerPH, senderName)
            .replaceAll(chatPH, command.substring(4).replace("\\", "\\\\")));
      } else if (command.length() > 3 && command.substring(0, 3).equalsIgnoreCase("me ")) {
        sc.postChat(rawMe.replaceAll(playerPH, senderName)
            .replaceAll(chatPH, command.substring(3).replace("\\", "\\\\")));
      };
    };
  }

  @EventHandler
  public void onPlayerBroadcast(PlayerCommandPreprocessEvent event) {
    String command = event.getMessage();
    if (command.length() > 1 && command.substring(0, 1).equals("/")) {
      sendPublicMsg(event.getPlayer().getName(), command.substring(1));
    };
  }
  @EventHandler
  public void onServerBroadcast(ServerCommandEvent event) {
    sendPublicMsg(event.getCommand(), event.getSender().getName());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (enabledPlayerStatus) {
      sc.postChat(rawDeath.replaceAll(playerPH, event.getEntity().getName())
          .replaceAll(chatPH, event.getDeathMessage().replace("\\", "\\\\")));
    };
  }
}
