package me.plotcore.listeners;

import me.plotcore.PlotCore;
import me.plotcore.gui.SignGUI;
import me.plotcore.managers.SelectionManager;
import me.plotcore.models.Plot;
import me.plotcore.utils.MessageUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractListener implements Listener {

    private final PlotCore plugin;
    private final SelectionManager sm;

    public PlayerInteractListener(PlotCore plugin) {
        this.plugin = plugin;
        this.sm = plugin.getSelectionManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();

        // ── Wand selection ────────────────────────────────────────────────
        if (sm.isWand(e.getItem())) {
            e.setCancelled(true);
            Block block = e.getClickedBlock();
            if (block == null) return;
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                sm.setCorner1(player.getUniqueId(), block.getLocation());
                MessageUtils.sendRaw(player, "&8[&aPlotCore&8] &aCorner &21 &aset at &e"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                sm.setCorner2(player.getUniqueId(), block.getLocation());
                MessageUtils.sendRaw(player, "&8[&aPlotCore&8] &aCorner &22 &aset at &e"
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
            }
            return;
        }

        // ── Sign click → open SignGUI ─────────────────────────────────────
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            Block block = e.getClickedBlock();
            if (!(block.getState() instanceof Sign)) return;

            Plot plot = plugin.getPlotManager().getPlotBySign(block.getLocation());
            if (plot == null) return;

            e.setCancelled(true);
            new SignGUI(plugin, player, plot).open();
        }
    }
}
