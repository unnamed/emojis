package team.unnamed.creativeglyphs.plugin.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creativeglyphs.plugin.CreativeGlyphsPlugin;

import java.io.IOException;
import java.util.logging.Level;

public final class GitHubUpdateChecker {
    private static final Permission UPDATE_NOTIFICATION_PERMISSION = new Permission(
            "creativeglyphs.update",
            "Allows the player to receive update notifications",
            PermissionDefault.OP
    );

    private static final String DOWNLOAD_URL = "https://spigotmc.org/resources/113722/";

    public static void checkAsync(final @NotNull CreativeGlyphsPlugin plugin) {
        ScheduleUtil.GLOBAL.runTaskAsynchronously(plugin, () -> check(plugin));
    }

    public static void check(final @NotNull CreativeGlyphsPlugin plugin) {
        // Don't check for updates if the user has disabled it
        if (!plugin.getConfig().getBoolean("check-for-updates")) {
            return;
        }

        // Check for updates
        final String latestTagName;
        try {
            latestTagName = GitHub.fetchLatestReleaseTagName("unnamed", "creative-glyphs");
        } catch (final IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
            return;
        }

        //noinspection deprecation
        final String currentVersion = plugin.getDescription().getVersion();

        if (currentVersion.equals(latestTagName)
                || (latestTagName.startsWith("v") && currentVersion.equals(latestTagName.substring(1)))) {
            // We are up-to-date!
            return;
        }

        // Register the update notifier
        Bukkit.getPluginManager().registerEvents(new UpdateAvailableNotifier(latestTagName), plugin);

        // Notify the console and online players
        notifyUpdateAvailable(Bukkit.getConsoleSender(), latestTagName);

        for (final var player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(UPDATE_NOTIFICATION_PERMISSION)) {
                notifyUpdateAvailable(player, latestTagName);
            }
        }
    }

    private static void notifyUpdateAvailable(final @NotNull Audience audience, final @NotNull String latestVersion) {
        final var pink = TextColor.color(0xff8df8);
        final var purple = TextColor.color(0xB545FF);
        audience.sendMessage(
                Component.text()
                        .append(Component.text(" [!] ", pink))
                        .append(Component.text("An update is available for "))
                        .append(Component.text("creative-glyphs", pink))
                        .append(Component.text(" ("))
                        .append(Component.text(latestVersion, pink))
                        .append(Component.text(")"))
                        .append(Component.newline())
                        .append(Component.text()
                                .content("Click to download")
                                .color(purple)
                                .hoverEvent(HoverEvent.showText(Component.text(DOWNLOAD_URL, pink)))
                                .clickEvent(ClickEvent.openUrl(DOWNLOAD_URL)))
                        .build());
    }

    private static class UpdateAvailableNotifier implements Listener {
        private final String latestVersion;

        public UpdateAvailableNotifier(final @NotNull String latestVersion) {
            this.latestVersion = latestVersion;
        }

        @EventHandler
        public void onJoin(final @NotNull PlayerJoinEvent event) {
            final var player = event.getPlayer();
            if (player.hasPermission(UPDATE_NOTIFICATION_PERMISSION)) {
                notifyUpdateAvailable(player, latestVersion);
            }
        }
    }
}
