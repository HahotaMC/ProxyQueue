package me.blackgoldytc.proxyqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
        id = "proxy-queue",
        name = "Proxy Queue",
        version = "1.0",
        description = "A proxy Queue Plugin",
        authors = {"BlackGoldYTC"}
)
public class ProxyQueue {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
