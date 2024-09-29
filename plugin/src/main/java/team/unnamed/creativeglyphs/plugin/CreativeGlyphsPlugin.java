package team.unnamed.creativeglyphs.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import team.unnamed.creative.central.CreativeCentralProvider;
import team.unnamed.creative.central.event.pack.ResourcePackGenerateEvent;
import team.unnamed.creativeglyphs.plugin.command.CommandService;
import team.unnamed.creativeglyphs.plugin.integration.carbon.CarbonChatIntegration;
import team.unnamed.creativeglyphs.plugin.integration.essentialsdiscord.EssentialsDiscordIntegration;
import team.unnamed.creativeglyphs.plugin.listener.misc.AnvilEditListener;
import team.unnamed.creativeglyphs.plugin.listener.misc.CommandPreprocessListener;
import team.unnamed.creativeglyphs.plugin.listener.bus.EventListener;
import team.unnamed.creativeglyphs.plugin.integration.PluginIntegration;
import team.unnamed.creativeglyphs.plugin.integration.IntegrationManager;
import team.unnamed.creativeglyphs.plugin.integration.discordsrv.DiscordSRVIntegration;
import team.unnamed.creativeglyphs.plugin.integration.ezchat.EzChatIntegration;
import team.unnamed.creativeglyphs.plugin.integration.miniplaceholders.MiniPlaceholdersIntegration;
import team.unnamed.creativeglyphs.plugin.integration.papi.PlaceholderAPIIntegration;
import team.unnamed.creativeglyphs.plugin.integration.townychat.TownyChatIntegration;
import team.unnamed.creativeglyphs.plugin.listener.chat.ChatCompletionsListener;
import team.unnamed.creativeglyphs.plugin.listener.bus.EventBus;
import team.unnamed.creativeglyphs.plugin.listener.ListenerFactory;
import team.unnamed.creativeglyphs.plugin.util.GitHubUpdateChecker;
import team.unnamed.creativeglyphs.resourcepack.ResourcePackGlyphWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public final class CreativeGlyphsPlugin extends JavaPlugin {
    private PluginGlyphMap registry;

    @Override
    public void onEnable() {
        final Path dataFolder = getDataFolder().toPath();

        //#region Backwards compatibility (creative-glyphs was called unemojis)
        // unemojis should be removed
        if (Bukkit.getPluginManager().isPluginEnabled("unemojis")) {
            getLogger().severe(
                    "Can't enable creative-glyphs since unemojis is enabled! Please remove " +
                            "unemojis JAR file only (NOT THE unemojis FOLDER, IT WILL BE AUTOMATICALLY" +
                            " RENAMED). Note that creative-glyphs is the new, improved version of unemojis."
            );
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Rename unemojis data folder to creative-glyphs if it exists
        final Path pluginsFolder = Bukkit.getPluginsFolder().toPath();
        final Path unemojisDataFolder = pluginsFolder.resolve("unemojis");
        if (Files.isDirectory(unemojisDataFolder)) {
            try {
                Files.move(unemojisDataFolder, dataFolder);
            } catch (final IOException e) {
                throw new IllegalStateException("(Backwards compatibility) Couldn't" +
                        " rename 'unemojis' folder to '" + dataFolder.getFileName() + "'", e);
            }
        }
        //#endregion

        saveDefaultConfig();

        try {
            this.registry = PluginGlyphMap.create(this);
        } catch (IOException e) {
            getLogger().severe("Cannot create emoji store...");
            setEnabled(false);
            return;
        }

        this.registry.load();

        // export
        CreativeCentralProvider.get().eventBus().listen(this, ResourcePackGenerateEvent.class, new ResourcePackGlyphWriter(registry));

        EventBus eventBus = EventBus.create(this);

        // todo:!
        new CommandService(this).start();

        Set<PluginIntegration> hooks = IntegrationManager.integrationManager(this)
                .register(new CarbonChatIntegration(this))
                .register(new EzChatIntegration(this, registry))
                .register(new TownyChatIntegration(this, registry))
                .register(new PlaceholderAPIIntegration(this, registry))
                .register(new DiscordSRVIntegration(registry))
                .register(new MiniPlaceholdersIntegration(this))
                .register(new EssentialsDiscordIntegration(this, registry))
                .check();

        if (hooks.stream().noneMatch(hook -> hook instanceof PluginIntegration.Chat)) {
            // if no chat plugin hooks, let's register our own listener
            EventPriority priority = EventPriority.valueOf(getConfig().getString(
                    "compat.listener-priority",
                    "HIGHEST"
            ).toUpperCase(Locale.ROOT));

            EventListener<?> chatListener = ListenerFactory.create(
                    this,
                    registry,
                    getConfig().getBoolean("compat.use-paper-listener")
            );

            eventBus.register(chatListener, priority);
        }

        try {
            // check if methods required to make completions work exist
            // (they may not exist in Spigot)
            Player.class.getDeclaredMethod("addAdditionalChatCompletions", Collection.class);

            // register emoji completions listener
            listen(new ChatCompletionsListener(registry));
        } catch (NoSuchMethodException ignored) {
        }

        listen(new AnvilEditListener(registry));
        listen(new CommandPreprocessListener(registry));

        // Metrics
        // new Metrics(this, 17168);

        // GitHub Update Checker
        GitHubUpdateChecker.checkAsync(this);
    }

    private void listen(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public PluginGlyphMap registry() {
        return registry;
    }
}
