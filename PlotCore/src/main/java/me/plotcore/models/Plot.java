package me.plotcore.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Plot {

    private final String name;
    private String worldName;
    private int x1, y1, z1, x2, y2, z2;

    private UUID ownerUUID;
    private String ownerName;
    private long expiryTime = -1;   // ms timestamp, -1 = no expiry set
    private double rentPrice;
    private long rentDuration;      // seconds

    private List<UUID> trusted = new ArrayList<>();
    private Location signLocation;  // where the auto-sign is placed

    public Plot(String name) {
        this.name = name;
        this.rentPrice = 100;
        this.rentDuration = 86400;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public boolean isRented() {
        if (ownerUUID == null) return false;
        if (expiryTime == -1) return true;
        return expiryTime > System.currentTimeMillis();
    }

    public boolean isExpired() {
        return ownerUUID != null && expiryTime != -1 && expiryTime <= System.currentTimeMillis();
    }

    public void evict() {
        ownerUUID = null;
        ownerName = null;
        expiryTime = -1;
        trusted.clear();
    }

    // ── Spatial ───────────────────────────────────────────────────────────────

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= Math.min(x1,x2) && bx <= Math.max(x1,x2)
            && by >= Math.min(y1,y2) && by <= Math.max(y1,y2)
            && bz >= Math.min(z1,z2) && bz <= Math.max(z1,z2);
    }

    public boolean overlaps(Plot o) {
        if (!worldName.equals(o.worldName)) return false;
        return Math.min(x1,x2) <= Math.max(o.x1,o.x2) && Math.max(x1,x2) >= Math.min(o.x1,o.x2)
            && Math.min(y1,y2) <= Math.max(o.y1,o.y2) && Math.max(y1,y2) >= Math.min(o.y1,o.y2)
            && Math.min(z1,z2) <= Math.max(o.z1,o.z2) && Math.max(z1,z2) >= Math.min(o.z1,o.z2);
    }

    // ── Trust ─────────────────────────────────────────────────────────────────

    public boolean isTrusted(UUID uuid) { return trusted.contains(uuid); }

    public boolean canAccess(UUID uuid) {
        return (ownerUUID != null && ownerUUID.equals(uuid)) || isTrusted(uuid);
    }

    public void addTrusted(UUID uuid) { if (!trusted.contains(uuid)) trusted.add(uuid); }
    public void removeTrusted(UUID uuid) { trusted.remove(uuid); }

    // ── Time ──────────────────────────────────────────────────────────────────

    public String getTimeLeftFormatted() {
        if (!isRented()) return "N/A";
        if (expiryTime == -1) return "Permanent";
        long secs = Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000);
        return formatSeconds(secs);
    }

    public static String formatSeconds(long s) {
        if (s <= 0) return "0s";
        long d = s/86400; s %= 86400;
        long h = s/3600;  s %= 3600;
        long m = s/60;    s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (d == 0 && h == 0) sb.append(s).append("s");
        return sb.toString().trim();
    }

    public long getTimeLeftSeconds() {
        if (!isRented() || expiryTime == -1) return 0;
        return Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000);
    }

    public void setTimeFromNow(long seconds) {
        expiryTime = System.currentTimeMillis() + seconds * 1000;
    }

    public void addTime(long seconds) {
        if (expiryTime == -1) expiryTime = System.currentTimeMillis() + seconds * 1000;
        else expiryTime += seconds * 1000;
    }

    public void removeTime(long seconds) {
        if (expiryTime != -1) expiryTime = Math.max(System.currentTimeMillis(), expiryTime - seconds * 1000);
    }

    // ── Location helpers ──────────────────────────────────────────────────────

    public Location getCenter() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w,
            (Math.min(x1,x2) + Math.max(x1,x2)) / 2.0,
            (Math.min(y1,y2) + Math.max(y1,y2)) / 2.0,
            (Math.min(z1,z2) + Math.max(z1,z2)) / 2.0);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getName()   { return name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String w) { worldName = w; }

    public int getX1() { return x1; } public int getY1() { return y1; } public int getZ1() { return z1; }
    public int getX2() { return x2; } public int getY2() { return y2; } public int getZ2() { return z2; }
    public void setCorner1(int x,int y,int z){ x1=x; y1=y; z1=z; }
    public void setCorner2(int x,int y,int z){ x2=x; y2=y; z2=z; }

    public UUID getOwnerUUID()  { return ownerUUID; }
    public void setOwnerUUID(UUID u) { ownerUUID = u; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String n) { ownerName = n; }

    public long getExpiryTime()  { return expiryTime; }
    public void setExpiryTime(long t) { expiryTime = t; }

    public double getRentPrice()   { return rentPrice; }
    public void setRentPrice(double p) { rentPrice = p; }
    public long getRentDuration()  { return rentDuration; }
    public void setRentDuration(long d) { rentDuration = d; }

    public List<UUID> getTrusted() { return trusted; }
    public void setTrusted(List<UUID> t) { trusted = t; }

    public Location getSignLocation() { return signLocation; }
    public void setSignLocation(Location l) { signLocation = l; }
}
