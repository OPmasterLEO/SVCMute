package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.util.SchedulerBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("svcunmute")
@CommandPermission("voicechat.svcunmute")
@Description("Unmute a player from voice chat.")
public class SCVUnmuteCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final IntegrationManager integrationManager;

    public SCVUnmuteCommand(SQLiteHelper db, SVCMute plugin, IntegrationManager integrationManager) {
        this.db = db;
        this.plugin = plugin;
        this.integrationManager = integrationManager;
    }

    @Default
    @Syntax("<player>")
    @Description("Unmute a player from voice chat.")
    public void onUnmute(CommandSender sender, String playerName) {

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("Player not found.");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (!integrationManager.hasMutedPlayer(playerUUID)) {
            sender.sendMessage(playerName + " is not muted.");
            return;
        }

        integrationManager.removeMutedPlayer(playerUUID);
        sender.sendMessage(playerName + " has been unmuted.");
        SchedulerBridge.runAsync(this.plugin, () -> db.removeMute(playerUUID.toString()));
    }
}
