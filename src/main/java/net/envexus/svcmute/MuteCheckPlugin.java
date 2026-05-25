package net.envexus.svcmute;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteCheckPlugin implements VoicechatPlugin {

    private final Set<UUID> notifiedPlayers;
    private final IntegrationManager integrationManager;
    private final ConfigurationManager configurationManager;

    public MuteCheckPlugin(IntegrationManager integrationManager, ConfigurationManager messagesManager) {
        this.notifiedPlayers = ConcurrentHashMap.newKeySet();
        this.integrationManager = integrationManager;
        this.configurationManager = messagesManager;
    }

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return "mutecheck_voicechat";
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        // Initialization logic if needed
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    public void clearPlayerState(UUID playerUUID) {
        notifiedPlayers.remove(playerUUID);
    }

    public void clearAllState() {
        notifiedPlayers.clear();
    }

    /**
     * This method is called whenever a player sends audio to the server via the voice chat.
     *
     * @param event the microphone packet event
     */
    private void onMicrophone(MicrophonePacketEvent event) {
        var senderConnection = event.getSenderConnection();
        if (senderConnection == null || senderConnection.getPlayer() == null) {
            return;
        }
        Object senderPlayer = senderConnection.getPlayer().getPlayer();
        if (!(senderPlayer instanceof Player player)) {
            return;
        }

        long remainingMilliseconds = integrationManager.getRemainingMilliseconds(player);
        if (remainingMilliseconds > 0) {
            event.cancel();

            String remainingTime = integrationManager.getRemainingTime(player);

            if (remainingTime == null) {
                remainingTime = "Unknown";
            }

            Component mutedMessage = configurationManager.getLocaleString(
                    "actionbar.muted",
                    Placeholder.parsed("remaining_time", remainingTime)
            );

            if (configurationManager.getConfig().getBoolean("actionbar", false)) {
                player.sendActionBar(mutedMessage);
            }

            if (configurationManager.getConfig().getBoolean("message", false) && notifiedPlayers.add(player.getUniqueId())) {
                player.sendMessage(mutedMessage);
            }
        } else {
            notifiedPlayers.remove(player.getUniqueId());
        }
    }

}
