package team.unnamed.creativeglyphs.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

// Original author: NCT-skyouo, NatureRevive
public class ScheduleUtil {
    private static boolean isFolia;
    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (Exception ignored) {
            isFolia = false;
        }
    }

    public static class GLOBAL {
        public static void runTask(Plugin plugin, Runnable task) {
            if (isFolia) {
                Bukkit.getGlobalRegionScheduler().run(plugin, (ignored) -> task.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }

        public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
            if (isFolia) {
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (ignored) -> task.run(), delay);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        }

        public static void runTaskTimer(Plugin plugin, Runnable task, long delay, long fixedRate) {
            if (isFolia) {
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (ignored) -> task.run(), delay, fixedRate);
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, fixedRate);
            }
        }

        public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
            if (isFolia) {
                Bukkit.getAsyncScheduler().runNow(plugin, (ignored) -> task.run());
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        }

        public static void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
            if (isFolia) {
                Bukkit.getAsyncScheduler().runDelayed(plugin, (ignored) -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
            } else {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            }
        }

        public static void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long fixedRate) {
            if (isFolia) {
                Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (ignored) -> task.run(), delay * 50L, fixedRate * 50L, TimeUnit.MILLISECONDS);
            } else {
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, fixedRate);
            }
        }
    }

    public static class REGION {
        public static void runTask(Plugin plugin, Chunk chunk, Runnable task) {
            if (isFolia) {
                Bukkit.getRegionScheduler().run(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), (ignored) -> task.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }

        public static void runTask(Plugin plugin, Location location, Runnable task) {
            if (isFolia) {
                Bukkit.getRegionScheduler().run(plugin, location, (ignored) -> task.run());
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }

        public static void runTaskLater(Plugin plugin, Chunk chunk, Runnable task, long delay) {
            if (isFolia) {
                Bukkit.getRegionScheduler().runDelayed(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), (ignored) -> task.run(), delay);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        }

        public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
            if (isFolia) {
                Bukkit.getRegionScheduler().runDelayed(plugin, location, (ignored) -> task.run(), delay);

            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        }

        public static void runTaskTimer(Plugin plugin, Chunk chunk, Runnable task, long delay, long fixedRate) {
            if (isFolia) {
                Bukkit.getRegionScheduler().runAtFixedRate(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), (ignored) -> task.run(), delay, fixedRate);
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, fixedRate);
            }
        }

        public static void runTaskTimer(Plugin plugin, Location location, Runnable task, long delay, long fixedRate) {
            if (isFolia) {
                Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (ignored) -> task.run(), delay, fixedRate);
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, fixedRate);
            }
        }
    }

    public static class ENTITY {
        public static void runTask(Plugin plugin, Entity entity, Runnable task) {
            if (isFolia) {
                entity.getScheduler().run(plugin, (ignored) -> task.run(), null);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }

        public static void runTaskLater(Plugin plugin, Entity entity, Runnable task, long delay) {
            if (isFolia) {
                entity.getScheduler().runDelayed(plugin, (ignored) -> task.run(), null, delay);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        }

        public static void runTaskTimer(Plugin plugin, Entity entity, Runnable task, long delay, long fixedRate) {
            if (isFolia) {
                entity.getScheduler().runAtFixedRate(plugin, (ignored) -> task.run(), null, delay, fixedRate);
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, fixedRate);
            }
        }
    }
}