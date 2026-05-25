package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.util.SchedulerBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CommandAlias("svcmute")
@CommandPermission("voicechat.mute")
public class SVCMuteCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final IntegrationManager integrationManager;

    public SVCMuteCommand(SQLiteHelper db, SVCMute plugin, IntegrationManager integrationManager) {
        this.db = db;
        this.plugin = plugin;
        this.integrationManager = integrationManager;
    }

    @Default
    @Syntax("<player> <time>")
    @Description("Mute a player from voice chat for a specified time.")
    public void onMute(CommandSender sender, String playerName, String timeStr) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("Player not found.");
            return;
        }

        long muteDurationMillis = parseTime(timeStr);
        if (muteDurationMillis <= 0) {
            sender.sendMessage("Invalid time format. Use examples: 1s, 5m, 2d.");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        long unmuteTime = System.currentTimeMillis() + muteDurationMillis;

        integrationManager.addMutedPlayer(playerUUID, unmuteTime);
        sender.sendMessage(playerName + " has been muted for " + timeStr + ".");

        SchedulerBridge.runAsync(this.plugin, () -> db.addMute(playerUUID.toString(), unmuteTime));
    }

    private long parseTime(String timeStr) {
        try {
            char unit = timeStr.charAt(timeStr.length() - 1);
            long amount = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));

            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(amount);
                case 'm' -> TimeUnit.MINUTES.toMillis(amount);
                case 'h' -> TimeUnit.HOURS.toMillis(amount);
                case 'd' -> TimeUnit.DAYS.toMillis(amount);
                default -> -1;
            };
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }
    }
}
