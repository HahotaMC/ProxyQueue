package me.blackgoldytc.proxyqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Optional;
import java.util.Stack;

@Plugin(
        id = "proxy-queue",
        name = "Proxy Queue",
        version = "1.0",
        description = "A proxy Queue Plugin",
        authors = {"BlackGoldYTC"}
)
public class ProxyQueue {

    private final ProxyServer server;
    private final Logger logger;
    private final HashMap<Player,Priority> queue= new HashMap<Player,Priority>();

    @Inject
    public ProxyQueue(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

    }
        @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    @Subscribe
    public  void onPlayerJoin(PlayerChooseInitialServerEvent e){
       try {
           e.setInitialServer(server.getServer("backend").get());
       }catch (Exception es){
           es.printStackTrace();
           queue.put(e.getPlayer(),Ultis.getPriority(e.getPlayer()));
       }
    }
}
