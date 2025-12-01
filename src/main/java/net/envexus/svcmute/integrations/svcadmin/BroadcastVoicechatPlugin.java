package net.envexus.svcmute.integrations.svcadmin;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class BroadcastVoicechatPlugin implements VoicechatPlugin {
   public static Permission BROADCAST_PERMISSION;

   public String getPluginId() {
      return "SimpleVoiceChatAdmin";
   }

   public void initialize(VoicechatApi api) {
   }

   public void registerEvents(EventRegistration registration) {
      registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
   }

   private void onMicrophone(MicrophonePacketEvent event) {
      if (event.getSenderConnection() != null) {
         Object var3 = event.getSenderConnection().getPlayer().getPlayer();
         if (var3 instanceof Player) {
            Player player = (Player)var3;
            if (player.hasPermission(BROADCAST_PERMISSION)) {
               Group group = event.getSenderConnection().getGroup();
               if (group != null) {
                  if (group.getName().strip().equalsIgnoreCase("broadcast")) {
                     event.cancel();
                     VoicechatServerApi api = event.getVoicechat();
                     Iterator var5 = Bukkit.getServer().getOnlinePlayers().iterator();

                     while(var5.hasNext()) {
                        Player onlinePlayer = (Player)var5.next();
                        if (!onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                           VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUniqueId());
                           if (connection != null) {
                              api.sendStaticSoundPacketTo(connection, ((MicrophonePacket)event.getPacket()).toStaticSoundPacket());
                           }
                        }
                     }

                  }
               }
            }
         }
      }
   }

   static {
      BROADCAST_PERMISSION = new Permission("svca.broadcast", PermissionDefault.OP);
   }
}
