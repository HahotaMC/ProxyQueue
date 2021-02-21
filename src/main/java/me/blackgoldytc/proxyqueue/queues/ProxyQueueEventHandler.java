package me.blackgoldytc.proxyqueue.queues;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.blackgoldytc.proxyqueue.ProxyQueues;
import me.blackgoldytc.proxyqueue.QueueType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyQueueEventHandler {
    private final ProxyQueues proxyQueues;
    private final ProxyQueue queue;

    private final Set<UUID> fatalKicks = ConcurrentHashMap.newKeySet();

    public ProxyQueueEventHandler(ProxyQueues proxyQueues, ProxyQueue queue) {
        this.proxyQueues = proxyQueues;
        this.queue = queue;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreConnect(ServerPreConnectEvent event) {
        if(!event.getResult().isAllowed()) {
            return;
        }

        RegisteredServer server = event.getOriginalServer();
        Player player = event.getPlayer();
        RegisteredServer redirected = event.getResult().getServer().orElse(null);

        if(redirected != null && !redirected.equals(server)) {
            server = redirected;
        }

        // Check if the server has a queue
        if(!server.equals(queue.getServer()) || !this.queue.isActive()) {
            return;
        }

        // Get the queue
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player, true);

        if(queuePlayer.isEmpty()) {
            if(event.getPlayer().getCurrentServer().isEmpty()) {
                Optional<RegisteredServer> waitingServer = proxyQueues.getWaitingServer();

                if(waitingServer.isPresent()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(waitingServer.get()));
                } else {
                    player.disconnect(Component.text(
                            "This server has queueing enabled and can't be connected to directly. Please connect via minecraft.rtgame.co.uk")
                            .color(NamedTextColor.RED));

                    return;
                }
            } else {
                // Cancel the event so they don't go right away
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }

            // Add the player to the queue
            queue.addPlayer(player);
        } else if(!queuePlayer.get().isConnecting()) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe(order = PostOrder.LATE)
    public void onConnected(ServerConnectedEvent event) {
        fatalKicks.remove(event.getPlayer().getUniqueId());

        if(event.getServer().equals(queue.getServer())) {
            queue.removePlayer(event.getPlayer(), true);
        }

        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        previousServer.ifPresent(server -> {
            if(server.equals(this.queue.getServer())) {
                queue.clearConnectedState(event.getPlayer());
            }
        });
    }
    @Subscribe(order = PostOrder.EARLY)
    public void onLeave(DisconnectEvent event) {
        Player player = event.getPlayer();
        Optional<QueuePlayer> queuePlayer = queue.getQueuePlayer(player, false);
        queue.clearConnectedState(player);

        proxyQueues.getLogger().info("Handling proxy disconnect for " + player.getUsername());

        if (queuePlayer.isPresent()) {
            proxyQueues.getLogger().info("Player in queue, setting last seen");
            queuePlayer.get().setLastSeen(Instant.now());
            return;
        }

        player.getCurrentServer().ifPresent(server -> {
            if(server.getServer().equals(queue.getServer())) {
                if(fatalKicks.contains(player.getUniqueId())) {
                    proxyQueues.getLogger().info(
                            "Player disconnecting due to fatal kick. Not re-queueing.");

                    fatalKicks.remove(player.getUniqueId());

                    return;
                }

                boolean staff = player.hasPermission("proxyQueus.staff");
                proxyQueues.getLogger().info(
                        "Player not in queue, adding to " + (staff ? "staff" : "priority") + " queue");
                queue.addPlayer(player, staff ? QueueType.STAFF : QueueType.PRIORITY);
            }
        });
    }

    @Subscribe(order = PostOrder.LAST)
    public void onKick(KickedFromServerEvent event) {
        if(!event.getServer().equals(queue.getServer()) || event.kickedDuringServerConnect()) {
            return;
        }

        proxyQueues.getLogger()
                .info("Player " + event.getPlayer().getUsername() + " kicked from " +
                        event.getServer().getServerInfo().getName() + ". Reason: " + event.getServerKickReason());

        Component reason = event.getServerKickReason().orElse(Component.empty());
        String reasonPlain = PlainComponentSerializer.plain().serialize(reason);

            boolean staff = event.getPlayer()
                    .hasPermission("proxyQueus.staff");

            queue.addPlayer(event.getPlayer(), staff ? QueueType.STAFF : QueueType.PRIORITY);

    }
}
