package net.envexus.svcmute.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public final class SchedulerBridge {

    private SchedulerBridge() {
    }

    public interface TaskHandle {
        void cancel();
    }

    public static TaskHandle runAsync(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
            return task::cancel;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        return task::cancel;
    }

    public static TaskHandle runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runDelayed(
                    plugin,
                    scheduledTask -> runnable.run(),
                    ticksToMillis(delayTicks),
                    TimeUnit.MILLISECONDS
            );
            return task::cancel;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        return task::cancel;
    }

    public static TaskHandle runAsyncTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (isFolia()) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    ticksToMillis(initialDelayTicks),
                    ticksToMillis(periodTicks),
                    TimeUnit.MILLISECONDS
            );
            return task::cancel;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelayTicks, periodTicks);
        return task::cancel;
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static long ticksToMillis(long ticks) {
        return ticks * 50L;
    }
}