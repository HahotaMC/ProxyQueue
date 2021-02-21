package me.blackgoldytc.proxyqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.blackgoldytc.proxyqueue.configuration.SettingsHandler;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "proxy-queue",
        name = "Proxy Queue",
        version = "1.0",
        description = "A proxy Queue Plugin",
        authors = {"BlackGoldYTC"}
)
public class ProxyQueues {
    private static ProxyQueues instance;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final SettingsHandler settingsHandler;

    @Inject
    @DataDirectory
    private Path dataFolder;

    @Inject
    public ProxyQueues(ProxyServer proxy, Logger logger) {
        this.proxyServer = proxy;
        this.logger = logger;
        this.settingsHandler = new SettingsHandler(this);
        instance = this;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        //startQueues();
        Optional<PluginContainer> prometheusExporter = proxyServer.getPluginManager().getPlugin("velocity-prometheus-exporter");

    }
    public SettingsHandler getSettingsHandler() {
        return this.settingsHandler;
    }


    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public File getDataFolder() {
        return dataFolder.toFile();
    }

    public Optional<RegisteredServer> getWaitingServer() {
        String waitingServerName = getSettingsHandler().getSettingsManager().getProperty(ConfigOptions.WAITING_SERVER);
        return getProxyServer().getServer(waitingServerName);
    }

    public static ProxyQueues getInstance() {
        return instance;
    }

}
