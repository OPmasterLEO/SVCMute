package net.envexus.svcmute;

import co.aikar.commands.BukkitCommandManager;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import net.envexus.svcmute.commands.SCVUnmuteCommand;
import net.envexus.svcmute.commands.SVCMuteCommand;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.integrations.svcadmin.SimpleVoiceChatAdmin;
import net.envexus.svcmute.placeholders.SVCMutePlaceholderExpansion;
import net.envexus.svcmute.integrations.svcadmin.SVCPlugin;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class SVCMute extends JavaPlugin {

    private static final String PLUGIN_ID = "mutecheck_voicechat";
    public static final Logger LOGGER = Logger.getLogger(PLUGIN_ID);

    private MuteCheckPlugin voicechatPlugin;
    private SQLiteHelper sqliteHelper;
    private IntegrationManager integrationManager;

    // new fields
    private SVCPlugin svcPlugin;
    private SimpleVoiceChatAdmin svcAdmin;

    @Override
    public void onEnable() {
        ConfigurationManager messageManager = new ConfigurationManager(this);

        // Initialize SQLiteHelper
        sqliteHelper = new SQLiteHelper(this);

        // Initialize IntegrationManager with SQLiteHelper
        integrationManager = new IntegrationManager(sqliteHelper);

        // Register voice chat plugin(s)
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            // SVCPlugin exposes the VoicechatServerApi once initialized
            svcPlugin = new SVCPlugin();
            service.registerPlugin(svcPlugin);

            // Register mute-check plugin
            voicechatPlugin = new MuteCheckPlugin(integrationManager, messageManager);
            service.registerPlugin(voicechatPlugin);

            // Create and register the admin helper which will register broadcast plugin and command executors
            svcAdmin = new SimpleVoiceChatAdmin(this, svcPlugin);
            svcAdmin.register(service);

            LOGGER.info("Successfully registered voice chat plugins.");
        } else {
            LOGGER.info("Failed to register voice chat plugins (BukkitVoicechatService unavailable).");
        }

        if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SVCMutePlaceholderExpansion(integrationManager).register();
        }
        
        // Initialize ACF Command Manager
        BukkitCommandManager manager = new BukkitCommandManager(this);

        // Register commands (plugin commands remain handled by ACF or Bukkit executors)
        manager.registerCommand(new SVCMuteCommand(sqliteHelper, this, integrationManager));
        manager.registerCommand(new SCVUnmuteCommand(sqliteHelper, integrationManager));
    }

    @Override
    public void onDisable() {
        // Unregister voicechat-related plugins from the ServicesManager
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
            LOGGER.info("Successfully unregistered voice chat mutecheck plugin");
        }
        if (svcAdmin != null && svcAdmin.getBroadcastPlugin() != null) {
            getServer().getServicesManager().unregister(svcAdmin.getBroadcastPlugin());
            LOGGER.info("Successfully unregistered voice chat broadcast plugin");
        }
        if (svcPlugin != null) {
            getServer().getServicesManager().unregister(svcPlugin);
            LOGGER.info("Successfully unregistered svc helper plugin");
        }
    }
}

// Removed non-public top-level SVCPlugin as it's now in net.envexus.svcmute.integrations.svcadmin
