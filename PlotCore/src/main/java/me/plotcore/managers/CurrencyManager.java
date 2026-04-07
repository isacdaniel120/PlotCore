package me.plotcore.managers;

import me.plotcore.PlotCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CurrencyManager {

    private final PlotCore plugin;
    private ItemStack currencyItem;

    public CurrencyManager(PlotCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currencyItem = buildFromConfig();
    }

    private ItemStack buildFromConfig() {
        String matName = plugin.getConfig().getString("currency.material", "EMERALD");
        Material mat;
        try { mat = Material.valueOf(matName.toUpperCase()); }
        catch (Exception e) { mat = Material.EMERALD; }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.getConfig().getString("currency.name", "");
        if (!name.isEmpty()) meta.setDisplayName(name.replace("&", "\u00a7"));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("currency.lore"))
            lore.add(line.replace("&", "\u00a7"));
        if (!lore.isEmpty()) meta.setLore(lore);

        int cmd = plugin.getConfig().getInt("currency.custom-model-data", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getCurrencyItem() { return currencyItem.clone(); }

    /** Set currency from an item in player's hand, save to config */
    public void setCurrencyItem(ItemStack item) {
        this.currencyItem = item.clone();
        this.currencyItem.setAmount(1);

        plugin.getConfig().set("currency.material", item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            plugin.getConfig().set("currency.name", meta.hasDisplayName() ? meta.getDisplayName() : "");
            plugin.getConfig().set("currency.lore", meta.hasLore() ? meta.getLore() : new ArrayList<>());
            plugin.getConfig().set("currency.custom-model-data", meta.hasCustomModelData() ? meta.getCustomModelData() : 0);
        }
        plugin.saveConfig();
    }

    public boolean isCurrency(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() != currencyItem.getType()) return false;
        ItemMeta cm = currencyItem.getItemMeta();
        ItemMeta im = item.getItemMeta();
        if (cm == null) return true;
        if (im == null) return false;
        if (cm.hasDisplayName() && (!im.hasDisplayName() || !im.getDisplayName().equals(cm.getDisplayName()))) return false;
        if (cm.hasCustomModelData() && (!im.hasCustomModelData() || im.getCustomModelData() != cm.getCustomModelData())) return false;
        return true;
    }

    public int count(Player p) {
        int total = 0;
        for (ItemStack item : p.getInventory().getContents())
            if (isCurrency(item)) total += item.getAmount();
        return total;
    }

    public boolean has(Player p, int amount) { return count(p) >= amount; }

    public boolean take(Player p, int amount) {
        if (!has(p, amount)) return false;
        int remaining = amount;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            if (!isCurrency(contents[i])) continue;
            if (contents[i].getAmount() <= remaining) {
                remaining -= contents[i].getAmount();
                contents[i] = null;
            } else {
                contents[i].setAmount(contents[i].getAmount() - remaining);
                remaining = 0;
            }
        }
        p.getInventory().setContents(contents);
        return true;
    }

    public void give(Player p, int amount) {
        ItemStack item = getCurrencyItem();
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, item.getMaxStackSize());
            ItemStack stack = item.clone(); stack.setAmount(batch);
            p.getInventory().addItem(stack).forEach((slot, leftover) ->
                p.getWorld().dropItemNaturally(p.getLocation(), leftover));
            remaining -= batch;
        }
    }

    public String getCurrencyDisplayName() {
        ItemMeta meta = currencyItem.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return currencyItem.getType().name();
    }
}
