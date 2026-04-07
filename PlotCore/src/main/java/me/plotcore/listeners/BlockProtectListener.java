package me.plotcore.listeners;

import me.plotcore.PlotCore;
import me.plotcore.managers.PlotManager;
import me.plotcore.models.Plot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

public class BlockProtectListener implements Listener {

    private final PlotCore plugin;
    private final PlotManager pm;

    // Everything a non-owner cannot open/use
    private static final Set<Material> CONTAINERS = Set.of(
        Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
        Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.BREWING_STAND, Material.ANVIL, Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL, Material.ENCHANTING_TABLE,
        Material.CRAFTING_TABLE, Material.LECTERN, Material.CHISELED_BOOKSHELF,
        Material.ENDER_CHEST, Material.SHULKER_BOX
    );

    public BlockProtectListener(PlotCore plugin) {
        this.plugin = plugin;
        this.pm = plugin.getPlotManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!cfg("protection.prevent-break")) return;
        if (bypass(e.getPlayer())) return;
        Plot plot = pm.getPlotAt(e.getBlock().getLocation());
        if (plot == null || !shouldProtect(plot)) return;
        if (!plot.canAccess(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!cfg("protection.prevent-build")) return;
        if (bypass(e.getPlayer())) return;
        Plot plot = pm.getPlotAt(e.getBlock().getLocation());
        if (plot == null || !shouldProtect(plot)) return;
        if (!plot.canAccess(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (bypass(e.getPlayer())) return;

        Plot plot = pm.getPlotAt(e.getClickedBlock().getLocation());
        if (plot == null || !shouldProtect(plot)) return;
        if (plot.canAccess(e.getPlayer().getUniqueId())) return;

        Material type = e.getClickedBlock().getType();

        if (cfg("protection.prevent-containers") && CONTAINERS.contains(type)) {
            e.setCancelled(true); return;
        }
        if (cfg("protection.prevent-interact")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (bypass(p)) return;
        Plot plot = pm.getPlotAt(e.getEntity().getLocation());
        if (plot == null || !shouldProtect(plot)) return;
        if (!plot.canAccess(p.getUniqueId())) e.setCancelled(true);
    }

    private boolean shouldProtect(Plot plot) {
        if (plot.isRented()) return true;
        return plugin.getConfig().getBoolean("protection.protect-unrented", true);
    }

    private boolean bypass(Player p) {
        return p.hasPermission("jplots.admin") || pm.isBypassing(p.getUniqueId());
    }

    private boolean cfg(String key) {
        return plugin.getConfig().getBoolean(key, true);
    }
}
