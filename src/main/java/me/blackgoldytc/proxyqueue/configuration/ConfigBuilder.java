package me.blackgoldytc.proxyqueue.configuration;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;

public class ConfigBuilder {
    private ConfigBuilder() {

    }

    public static ConfigurationData buildConfig() {
        return ConfigurationDataBuilder
                .createConfiguration(ConfigOptions.class);
    }

}
