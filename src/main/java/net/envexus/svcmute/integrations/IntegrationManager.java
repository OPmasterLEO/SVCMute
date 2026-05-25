package net.envexus.svcmute.integrations;

import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.advancedbans.AdvancedBansMuteChecker;
import net.envexus.svcmute.integrations.advancedbanx.AdvancedBanXMuteChecker;
import net.envexus.svcmute.integrations.essentials.EssentialsMuteChecker;
import net.envexus.svcmute.integrations.litebans.LiteBansMuteChecker;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.util.SchedulerBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IntegrationManager {
    private static final long CHECK_CACHE_TTL_MS = 1000L;
    private static final long CACHE_CLEANUP_PERIOD_TICKS = 20L * 60L;

    private final List<MuteChecker> muteCheckers = new ArrayList<>();
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, CachedMuteState> muteStateCache = new ConcurrentHashMap<>();
    private final SQLiteHelper sqliteHelper;
    private final SchedulerBridge.TaskHandle cleanupTask;

    public IntegrationManager(SVCMute plugin, SQLiteHelper sqliteHelper) {
        this.sqliteHelper = sqliteHelper;
        registerPlugins();
        initializeStoredMutes();
        this.cleanupTask = SchedulerBridge.runAsyncTimer(plugin, this::cleanupExpiredState, CACHE_CLEANUP_PERIOD_TICKS, CACHE_CLEANUP_PERIOD_TICKS);
    }

    private void registerPlugins() {
        Plugin liteBansPlugin = Bukkit.getPluginManager().getPlugin("LiteBans");
        boolean isLiteBansEnabled = liteBansPlugin != null && liteBansPlugin.isEnabled();

        Plugin advancedBansPlugin = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        boolean isAdvancedBanEnabled = advancedBansPlugin != null && advancedBansPlugin.isEnabled();

        Plugin advancedBanXPlugin = Bukkit.getPluginManager().getPlugin("AdvancedBanX");
        boolean isAdvancedBanXEnabled = advancedBanXPlugin != null && advancedBanXPlugin.isEnabled();

        if (!isLiteBansEnabled && !isAdvancedBanEnabled && !isAdvancedBanXEnabled) {
            Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
                muteCheckers.add(new EssentialsMuteChecker(essentialsPlugin));
                SVCMute.LOGGER.info("Adding Essentials Mute Checker");
            }
        }

        if (isLiteBansEnabled) {
            muteCheckers.add(new LiteBansMuteChecker());
        }

        if (isAdvancedBanEnabled) {
            muteCheckers.add(new AdvancedBansMuteChecker(advancedBansPlugin));
        }

        if (isAdvancedBanXEnabled) {
            muteCheckers.add(new AdvancedBanXMuteChecker(advancedBanXPlugin));
        }
    }

    private void initializeStoredMutes() {
        long now = System.currentTimeMillis();
        sqliteHelper.removeExpiredMutes(now);
        mutedPlayers.putAll(sqliteHelper.getActiveMutes(now));
    }

    public boolean isPlayerMuted(Player player) {
        long now = System.currentTimeMillis();
        long maxUnmuteTimestamp = getMaxUnmuteTimestamp(player, now);
        return maxUnmuteTimestamp > now;
    }

    public void addMutedPlayer(UUID playerUUID, long unmuteTime) {
        mutedPlayers.put(playerUUID, unmuteTime);
        muteStateCache.remove(playerUUID);
    }

    public void removeMutedPlayer(UUID playerUUID) {
        mutedPlayers.remove(playerUUID);
        muteStateCache.remove(playerUUID);
    }

    public boolean hasMutedPlayer(UUID playerUUID) {
        long now = System.currentTimeMillis();
        return getManualUnmuteTime(playerUUID, now) > now;
    }

    public void clearPlayerCaches(UUID playerUUID) {
        muteStateCache.remove(playerUUID);
    }

    public void shutdown() {
        cleanupTask.cancel();
        mutedPlayers.clear();
        muteStateCache.clear();
    }

    public long getRemainingMilliseconds(Player player) {
        long now = System.currentTimeMillis();
        long unmuteTimestamp = getMaxUnmuteTimestamp(player, now);
        long remainingTime = unmuteTimestamp - now;
        if (remainingTime >= 0) {
            return remainingTime;
        }
        return -1;
    }

    public String getRemainingTime(Player player) {
        var ms = getRemainingMilliseconds(player);

        if (ms < 0) {
            return "0s";
        }

        return formatTime(ms);
    }

    private String formatTime(long remainingTime) {
        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + hours % 24 + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes % 60 + "m";
        } else if (minutes > 0) {
            return minutes + "m " + seconds % 60 + "s";
        } else {
            return seconds + "s";
        }
    }

    private long getMaxUnmuteTimestamp(Player player, long now) {
        UUID playerUUID = player.getUniqueId();
        long manualUnmute = getManualUnmuteTime(playerUUID, now);

        CachedMuteState cachedState = muteStateCache.get(playerUUID);
        if (cachedState != null && cachedState.cacheExpiresAt > now) {
            return Math.max(manualUnmute, cachedState.unmuteTime);
        }
        if (cachedState != null) {
            muteStateCache.remove(playerUUID, cachedState);
        }

        long externalUnmute = -1;
        for (MuteChecker checker : muteCheckers) {
            long checkerUnmute = checker.getUnmuteTime(player);
            if (checkerUnmute > now) {
                externalUnmute = Math.max(externalUnmute, checkerUnmute);
            }
        }

        long mergedUnmute = Math.max(manualUnmute, externalUnmute);
        muteStateCache.put(playerUUID, new CachedMuteState(mergedUnmute, now + CHECK_CACHE_TTL_MS));
        return mergedUnmute;
    }

    private void cleanupExpiredState() {
        long now = System.currentTimeMillis();
        sqliteHelper.removeExpiredMutes(now);
        mutedPlayers.entrySet().removeIf(entry -> entry.getValue() <= now);
        muteStateCache.entrySet().removeIf(entry -> entry.getValue().cacheExpiresAt <= now);
    }

    private long getManualUnmuteTime(UUID playerUUID, long now) {
        Long manualUnmute = mutedPlayers.get(playerUUID);
        if (manualUnmute == null) {
            return -1;
        }
        if (manualUnmute <= now) {
            mutedPlayers.remove(playerUUID, manualUnmute);
            return -1;
        }
        return manualUnmute;
    }

    private static final class CachedMuteState {
        private final long unmuteTime;
        private final long cacheExpiresAt;

        private CachedMuteState(long unmuteTime, long cacheExpiresAt) {
            this.unmuteTime = unmuteTime;
            this.cacheExpiresAt = cacheExpiresAt;
        }
    }
}
