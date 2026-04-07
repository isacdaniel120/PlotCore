package me.plotcore.gui;

import me.plotcore.PlotCore;
import me.plotcore.managers.PlotManager;
import me.plotcore.managers.SignManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI that opens when a player right-clicks a plot sign.
 * Shows different options based on whether the player is owner, outsider, or admin.
 */
public class SignGUI implements Listener {

    private final PlotCore plugin;
    private final Player player;
    private final Plot plot;
    private Inventory inv;

    // Slot layout for a 54-slot GUI
    private static final int SLOT_INFO    = 4;
    private static final int SLOT_RENT    = 20;  // Unrented: Rent
    private static final int SLOT_EXTEND  = 20;  // Owner: Extend
    private static final int SLOT_PAY     = 24;  // Owner: Pay (extend alias)
    private static final int SLOT_TRUST   = 29;  // Owner: Trust management
    private static final int SLOT_DISBAND = 33;  // Owner: Disband
    private static final int SLOT_CLOSE   = 49;

    public SignGUI(PlotCore plugin, Player player, Plot plot) {
        this.plugin = plugin;
        this.player = player;
        this.plot   = plot;
    }

    public void open() {
        build();
        player.openInventory(inv);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        boolean isOwner  = plot.getOwnerUUID() != null && plot.getOwnerUUID().equals(player.getUniqueId());
        boolean isRented = plot.isRented();
        boolean isAdmin  = player.hasPermission("jplots.admin");

        String title = isRented
            ? MessageUtils.c("&8Plot &7\u00bb &e" + plot.getName() + " &8\u00bb &cRented")
            : MessageUtils.c("&8Plot &7\u00bb &e" + plot.getName() + " &8\u00bb &aAvailable");

        inv = Bukkit.createInventory(null, 54, title);

        // ── Decorative border ──
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);
        // Inner clear area
        for (int slot : new int[]{10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43}) {
            inv.setItem(slot, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // ── Info item (centre top) ──
        inv.setItem(SLOT_INFO, buildInfoItem());

        if (!isRented) {
            // ── RENT button ──
            boolean canAfford = plugin.getCurrencyManager().has(player, (int) plot.getRentPrice());
            List<String> rentLore = new ArrayList<>();
            rentLore.add(MessageUtils.c("&7Duration: &f" + Plot.formatSeconds(plot.getRentDuration())));
            rentLore.add(MessageUtils.c("&7Cost: &f" + (int) plot.getRentPrice() + " &7" + plugin.getCurrencyManager().getCurrencyDisplayName()));
            rentLore.add("");
            rentLore.add(canAfford ? MessageUtils.c("&a\u25ba Click to rent!") : MessageUtils.c("&c\u2718 Not enough currency!"));
            inv.setItem(SLOT_RENT, makeItemLore(
                canAfford ? Material.LIME_DYE : Material.GRAY_DYE,
                canAfford ? "&a&l  RENT PLOT  " : "&7  RENT PLOT  ",
                rentLore));
        } else if (isOwner) {
            // ── EXTEND / PAY (left) ──
            boolean canAfford = plugin.getCurrencyManager().has(player, (int) plot.getRentPrice());
            List<String> extLore = new ArrayList<>();
            extLore.add(MessageUtils.c("&7Adds: &f" + Plot.formatSeconds(plot.getRentDuration())));
            extLore.add(MessageUtils.c("&7Cost: &f" + (int) plot.getRentPrice() + " &7" + plugin.getCurrencyManager().getCurrencyDisplayName()));
            extLore.add(MessageUtils.c("&7Your balance: &f" + plugin.getCurrencyManager().count(player)));
            extLore.add("");
            extLore.add(canAfford ? MessageUtils.c("&a\u25ba Click to extend!") : MessageUtils.c("&c\u2718 Not enough currency!"));
            inv.setItem(SLOT_EXTEND, makeItemLore(Material.CLOCK, "&e&l  EXTEND RENTAL  ", extLore));

            // ── PAY (same as extend, slot 24) ──
            inv.setItem(SLOT_PAY, makeItemLore(Material.GOLD_INGOT, "&6&l  PAY / RENEW  ", extLore));

            // ── TRUST (slot 29) ──
            List<String> trustLore = buildTrustLore();
            inv.setItem(SLOT_TRUST, makeItemLore(Material.PLAYER_HEAD, "&b&l  TRUSTED PLAYERS  ", trustLore));

            // ── DISBAND (slot 33) ──
            inv.setItem(SLOT_DISBAND, makeItemLore(Material.BARRIER, "&c&l  DISBAND PLOT  ",
                List.of(MessageUtils.c("&7Leave this plot."),
                        MessageUtils.c("&cRemaining time will be lost!"),
                        "",
                        MessageUtils.c("&c\u25ba Click to disband"))));
        } else if (isAdmin) {
            // Admin sees evict button
            inv.setItem(SLOT_RENT, makeItemLore(Material.BLAZE_POWDER, "&c&l  ADMIN EVICT  ",
                List.of(MessageUtils.c("&7Force evict &e" + plot.getOwnerName()),
                        "", MessageUtils.c("&c\u25ba Click to evict"))));
        }

        // ── CLOSE ──
        inv.setItem(SLOT_CLOSE, makeItem(Material.DARK_OAK_BUTTON, "&7  Close  "));
    }

    private ItemStack buildInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.c("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        lore.add(MessageUtils.c("&7Status:  " + (plot.isRented() ? "&cRented" : "&aAvailable")));
        if (plot.isRented()) {
            lore.add(MessageUtils.c("&7Owner:   &f" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Unknown")));
            lore.add(MessageUtils.c("&7Expires: &f" + plot.getTimeLeftFormatted()));
            lore.add(MessageUtils.c("&7Trusted: &f" + plot.getTrusted().size() + "&7/&f" + plugin.getConfig().getInt("settings.max-trusted", 4)));
        }
        lore.add(MessageUtils.c("&7Price:   &f" + (int) plot.getRentPrice() + " &7per &f" + Plot.formatSeconds(plot.getRentDuration())));
        lore.add(MessageUtils.c("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        return makeItemLore(Material.PAPER, "&e&l  " + plot.getName() + "  ", lore);
    }

    private List<String> buildTrustLore() {
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.c("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        int max = plugin.getConfig().getInt("settings.max-trusted", 4);
        if (plot.getTrusted().isEmpty()) {
            lore.add(MessageUtils.c("&7No trusted players."));
        } else {
            for (UUID uuid : plot.getTrusted()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                lore.add(MessageUtils.c("  &7\u25cf &f" + (name != null ? name : uuid.toString().substring(0,8))));
            }
        }
        lore.add(MessageUtils.c("&8(" + plot.getTrusted().size() + "/" + max + ")"));
        lore.add("");
        lore.add(MessageUtils.c("&e\u25ba Click to open Trust Manager"));
        return lore;
    }

    // ── Event Handling ────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv) || !e.getWhoClicked().equals(player)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        boolean isOwner  = plot.getOwnerUUID() != null && plot.getOwnerUUID().equals(player.getUniqueId());
        boolean isRented = plot.isRented();
        boolean isAdmin  = player.hasPermission("jplots.admin");

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        if (slot == SLOT_INFO) return; // decorative

        if (!isRented && slot == SLOT_RENT) {
            // Rent
            if (!player.hasPermission("jplots.use")) {
                MessageUtils.send(player, "no-permission");
                player.closeInventory(); return;
            }
            boolean onePlot = plugin.getConfig().getBoolean("settings.one-plot-per-player", true);
            if (onePlot && plugin.getPlotManager().getPlotOwnedBy(player.getUniqueId()) != null) {
                MessageUtils.send(player, "already-renting");
                player.closeInventory(); return;
            }
            if (!plugin.getCurrencyManager().has(player, (int) plot.getRentPrice())) {
                MessageUtils.send(player, "not-enough-currency", "{amount}", String.valueOf((int) plot.getRentPrice()));
                player.closeInventory(); return;
            }
            boolean ok = plugin.getPlotManager().rentPlot(player, plot);
            if (ok) {
                MessageUtils.send(player, "plot-rented", "{name}", plot.getName(), "{time}", Plot.formatSeconds(plot.getRentDuration()));
            }
            player.closeInventory(); return;
        }

        if (isOwner) {
            if (slot == SLOT_EXTEND || slot == SLOT_PAY) {
                if (!plugin.getCurrencyManager().has(player, (int) plot.getRentPrice())) {
                    MessageUtils.send(player, "not-enough-currency", "{amount}", String.valueOf((int) plot.getRentPrice()));
                    player.closeInventory(); return;
                }
                plugin.getCurrencyManager().take(player, (int) plot.getRentPrice());
                plot.addTime(plot.getRentDuration());
                SignManager.refreshSign(plot);
                plugin.getPlotManager().saveAll();
                MessageUtils.send(player, "plot-rented", "{name}", plot.getName(), "{time}", Plot.formatSeconds(plot.getRentDuration()));
                player.closeInventory(); return;
            }
            if (slot == SLOT_TRUST) {
                player.closeInventory();
                new TrustGUI(plugin, player, plot).open();
                return;
            }
            if (slot == SLOT_DISBAND) {
                plugin.getPlotManager().disbandPlot(plot);
                MessageUtils.send(player, "plot-disbanded", "{name}", plot.getName());
                player.closeInventory(); return;
            }
        }

        if (isAdmin && isRented && slot == SLOT_RENT) {
            // Admin evict
            if (plot.getOwnerUUID() != null) {
                Player evicted = Bukkit.getPlayer(plot.getOwnerUUID());
                if (evicted != null) MessageUtils.send(evicted, "plot-evicted", "{name}", plot.getName());
            }
            plugin.getPlotManager().disbandPlot(plot);
            MessageUtils.send(player, "player-evicted", "{name}", plot.getName());
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inv) && e.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtils.c(name)); item.setItemMeta(m); }
        return item;
    }

    private ItemStack makeItemLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtils.c(name));
            m.setLore(lore);
            item.setItemMeta(m);
        }
        return item;
    }
}
