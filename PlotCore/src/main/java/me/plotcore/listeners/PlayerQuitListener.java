package me.plotcore.listeners;

import me.plotcore.PlotCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final PlotCore plugin;

    public PlayerQuitListener(PlotCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getSelectionManager().clear(e.getPlayer().getUniqueId());
        // Bypass is intentionally cleared on logout for security
    }
}
