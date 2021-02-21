package me.blackgoldytc.proxyqueue.task;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.blackgoldytc.proxyqueue.ProxyQueues;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;
import me.blackgoldytc.proxyqueue.queues.ProxyQueue;
import me.blackgoldytc.proxyqueue.queues.QueuePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueTask implements Runnable{
    private final ProxyQueue queue;
    private final RegisteredServer server;
    private final ProxyQueues proxyQueues;
    private final RegisteredServer waitingServer;

    private QueuePlayer targetPlayer = null;

    public QueueTask(ProxyQueue queue, RegisteredServer server, ProxyQueues proxyQueues) {
        this.queue = queue;
        this.server = server;
        this.proxyQueues = proxyQueues;

        waitingServer = proxyQueues.getWaitingServer().orElse(null);
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<QueuePlayer> normalQueue = queue.getQueue();
        ConcurrentLinkedQueue<QueuePlayer> priorityQueue = queue.getPriorityQueue();
        ConcurrentLinkedQueue<QueuePlayer> staffQueue = queue.getStaffQueue();

        if(targetPlayer != null && (!targetPlayer.isConnecting() || !targetPlayer.getPlayer().isActive())) {
            targetPlayer.setConnecting(false);
            targetPlayer = null;
        }

        if(targetPlayer != null && targetPlayer.getLastConnectionAttempt().isBefore(Instant.now().minusSeconds(10))) {
            proxyQueues.getLogger().debug("Target player timed out");
            targetPlayer.setConnecting(false);
            targetPlayer = null;
        }

        handleQueue(staffQueue);
        handleQueue(priorityQueue);
        handleQueue(normalQueue);

        // Nothing to do if no player to queue, or connection attempt already underway
        if(targetPlayer == null || targetPlayer.isConnecting()) {
            return;
        }

        // Check if the max amount of players on the server are the max slots
        if (queue.isServerFull(targetPlayer.getQueueType())) {
            return;
        }

        connectPlayer();
    }

    private void connectPlayer() {
        proxyQueues.getLogger().info("Attempting to connect player " + targetPlayer.toString());

        targetPlayer.getPlayer().createConnectionRequest(server).connect().thenAcceptAsync(result -> {
            targetPlayer.setConnecting(false);

            if(result.isSuccessful()) {
                return;
            }

            proxyQueues.getLogger()
                    .info("Player " +
                            targetPlayer.getPlayer().getUsername() + " failed to join "
                            + queue.getServer().getServerInfo().getName()
                            + ". Reason: \""
                            + PlainComponentSerializer.plain()
                            .serialize(result.getReasonComponent().orElse(Component.text("(None)"))) + "\"");

            Component reason = result.getReasonComponent().orElse(Component.empty());
            String reasonPlain = PlainComponentSerializer.plain().serialize(reason);
            ServerConnection currentServer = targetPlayer.getPlayer().getCurrentServer().orElse(null);
        });

        targetPlayer.setConnecting(true);
    }

    private void handleQueue(ConcurrentLinkedQueue<QueuePlayer> q) {
        int position = 1;
        int disconnectTimeout = proxyQueues.getSettingsHandler()
                .getSettingsManager().getProperty(ConfigOptions.DISCONNECT_TIMEOUT);

        for (QueuePlayer player : q) {
            boolean online = player.getPlayer().isActive();

            if (online || player.getLastSeen() == null) {
                player.setLastSeen(Instant.now());
            }

            if (!online && player.getLastSeen().isBefore(Instant.now().minusSeconds(disconnectTimeout))) {
                proxyQueues.getLogger()
                        .info("Removing timed out player " + player.getPlayer().getUsername()
                                + " from " + player.getQueueType() + "/" + position);

                queue.removePlayer(player, false);
                continue;
            }

            if (targetPlayer == null && online) {
                targetPlayer = player;
            }

            player.setPosition(position);
            queue.getNotifier().notifyPlayer(player);
            position++;
        }
    }
}
