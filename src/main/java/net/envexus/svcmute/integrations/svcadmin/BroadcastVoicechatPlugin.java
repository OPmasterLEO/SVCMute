package net.envexus.svcmute.integrations.svcadmin;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class BroadcastVoicechatPlugin implements VoicechatPlugin {
   public static Permission BROADCAST_PERMISSION;

   @Override
   public String getPluginId() {
      return "SimpleVoiceChatAdmin";
   }

   @Override
   public void initialize(VoicechatApi api) {
   }

   @Override
   public void registerEvents(EventRegistration registration) {
      registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
   }

   private void onMicrophone(MicrophonePacketEvent event) {
      var senderConnection = event.getSenderConnection();
      if (senderConnection == null || senderConnection.getPlayer() == null) {
         return;
      }

      Object senderPlayer = senderConnection.getPlayer().getPlayer();
      if (!(senderPlayer instanceof Player player)) {
         return;
      }

      if (!player.hasPermission(BROADCAST_PERMISSION)) {
         return;
      }

      Group group = senderConnection.getGroup();
      if (group == null || !group.getName().equalsIgnoreCase("broadcast")) {
         return;
      }

      event.cancel();
      VoicechatServerApi api = event.getVoicechat();
      MicrophonePacket microphonePacket = (MicrophonePacket) event.getPacket();
      var staticPacket = microphonePacket.toStaticSoundPacket();

      for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
         if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
            continue;
         }

         VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUniqueId());
         if (connection != null) {
            api.sendStaticSoundPacketTo(connection, staticPacket);
         }
      }
   }

   static {
      BROADCAST_PERMISSION = new Permission("svca.broadcast", PermissionDefault.OP);
   }
}
