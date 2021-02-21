package me.blackgoldytc.proxyqueue.configuration.section;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;


import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class ConfigOptions implements SettingsHolder {
        @Comment("How many seconds should be in-between each queue movement?")
        public static final Property<Integer> DELAY_LENGTH =
                newProperty("settings.delay-length", 2);

        @Comment("How many seconds after a player disconnects should they be removed from the queue?")
        public static final Property<Integer> DISCONNECT_TIMEOUT =
                newProperty("settings.disconnect-timeout", 180);

        public static final Property<String> QUEUE_SERVER =
                newProperty("settings.server", "backend");

        public static final Property<Integer> NORMAL_QUEUE_SIZE =
                newProperty("settings.normal-queue-size",100);

        public static final Property<Integer> PRIORITY_QUEUE_SIZE =
                newProperty("settings.priority-queue-size",100);

        @Comment({"Players joining queue servers directly will be redirect to this server, or kicked if it isn't available"})
        public static final Property<String> WAITING_SERVER =
                newProperty("settings.waiting-server", "limbo");

        @Comment({"What would you like the priority permission node to be?"})
        public static final Property<String> PRIORITY_PERMISSION =
                newProperty("settings.priority-permission", "proxyqueues.priority");

        @Comment({"What would you like the staff permission node to be?"})
        public static final Property<String> STAFF_PERMISSION =
                newProperty("settings.staff-permission", "proxyqueues.staff");

        @Comment("How would you like the design for the ActionBar to look like?")
        public static final Property<String> BOSSBAR_DESIGN =
                newProperty("notify.bossbar.design", "Current position: {pos}");

        @Override
        public void registerComments(CommentsConfiguration configuration) {
            String[] pluginHeader = {
                    "ProxyQueues"
            };
            configuration.setComment("settings", pluginHeader);
        }
}
