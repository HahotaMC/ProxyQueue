package me.blackgoldytc.proxyqueue.queues;

import ch.jalu.configme.SettingsManager;
import me.blackgoldytc.proxyqueue.ProxyQueues;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;
import net.kyori.adventure.text.Component;

public class ProxyQueueNotifier {
    private final SettingsManager settingsManager;
    private final ProxyQueue queue;

    public ProxyQueueNotifier(ProxyQueues proxyQueues, ProxyQueue queue) {
        this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
        this.queue = queue;

    }

    public void notifyPlayer(QueuePlayer player) {
        updateBossBar(player);
    }

    private void updateBossBar(QueuePlayer player) {
        int position = player.getPosition();
        String message =settingsManager.getProperty(ConfigOptions.BOSSBAR_DESIGN);
        message = message.replace("{pos}", String.valueOf(position));

        player.showBossBar();
        player.getBossBar().name(Component.text(message));
    }
}
