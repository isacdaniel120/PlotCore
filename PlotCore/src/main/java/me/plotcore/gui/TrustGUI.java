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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for managing trusted players on a plot.
 * Top 4 rows = trusted player heads. Bottom row = controls.
 */
public class TrustGUI implements Listener {

    private final PlotCore plugin;
    private final Player player;
    private final Plot plot;
    private Inventory inv;

    private static final int MAX_DISPLAY = 28; // slots 0-27
    private static final int SLOT_ADD    = 45;
    private static final int SLOT_BACK   = 49;
    private static final int SLOT_INFO   = 53;

    public TrustGUI(PlotCore plugin, Player player, Plot plot) {
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
        int max = plugin.getConfig().getInt("settings.max-trusted", 4);
        inv = Bukkit.createInventory(null, 54,
            MessageUtils.c("&8Trusted &7\u00bb &e" + plot.getName() + " &8(" + plot.getTrusted().size() + "/" + max + ")"));

        // Fill all with glass
        ItemStack bg = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Trusted player heads in slots 0-27
        List<UUID> trusted = plot.getTrusted();
        for (int i = 0; i < trusted.size() && i < MAX_DISPLAY; i++) {
            UUID uuid = trusted.get(i);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                sm.setDisplayName(MessageUtils.c("&b" + name));
                sm.setLore(List.of(MessageUtils.c("&7Click to &cremove &7from trusted")));
                skull.setItemMeta(sm);
            }
            inv.setItem(i, skull);
        }

        // Add trusted button
        boolean atMax = trusted.size() >= max;
        inv.setItem(SLOT_ADD, makeItemLore(
            atMax ? Material.BARRIER : Material.LIME_DYE,
            atMax ? "&c  Max trusted reached  " : "&a  Add Trusted Player  ",
            atMax
                ? List.of(MessageUtils.c("&7Max: &c" + max + " &7players"))
                : List.of(MessageUtils.c("&7Type the player name in chat"), MessageUtils.c("&7after clicking this."), "", MessageUtils.c("&a\u25ba Click to begin"))));

        inv.setItem(SLOT_BACK, makeItem(Material.ARROW, "&7  Back  "));

        inv.setItem(SLOT_INFO, makeItemLore(Material.PAPER, "&eTrusted Info",
            List.of(MessageUtils.c("&7Players: &f" + trusted.size() + "&7/&f" + max),
                    MessageUtils.c("&7Trusted players can"),
                    MessageUtils.c("&7build, use containers,"),
                    MessageUtils.c("&7and interact in the plot."))));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv) || !e.getWhoClicked().equals(player)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Clicked a player head = remove trusted
        if (slot < MAX_DISPLAY && inv.getItem(slot) != null && inv.getItem(slot).getType() == Material.PLAYER_HEAD) {
            List<UUID> trusted = plot.getTrusted();
            if (slot < trusted.size()) {
                UUID uuid = trusted.get(slot);
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                plot.removeTrusted(uuid);
                plugin.getPlotManager().saveAll();
                MessageUtils.send(player, "trust-removed", "{player}", name != null ? name : "?", "{name}", plot.getName());
                build(); player.openInventory(inv);
            }
            return;
        }

        if (slot == SLOT_BACK) {
            player.closeInventory();
            new SignGUI(plugin, player, plot).open();
            return;
        }

        if (slot == SLOT_ADD) {
            int max = plugin.getConfig().getInt("settings.max-trusted", 4);
            if (plot.getTrusted().size() >= max) {
                MessageUtils.send(player, "trust-max", "{max}", String.valueOf(max));
                return;
            }
            // Enter chat input mode
            player.closeInventory();
            MessageUtils.sendRaw(player, "&8[&aPlotCore&8] &7Type the player name to trust, or &ccancel&7:");
            // Register one-time chat listener
            Bukkit.getPluginManager().registerEvents(new TrustChatListener(plugin, player, plot), plugin);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inv) && e.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtils.c(name)); item.setItemMeta(m); }
        return item;
    }

    private ItemStack makeItemLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtils.c(name)); m.setLore(lore); item.setItemMeta(m); }
        return item;
    }
}
