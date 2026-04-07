package me.plotcore.managers;

import me.plotcore.models.PlotSelection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private static final int WAND_CMD = 64048;
    private final Map<UUID, PlotSelection> selections = new HashMap<>();

    public ItemStack getWand() {
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7aPlot Selection Wand");
            meta.setLore(List.of("\u00a77Left-click \u00bb Corner 1", "\u00a77Right-click \u00bb Corner 2"));
            meta.setCustomModelData(WAND_CMD);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) return false;
        ItemMeta m = item.getItemMeta();
        return m != null && m.hasCustomModelData() && m.getCustomModelData() == WAND_CMD;
    }

    public void setCorner1(UUID id, Location loc) { getOrCreate(id).setCorner1(loc); }
    public void setCorner2(UUID id, Location loc) { getOrCreate(id).setCorner2(loc); }
    public PlotSelection getSelection(UUID id)    { return selections.get(id); }
    public void clear(UUID id)                    { selections.remove(id); }
    private PlotSelection getOrCreate(UUID id)    { return selections.computeIfAbsent(id, k -> new PlotSelection()); }
}
