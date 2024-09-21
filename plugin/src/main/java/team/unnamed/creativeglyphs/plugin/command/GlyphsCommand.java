package team.unnamed.creativeglyphs.plugin.command;

import me.fixeddev.commandflow.annotated.CommandClass;
import me.fixeddev.commandflow.annotated.annotation.Command;
import me.fixeddev.commandflow.annotated.annotation.Named;
import me.fixeddev.commandflow.annotated.annotation.OptArg;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.central.CreativeCentralProvider;
import team.unnamed.creativeglyphs.Glyph;
import team.unnamed.creativeglyphs.cloud.FileCloudService;
import team.unnamed.creativeglyphs.plugin.CreativeGlyphsPlugin;
import team.unnamed.creativeglyphs.plugin.util.Permissions;
import team.unnamed.creativeglyphs.serialization.GlyphReader;
import team.unnamed.creativeglyphs.serialization.GlyphWriter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

@Command(names = { "glyphs", "glyph", "emojis", "emoji" })
public final class GlyphsCommand implements CommandClass {
    private final CreativeGlyphsPlugin plugin;

    public GlyphsCommand(final @NotNull CreativeGlyphsPlugin plugin) {
        this.plugin = requireNonNull(plugin, "plugin");
    }

    @Command(names = {"help", "?"}, permission = "emojis.admin")
    public void help(final @NotNull CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes(
                '&',
                plugin.getConfig().getString("messages.help", "Message not found")
        ));
    }

    @Command(names = "reload", permission = "emojis.admin")
    public void reload() {
        plugin.reloadConfig();
        plugin.registry().load();
    }

    @Command(names = "update", permission = "emojis.admin")
    public void update(final @NotNull CommandSender sender, final @NotNull @Named("id") String id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> execute(sender, id));
    }

    @Command(names = "edit", permission = "emojis.admin")
    public void edit(final @NotNull CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> edit0(sender));
    }

    private void edit0(final @NotNull CommandSender sender) {
        final var file = (Writable) (output -> GlyphWriter.mcglyph().write(output, plugin.registry().values()));
        final var id = FileCloudService.artemis().upload(file);
        final var url = "https://unnamed.team/project/glyphs?id=" + id;
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Edit the glyph list at the following URL: ");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "  " + ChatColor.UNDERLINE + url);
        sender.sendMessage("");
    }

    private void execute(CommandSender sender, String id) {
        try {
            final var file = FileCloudService.artemis().download(id);
            if (file == null) {
                sender.sendMessage(ChatColor.RED + "No glyphs found with the given ID");
                return;
            }

            Collection<Glyph> glyphs;
            try (final var stream = file.open()) {
                glyphs = GlyphReader.mcglyph().read(stream);
            }

            // synchronous update and save
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.registry().setGlyphs(glyphs);
                plugin.registry().save();

                // asynchronous export
                CreativeCentralProvider.get().generate();
            });
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Something went wrong, please" +
                    " contact an administrator to read the console.");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // stack trace in this case isn't so relevant
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
    }

    @Command(names = { "", "list" })
    @SuppressWarnings("deprecation") // Spigot
    public void list(final @NotNull CommandSender sender, final @OptArg("1") @Named("page") int page) {
        final int pageIndex = page - 1;
        // load the configuration for listing emojis
        ConfigurationSection listConfig = plugin.getConfig().getConfigurationSection("messages.list");
        if (listConfig == null) {
            throw new IllegalStateException("No configuration for list subcommand");
        }

        // load the separation config
        ConfigurationSection separationConfig = listConfig.getConfigurationSection("separation");
        Map<Integer, String> separators = new TreeMap<>((a, b) -> b - a);

        if (separationConfig != null) {
            for (String key : separationConfig.getKeys(false)) {
                separators.put(
                        Integer.parseInt(key),
                        ChatColor.translateAlternateColorCodes(
                                '&',
                                separationConfig.getString(key, "Not found")
                        )
                );
            }
        }

        List<Glyph> glyphs = new ArrayList<>(plugin.registry().values());

        boolean showUnavailable = listConfig.getBoolean("show-unavailable", false);
        if (showUnavailable) {
            // sort the emojis alphabetically, put the unavailable ones at the end
            glyphs.sort((a, b) -> {
                boolean canUseA = Permissions.canUse(sender, a);
                boolean canUseB = Permissions.canUse(sender, b);
                if (canUseA && !canUseB) {
                    return -1;
                } else if (!canUseA && canUseB) {
                    return 1;
                } else {
                    return a.name().compareToIgnoreCase(b.name());
                }
            });
        } else {
            // remove emojis that the sender can't use
            glyphs.removeIf(emoji -> !Permissions.canUse(sender, emoji));
            // sort the emojis alphabetically, by name, ignoring case
            glyphs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        }

        int len = glyphs.size();
        int emojisPerPage = listConfig.getInt("max-emojis-per-page", 30);
        int maxPages = (int) Math.ceil(len / (float) emojisPerPage);

        if (pageIndex < 0 || pageIndex >= maxPages) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', listConfig.getString("invalid-page", "Invalid page")));
            return;
        }

        // get the emojis for the current page
        glyphs = glyphs.subList(pageIndex * emojisPerPage, Math.min(len, (pageIndex + 1) * emojisPerPage));

        TextComponent message = new TextComponent("");
        for (int i = 0; i < glyphs.size(); i++) {
            // add separators if needed
            if (i != 0) {
                for (Map.Entry<Integer, String> entry : separators.entrySet()) {
                    if (i % entry.getKey() == 0) {
                        message.addExtra(entry.getValue());
                    }
                    break;
                }
            }

            Glyph glyph = glyphs.get(i);
            boolean available = Permissions.canUse(sender, glyph);
            String basePath = "element." + (available ? "available" : "unavailable");

            TextComponent component = new TextComponent(
                    ChatColor.translateAlternateColorCodes(
                                    '&',
                                    listConfig.getString(basePath + ".content", "Not found")
                            )
                            .replace("<emoji>", glyph.replacement())
                            .replace("<emojiname>", glyph.name())
            );
            component.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes(
                                            '&',
                                            listConfig.getString(basePath + ".hover", "Not found")
                                    )
                                    .replace("<emojiname>", glyph.name())
                                    .replace("<emoji>", glyph.replacement())
                    )
            ));
            if (available) {
                component.setClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        glyph.replacement()
                ));
            }
            message.addExtra(component);
        }

        // send the header message
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', listConfig.getString("header", "Not found"))
                .replace("<page>", String.valueOf(page))
                .replace("<maxpages>", String.valueOf(maxPages))
        );

        // send the content
        sender.spigot().sendMessage(message);

        // send the footer message
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', listConfig.getString("footer", "Not found"))
                .replace("<page>", String.valueOf(page))
                .replace("<maxpages>", String.valueOf(maxPages))
        );
    }
}
