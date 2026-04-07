package me.plotcore.gui;

import me.plotcore.PlotCore;
import me.plotcore.models.Plot;
import me.plotcore.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlotMenuGUI implements Listener {

    private final PlotCore plugin;
    private final Player player;
    private final List<Plot> plotList;
    private Inventory inv;
    private int page = 0;
    private static final int PER_PAGE = 45;

    public PlotMenuGUI(PlotCore plugin, Player player) {
        this.plugin   = plugin;
        this.player   = player;
        this.plotList = new ArrayList<>(plugin.getPlotManager().getAllPlots());
    }

    public void open() {
        build();
        player.openInventory(inv);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        int totalPages = Math.max(1, (int) Math.ceil(plotList.size() / (double) PER_PAGE));
        String title = MessageUtils.c("&8Plots &7(" + (page+1) + "/" + totalPages + ")");
        inv = Bukkit.createInventory(null, 54, title);

        // Plots
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && (start+i) < plotList.size(); i++) {
            inv.setItem(i, buildPlotItem(plotList.get(start+i)));
        }

        // Bottom bar
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        if (page > 0) inv.setItem(45, makeItemLore(Material.ARROW, "&a\u2190 Previous", List.of()));
        inv.setItem(49, makeItemLore(Material.NETHER_STAR, "&e&lPlotCore Menu",
            List.of(MessageUtils.c("&7Total plots: &f" + plotList.size()),
                    MessageUtils.c("&7Your plot: &f" + (plugin.getPlotManager().getPlotOwnedBy(player.getUniqueId()) != null
                        ? plugin.getPlotManager().getPlotOwnedBy(player.getUniqueId()).getName() : "None")))));
        if ((page+1) < totalPages) inv.setItem(53, makeItemLore(Material.ARROW, "&aNext \u2192", List.of()));
        inv.setItem(48, makeItem(Material.BARRIER, "&c  Close  "));
    }

    private ItemStack buildPlotItem(Plot plot) {
        boolean mine = plot.getOwnerUUID() != null && plot.getOwnerUUID().equals(player.getUniqueId());
        Material mat = mine ? Material.GREEN_STAINED_GLASS_PANE
            : plot.isRented() ? Material.RED_STAINED_GLASS_PANE
            : Material.LIME_STAINED_GLASS_PANE;

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.c("&7Status: " + (mine ? "&aYours" : plot.isRented() ? "&cRented" : "&2Available")));
        if (plot.isRented()) {
            lore.add(MessageUtils.c("&7Owner: &f" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Unknown")));
            lore.add(MessageUtils.c("&7Time left: &f" + plot.getTimeLeftFormatted()));
        }
        lore.add(MessageUtils.c("&7Price: &f" + (int) plot.getRentPrice() + " &7/ &f" + Plot.formatSeconds(plot.getRentDuration())));
        lore.add("");
        if (!plot.isRented()) lore.add(MessageUtils.c("&a\u25ba Click to rent / view"));
        else if (mine)        lore.add(MessageUtils.c("&e\u25ba Click to manage"));
        else                  lore.add(MessageUtils.c("&c\u2718 Already rented"));

        return makeItemLore(mat, "&6" + plot.getName(), lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv) || !e.getWhoClicked().equals(player)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == 45 && page > 0) { page--; build(); player.openInventory(inv); return; }
        if (slot == 53) { page++; build(); player.openInventory(inv); return; }
        if (slot == 48) { player.closeInventory(); return; }

        if (slot < 45) {
            int idx = page * PER_PAGE + slot;
            if (idx >= plotList.size()) return;
            Plot plot = plotList.get(idx);
            player.closeInventory();
            new SignGUI(plugin, player, plot).open();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inv) && e.getPlayer().equals(player))
            HandlerList.unregisterAll(this);
    }

    private ItemStack makeItem(Material mat, String name) {
        ItemStack i = new ItemStack(mat); ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtils.c(name)); i.setItemMeta(m); } return i;
    }
    private ItemStack makeItemLore(Material mat, String name, List<String> lore) {
        ItemStack i = new ItemStack(mat); ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtils.c(name)); m.setLore(lore); i.setItemMeta(m); } return i;
    }
}
