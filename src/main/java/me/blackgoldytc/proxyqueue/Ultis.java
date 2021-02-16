package me.blackgoldytc.proxyqueue;

import com.velocitypowered.api.proxy.Player;

public class Ultis {

    public static Priority getPriority(Player p){
        if(p.hasPermission("ProxyQueues.Admin")){
            return Priority.AdminPriority;
        }else if(p.hasPermission("ProxyQueues.High")){
            return  Priority.HighPriority;
        }
        return Priority.NormalPriority;
    }
}
