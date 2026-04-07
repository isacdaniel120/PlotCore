package me.plotcore.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.plotcore.PlotCore;
import me.plotcore.models.Plot;
import me.plotcore.utils.MessageUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class TrustChatListener implements Listener {

    private final PlotCore plugin;
    private final Player player;
    private final Plot plot;

    public TrustChatListener(PlotCore plugin, Player player, Plot plot) {
        this.plugin = plugin;
        this.player = player;
        this.plot   = plot;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (!e.getPlayer().equals(player)) return;
        e.setCancelled(true);
        HandlerList.unregisterAll(this);

        String input = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (input.equalsIgnoreCase("cancel")) {
            MessageUtils.sendRaw(player, "&8[&aPlotCore&8] &7Cancelled.");
            // Re-open trust GUI on main thread
            Bukkit.getScheduler().runTask(plugin, () -> new TrustGUI(plugin, player, plot).open());
            return;
        }

        @SuppressWarnings("deprecation")
        Player target = Bukkit.getPlayerExact(input);
        if (target == null) {
            MessageUtils.sendRaw(player, "&8[&aPlotCore&8] &cPlayer &e" + input + " &cnot found or offline.");
            Bukkit.getScheduler().runTask(plugin, () -> new TrustGUI(plugin, player, plot).open());
            return;
        }

        int max = plugin.getConfig().getInt("settings.max-trusted", 4);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plot.isTrusted(target.getUniqueId())) {
                MessageUtils.send(player, "trust-already", "{player}", target.getName());
            } else if (plot.getTrusted().size() >= max) {
                MessageUtils.send(player, "trust-max", "{max}", String.valueOf(max));
            } else {
                plot.addTrusted(target.getUniqueId());
                plugin.getPlotManager().saveAll();
                MessageUtils.send(player, "trust-added", "{player}", target.getName(), "{name}", plot.getName());
            }
            new TrustGUI(plugin, player, plot).open();
        });
    }
}
