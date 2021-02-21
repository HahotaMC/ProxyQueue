package me.blackgoldytc.proxyqueue.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.blackgoldytc.proxyqueue.ProxyQueues;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class QueueHandler {

    private final ConcurrentHashMap<RegisteredServer, ProxyQueue> queues;
    private final SettingsManager settingsManager;
    private final ProxyQueues proxyQueues;

    public QueueHandler(SettingsManager settingsManager, ProxyQueues proxyQueues) {
        this.settingsManager = settingsManager;
        this.proxyQueues = proxyQueues;
        this.queues = new ConcurrentHashMap<>();

        updateQueues();
    }

    public void updateQueues() {
        ArrayList<RegisteredServer> queuedServers = new ArrayList<>();
        try{
            String serverName = settingsManager.getProperty(ConfigOptions.QUEUE_SERVER);

            int maxPriority = settingsManager.getProperty(ConfigOptions.PRIORITY_QUEUE_SIZE);

            RegisteredServer server = proxyQueues.getProxyServer().getServer(serverName).get();

            ProxyQueue queue = createQueue(server, maxPriority);
            queue.setDelayLength(settingsManager.getProperty(ConfigOptions.DELAY_LENGTH));

            queuedServers.add(server);
        } catch (Exception ex) {
                proxyQueues.getLogger().warn("It seems like one of your servers was configured invalidly in the config.");
                ex.printStackTrace();
        }
    }

    public ProxyQueue createQueue(@NotNull RegisteredServer server,int maxPriority) {
        return queues.compute(server, (s, queue) -> {
            if(queue == null) {
                proxyQueues.getLogger().info("Creating queue for " + server.getServerInfo().getName());
                return new ProxyQueue(proxyQueues, s, maxPriority);
            } else {
                proxyQueues.getLogger().info("Updating queue for " + server.getServerInfo().getName());
                queue.setPriorityMaxSlots(maxPriority);

                return queue;
            }
        });
    }
}
