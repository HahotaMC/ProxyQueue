package me.blackgoldytc.proxyqueue.queues;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.blackgoldytc.proxyqueue.ProxyQueues;
import me.blackgoldytc.proxyqueue.QueueType;
import me.blackgoldytc.proxyqueue.configuration.SettingsHandler;
import me.blackgoldytc.proxyqueue.configuration.section.ConfigOptions;
import me.blackgoldytc.proxyqueue.events.PlayerQueueEvent;
import me.blackgoldytc.proxyqueue.task.QueueTask;
import net.kyori.adventure.text.Component;

import java.awt.*;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyQueue {
        private final ProxyQueues proxyQueues;

        private final ConcurrentLinkedQueue<QueuePlayer> queue = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<QueuePlayer> priorityQueue = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<QueuePlayer> staffQueue = new ConcurrentLinkedQueue<>();

        private final ConcurrentHashMap<UUID, QueuePlayer> queuePlayers = new ConcurrentHashMap<>();

        //Cache of connected priority/staff players, to ease calculation of queue player thresholds
        private final Set<UUID> connectedPriority = ConcurrentHashMap.newKeySet();
        private final Set<UUID> connectedStaff = ConcurrentHashMap.newKeySet();

        private final RegisteredServer server;
        private ScheduledTask scheduledTask;
        private int delayLength;
        private int playersRequired;
        private SettingsManager settingsManager;

        private int maxSlots;
        private int priorityMaxSlots;
        private int staffMaxSlots;

        private final ProxyQueueNotifier notifier;
        private final ProxyQueueEventHandler eventHandler;
        private final QueueTask moveTask;

        public ProxyQueue(ProxyQueues proxyQueues, RegisteredServer server, int playersRequired, int maxSlots, int priorityMaxSlots, int staffMaxSlots) {
            this.proxyQueues = proxyQueues;
            this.server = server;
            this.playersRequired = Math.max(playersRequired, 0);

            this.maxSlots = Math.max(maxSlots, playersRequired);
            this.priorityMaxSlots = Math.max(priorityMaxSlots, maxSlots);
            this.staffMaxSlots = Math.max(staffMaxSlots, priorityMaxSlots);

            this.eventHandler = new ProxyQueueEventHandler(proxyQueues, this);
            proxyQueues.getProxyServer().getEventManager().register(proxyQueues, eventHandler);
            this.settingsManager = proxyQueues.getSettingsHandler().getSettingsManager();
            this.notifier = new ProxyQueueNotifier(proxyQueues, this);

            this.moveTask = new QueueTask(this, server, proxyQueues);
            this.scheduledTask = proxyQueues.getProxyServer().getScheduler().buildTask(proxyQueues, moveTask)
                    .repeat(delayLength, TimeUnit.SECONDS).schedule();
        }

        public void addPlayer(Player player) {
            QueueType queueType = QueueType.NORMAL;

            if (player.hasPermission(settingsManager.getProperty(ConfigOptions.PRIORITY_PERMISSION))) {
                queueType = QueueType.PRIORITY;
            } else if (player.hasPermission(settingsManager.getProperty(ConfigOptions.STAFF_PERMISSION))) {
                queueType = QueueType.STAFF;
            }

            addPlayer(player, queueType);

        }

        public void addPlayer(Player player, QueueType queueType) {
            AtomicBoolean added = new AtomicBoolean(false);

            QueuePlayer result = queuePlayers.compute(player.getUniqueId(), (uuid, queuePlayer) -> {
                added.set(false);

                if (queuePlayer != null) {
                    if (queuePlayer.getPlayer().equals(player)) { //Player is already in queue
                        return queuePlayer;
                    } else { //Player was previous in queue before disconnecting, update and reuse object
                        if (shouldAddPlayer(player)) {
                            queuePlayer.setPlayer(player);
                            queuePlayer.setLastSeen(Instant.now());
                            return queuePlayer;
                        } else {
                            return null;
                        }
                    }
                } else { //New player
                    added.set(shouldAddPlayer(player));

                    return added.get() ? new QueuePlayer(player, queueType) : null;
                }
            });

            if (result != null) {
                //Only add to queue if they aren't already there
                if (added.get()) {
                    proxyQueues.getLogger().info("Added " + player.getUsername() + " added already in queue. Restoring position");
                    switch (result.getQueueType()) {
                        case STAFF:
                            staffQueue.add(result);
                            break;

                        case PRIORITY:
                            priorityQueue.add(result);
                            break;

                        case NORMAL:
                        default:
                            queue.add(result);
                            break;
                    }
                } else {
                    proxyQueues.getLogger().info("Restoring queue position of " + player.getUsername());

                   /* if (result.getQueueType() == QueueType.PRIORITY) {
                        proxyQueues.sendMessage(result.getPlayer(), MessageType.INFO, Messages.RECONNECT__RESTORE_PRIORITY);
                    } else {
                        proxyQueues.sendMessage(result.getPlayer(), MessageType.INFO, Messages.RECONNECT__RESTORE_POSITION);
                    }

                    */
                }
            }
        }

        private boolean shouldAddPlayer(Player player) {
            PlayerQueueEvent event = new PlayerQueueEvent(player, server);
            proxyQueues.getProxyServer().getEventManager().fire(event).join();

            if (event.isCancelled()) {
                String reason = event.getReason() != null ? event.getReason() : "An unexpected error occurred. Please try again later";
                ServerConnection currentServer = player.getCurrentServer().orElse(null);
                RegisteredServer waitingServer = proxyQueues.getWaitingServer().orElse(null);

                proxyQueues.getLogger().info(player.getUsername() + "'s PlayerQueueEvent cancelled");

            }

            return !event.isCancelled();

        }

        public void removePlayer(QueuePlayer player, boolean connected) {
            player.hideBossBar();
            player.setConnecting(false);
            boolean removed;

            if (!connected) {
                clearConnectedState(player.getPlayer());
            }

            switch (player.getQueueType()) {
                case STAFF:
                    removed = staffQueue.remove(player);

                    //Update connected players cache
                    if (connected) {
                        connectedStaff.add(player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                    }

                    break;

                case PRIORITY:
                    removed = priorityQueue.remove(player);

                    //Update connected players cache
                    if (connected) {
                        connectedPriority.add(player.getPlayer().getUniqueId()); //Is connected to queued server, add to connected
                    }

                    break;

                case NORMAL:
                default:
                    removed = queue.remove(player);
                    break;
            }

            proxyQueues.getLogger().info("removePlayer: type = " + player.getQueueType() + ", removed? " + removed);
            queuePlayers.remove(player.getPlayer().getUniqueId());
        }
        public void removePlayer(Player player, boolean connected) {
            Optional<QueuePlayer> queuePlayer = getQueuePlayer(player, false);

            if (queuePlayer.isPresent()) {
                removePlayer(queuePlayer.get(), connected);
            } else {
                proxyQueues.getLogger().info("Not in queue, removing cached entries");
                clearConnectedState(player);
            }
        }

        public void removePlayer(UUID uuid, boolean connected) {
            Optional<QueuePlayer> queuePlayer = getQueuePlayer(uuid);

            if (queuePlayer.isPresent()) {
                removePlayer(queuePlayer.get(), connected);
            } else {
                proxyQueues.getLogger().info("Not in queue, removing cached entries");
                clearConnectedState(uuid);
            }
        }



        public boolean isPlayerQueued(Player player) {
            return getQueuePlayer(player, false).isPresent();
        }

        public boolean isPlayerQueued(UUID uuid) {
            return getQueuePlayer(uuid).isPresent();
        }

        public Optional<QueuePlayer> getQueuePlayer(Player player, boolean strict) {
            QueuePlayer queuePlayer = queuePlayers.get(player.getUniqueId());

            if (queuePlayer == null) {
                queuePlayer = queuePlayers.computeIfAbsent(player.getUniqueId(), key -> null);
            }

            if (strict && queuePlayer != null && !queuePlayer.getPlayer().equals(player)) {
                queuePlayer = null;
            }

            return Optional.ofNullable(queuePlayer);
        }

        public Optional<QueuePlayer> getQueuePlayer(UUID uuid) {
            QueuePlayer queuePlayer = queuePlayers.get(uuid);

            if (queuePlayer == null) {
                queuePlayer = queuePlayers.computeIfAbsent(uuid, key -> null);
            }

            return Optional.ofNullable(queuePlayer);
        }

        public boolean isActive() {
            return server.getPlayersConnected().size() >= playersRequired;
        }

        public boolean isServerFull(QueueType queueType) {
            int modSlots = getMaxSlots(QueueType.STAFF) - getMaxSlots(QueueType.PRIORITY);
            int prioritySlots = getMaxSlots(QueueType.PRIORITY) - getMaxSlots(QueueType.NORMAL);

            int usedModSlots;
            int usedPrioritySlots;
            int totalPlayers = server.getPlayersConnected().size();

            switch (queueType) {
                //Staff, check total count is below staff limit
                case STAFF:
                    break;

                //Priority, check total count ignoring filled mod slots is below priority limit
                case PRIORITY:
                    usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave
                    totalPlayers -= usedModSlots;

                    break;

                //Normal, check total count ignoring filled mod and priority slots is below normal limit
                case NORMAL:
                default:
                    usedModSlots = Math.min(connectedStaff.size(), modSlots); //Ignore staff beyond assigned slots, prevents mods "stealing" normal slots if normal players leave
                    usedPrioritySlots = Math.min(connectedPriority.size() + (connectedStaff.size() - usedModSlots), prioritySlots); //Count mods not counted above as filled priority slots, prevents mods "stealing" normal slots if normal players leave
                    totalPlayers -= (usedModSlots + usedPrioritySlots);

                    break;
            }

            return totalPlayers >= getMaxSlots(queueType);
        }

        public int getMaxSlots(QueueType queueType) {
            switch (queueType) {
                case STAFF:
                    return staffMaxSlots;
                case PRIORITY:
                    return priorityMaxSlots;
                case NORMAL:
                default:
                    return maxSlots;
            }
        }

        public int getQueueSize(QueueType queueType) {
            switch (queueType) {
                case STAFF:
                    return staffQueue.size();
                case PRIORITY:
                    return priorityQueue.size();
                case NORMAL:
                default:
                    return queue.size();
            }
        }

        public int getConnectedCount() {
            return server.getPlayersConnected().size();
        }

        public int getConnectedCount(QueueType queueType) {
            switch (queueType) {
                case STAFF:
                    return connectedStaff.size();
                case PRIORITY:
                    return connectedPriority.size();
                case NORMAL:
                default:
                    return server.getPlayersConnected().size() - connectedStaff.size() - connectedPriority.size();
            }
        }

        public QueuePlayer[] getTopPlayers(QueueType queueType, int count) {
            QueuePlayer[] players = new QueuePlayer[count];
            ConcurrentLinkedQueue<QueuePlayer> queue;

            switch (queueType) {
                case STAFF:
                    queue = staffQueue;
                    break;
                case PRIORITY:
                    queue = priorityQueue;
                    break;
                case NORMAL:
                default:
                    queue = this.queue;
                    break;
            }

            int index = 0;

            for (QueuePlayer player : queue) {
                players[index] = player;

                if (++index > 2) {
                    break;
                }
            }

            return players;
        }

        public ConcurrentLinkedQueue<QueuePlayer> getQueue() {
            return queue;
        }

        public ConcurrentLinkedQueue<QueuePlayer> getPriorityQueue() {
            return priorityQueue;
        }

        public ConcurrentLinkedQueue<QueuePlayer> getStaffQueue() {
            return staffQueue;
        }

        public RegisteredServer getServer() {
            return this.server;
        }

        public int getPlayersRequired() {
            return this.playersRequired;
        }

        public void setDelayLength(int delayLength) {
            if (delayLength != this.delayLength) {
                scheduledTask.cancel();
                scheduledTask = proxyQueues.getProxyServer().getScheduler().buildTask(proxyQueues, moveTask)
                        .repeat(delayLength, TimeUnit.SECONDS).schedule();
            }

            this.delayLength = delayLength;
        }

        public void setPlayersRequired(int playersRequired) {
            this.playersRequired = playersRequired;
        }

        public void setMaxSlots(int maxSlots) {
            this.maxSlots = maxSlots;
        }

        public void setPriorityMaxSlots(int priorityMaxSlots) {
            this.priorityMaxSlots = priorityMaxSlots;
        }

        public void setStaffMaxSlots(int staffMaxSlots) {
            this.staffMaxSlots = staffMaxSlots;
        }

        public String toString() {
            return "ProxyQueue(queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.delayLength + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots(QueueType.STAFF) + ", notifyMethod=bossbar)";
        }

        void clearConnectedState(Player player) {
            connectedStaff.remove(player.getUniqueId());
            connectedPriority.remove(player.getUniqueId());
        }

        void clearConnectedState(UUID uuid) {
            connectedStaff.remove(uuid);
            connectedPriority.remove(uuid);
        }
        public ProxyQueueNotifier getNotifier() {
            return notifier;
        }
    }

