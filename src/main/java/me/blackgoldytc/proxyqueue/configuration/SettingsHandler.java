package me.blackgoldytc.proxyqueue.configuration;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.configme.migration.PlainMigrationService;
import me.blackgoldytc.proxyqueue.ProxyQueues;

import java.io.File;

public class SettingsHandler
{
    private final SettingsManager settingsManager;

    public SettingsHandler(ProxyQueues proxyQueues) {

        settingsManager = SettingsManagerBuilder
                .withYamlFile(new File(proxyQueues.getDataFolder(), "config.yml"))
                .migrationService(new PlainMigrationService())
                .configurationData(ConfigBuilder.buildConfig())
                .create();
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
