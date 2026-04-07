package me.plotcore.models;

import org.bukkit.Location;

public class PlotSelection {
    private Location c1, c2;
    public boolean isComplete() {
        return c1 != null && c2 != null && c1.getWorld() != null && c2.getWorld() != null
            && c1.getWorld().getName().equals(c2.getWorld().getName());
    }
    public Location getCorner1() { return c1; }
    public void setCorner1(Location l) { c1 = l; }
    public Location getCorner2() { return c2; }
    public void setCorner2(Location l) { c2 = l; }
}
