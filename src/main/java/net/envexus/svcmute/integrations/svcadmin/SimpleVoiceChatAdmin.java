package net.envexus.svcmute.integrations.svcadmin;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.Group.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.envexus.svcmute.SVCMute;
import java.util.logging.Logger;

public final class SimpleVoiceChatAdmin implements CommandExecutor {
   public static final Logger LOGGER = Logger.getLogger(SimpleVoiceChatAdmin.class.getName());

   private final SVCMute plugin;
   private final SVCPlugin svcPlugin;
   private BroadcastVoicechatPlugin voicechatPlugin;

   public SimpleVoiceChatAdmin(SVCMute plugin, SVCPlugin svcPlugin) {
      this.plugin = plugin;
      this.svcPlugin = svcPlugin;
   }

   // Called from SVCMute when the BukkitVoicechatService is available
   public void register(BukkitVoicechatService service) {
      this.voicechatPlugin = new BroadcastVoicechatPlugin();
      service.registerPlugin(this.voicechatPlugin);

      // Register command executors on the main plugin
      if (plugin.getCommand("adminjoin") != null) plugin.getCommand("adminjoin").setExecutor(this);
      if (plugin.getCommand("broadcastvoice") != null) plugin.getCommand("broadcastvoice").setExecutor(this);

      LOGGER.info("Voice chat broadcast plugin registered successfully.");
   }

   public BroadcastVoicechatPlugin getBroadcastPlugin() {
      return this.voicechatPlugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("broadcastvoice")) {
         return this.handleCreateGroupCommand(sender);
      } else if (command.getName().equalsIgnoreCase("adminjoin")) {
         if (sender instanceof Player player) {
            if (player.hasPermission("svca.join")) {
               if (args.length > 0) {
                  String groupName = String.join(" ", args);
                  try {
                     VoicechatServerApi api = svcPlugin.getVoicechatApi();
                     if (api == null) {
                        player.sendMessage("VoiceChat API not available.");
                        return true;
                     }

                     VoicechatConnection connection = api.getConnectionOf(player.getUniqueId());
                     if (connection == null) {
                        player.sendMessage("Connection error.");
                        return true;
                     }

                     for (Group group : api.getGroups()) {
                        if (group.getName().equals(groupName)) {
                           connection.setGroup(group);
                           player.sendMessage("§rYou joined group: §a" + groupName);
                           return true;
                        }
                     }

                     player.sendMessage("§rGroup §a" + groupName + " §rnot found.");
                  } catch (Exception e) {
                     player.sendMessage("Error on group join.");
                     e.printStackTrace();
                  }
               } else {
                  player.sendMessage("Please provide a group name.");
               }
            } else {
               player.sendMessage("§cYou don't have the permission for this.");
            }
         } else {
            sender.sendMessage("This command can only be executed by a player.");
         }
         return true;
      }
      return false;
   }

   private boolean handleCreateGroupCommand(CommandSender sender) {
      if (sender instanceof Player player) {
         if (!player.hasPermission("svca.broadcast")) {
            player.sendMessage("§cYou don't have the permission for this.");
            return true;
         } else {
            VoicechatServerApi api = svcPlugin.getVoicechatApi();
            if (api == null) {
               player.sendMessage("VoiceChat API not available.");
               return true;
            } else {
               Group group = api.groupBuilder().setPersistent(false).setName("broadcast").setPassword(this.plugin.getServer().getIp()).setType(Type.OPEN).build();
               VoicechatConnection connection = api.getConnectionOf(player.getUniqueId());
               if (connection == null) {
                  player.sendMessage("Connection error.");
                  return true;
               }
               connection.setGroup(group);
               player.sendMessage("§rJoined &aBroadcast §rgroup.");
               return true;
            }
         }
      } else {
         sender.sendMessage("Can only be executed by a player.");
         return true;
      }
   }
}
