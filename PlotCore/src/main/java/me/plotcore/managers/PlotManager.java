package me.plotcore.managers;

import me.plotcore.PlotCore;
import me.plotcore.models.Plot;
import me.plotcore.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlotManager {

    private final PlotCore plugin;
    private final Map<String, Plot> plots = new LinkedHashMap<>();
    private final Set<UUID> bypassers = new HashSet<>();

    private File dataFile;
    private FileConfiguration dataConfig;
    private BukkitTask expiryTask;

    public PlotManager(PlotCore plugin) {
        this.plugin = plugin;
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "plots.yml");
        if (!dataFile.exists()) plugin.saveResource("plots.yml", false);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        plots.clear();

        ConfigurationSection sec = dataConfig.getConfigurationSection("plots");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection ps = sec.getConfigurationSection(key);
            if (ps == null) continue;
            Plot p = new Plot(key);
            p.setWorldName(ps.getString("world", "world"));
            p.setCorner1(ps.getInt("x1"), ps.getInt("y1"), ps.getInt("z1"));
            p.setCorner2(ps.getInt("x2"), ps.getInt("y2"), ps.getInt("z2"));
            p.setRentPrice(ps.getDouble("price", 100));
            p.setRentDuration(ps.getLong("duration", 86400));
            p.setExpiryTime(ps.getLong("expiry", -1));

            String ownerStr = ps.getString("owner", "");
            if (!ownerStr.isEmpty()) {
                try { p.setOwnerUUID(UUID.fromString(ownerStr)); } catch (Exception ignored) {}
            }
            p.setOwnerName(ps.getString("ownerName", null));

            List<UUID> trusted = new ArrayList<>();
            for (String s : ps.getStringList("trusted")) {
                try { trusted.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
            p.setTrusted(trusted);

            // Sign location
            if (ps.contains("sign.world")) {
                World w = Bukkit.getWorld(Objects.requireNonNull(ps.getString("sign.world")));
                if (w != null) {
                    p.setSignLocation(new Location(w,
                        ps.getDouble("sign.x"), ps.getDouble("sign.y"), ps.getDouble("sign.z")));
                }
            }
            plots.put(key.toLowerCase(), p);
        }
        plugin.getLogger().info("Loaded " + plots.size() + " plot(s).");
    }

    public void saveAll() {
        dataConfig.set("plots", null);
        for (Plot p : plots.values()) {
            String path = "plots." + p.getName();
            dataConfig.set(path + ".world", p.getWorldName());
            dataConfig.set(path + ".x1", p.getX1()); dataConfig.set(path + ".y1", p.getY1()); dataConfig.set(path + ".z1", p.getZ1());
            dataConfig.set(path + ".x2", p.getX2()); dataConfig.set(path + ".y2", p.getY2()); dataConfig.set(path + ".z2", p.getZ2());
            dataConfig.set(path + ".price", p.getRentPrice());
            dataConfig.set(path + ".duration", p.getRentDuration());
            dataConfig.set(path + ".expiry", p.getExpiryTime());
            dataConfig.set(path + ".owner", p.getOwnerUUID() != null ? p.getOwnerUUID().toString() : null);
            dataConfig.set(path + ".ownerName", p.getOwnerName());
            List<String> tr = new ArrayList<>();
            p.getTrusted().forEach(u -> tr.add(u.toString()));
            dataConfig.set(path + ".trusted", tr);
            if (p.getSignLocation() != null) {
                Location sl = p.getSignLocation();
                dataConfig.set(path + ".sign.world", sl.getWorld().getName());
                dataConfig.set(path + ".sign.x", sl.getX());
                dataConfig.set(path + ".sign.y", sl.getY());
                dataConfig.set(path + ".sign.z", sl.getZ());
            }
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Could not save plots.yml! " + e.getMessage()); }
    }

    public void reload() {
        if (expiryTask != null) expiryTask.cancel();
        load();
        startExpiryTask();
    }

    // ── Expiry checker ────────────────────────────────────────────────────────

    public void startExpiryTask() {
        int interval = plugin.getConfig().getInt("settings.expiry-check-interval", 60) * 20;
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long warningThreshold = 3600L;
            int checkSec = plugin.getConfig().getInt("settings.expiry-check-interval", 60);
            for (Plot plot : new ArrayList<>(plots.values())) {
                if (plot.isExpired()) {
                    Player owner = plot.getOwnerUUID() != null ? Bukkit.getPlayer(plot.getOwnerUUID()) : null;
                    if (owner != null) MessageUtils.send(owner, "plot-expired", "{name}", plot.getName());
                    plot.evict();
                    SignManager.refreshSign(plot);
                    saveAll();
                    MessageUtils.log("&ePlot &6" + plot.getName() + " &eexpired and was cleared.");
                    continue;
                }
                if (!plot.isRented()) continue;
                long left = plot.getTimeLeftSeconds();
                if (left <= warningThreshold && left > (warningThreshold - checkSec)) {
                    Player owner = plot.getOwnerUUID() != null ? Bukkit.getPlayer(plot.getOwnerUUID()) : null;
                    if (owner != null)
                        MessageUtils.send(owner, "plot-expired", "{name}", plot.getName()); // reuse msg key
                }
            }
        }, interval, interval);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public void addPlot(Plot plot) {
        plots.put(plot.getName().toLowerCase(), plot);
        saveAll();
    }

    public boolean removePlot(String name) {
        if (plots.remove(name.toLowerCase()) == null) return false;
        saveAll();
        return true;
    }

    public Plot getPlot(String name) {
        return name == null ? null : plots.get(name.toLowerCase());
    }

    public Plot getPlotAt(Location loc) {
        if (loc == null) return null;
        for (Plot p : plots.values()) if (p.contains(loc)) return p;
        return null;
    }

    public Plot getPlotBySign(Location loc) {
        if (loc == null) return null;
        for (Plot p : plots.values()) {
            Location sl = p.getSignLocation();
            if (sl != null && sl.getWorld() != null
                && sl.getWorld().getName().equals(loc.getWorld().getName())
                && sl.getBlockX() == loc.getBlockX()
                && sl.getBlockY() == loc.getBlockY()
                && sl.getBlockZ() == loc.getBlockZ()) return p;
        }
        return null;
    }

    public boolean overlapsAny(Plot candidate) {
        for (Plot existing : plots.values()) {
            if (existing.getName().equalsIgnoreCase(candidate.getName())) continue;
            if (candidate.overlaps(existing)) return true;
        }
        return false;
    }

    /** Returns the plot currently rented by this player, or null */
    public Plot getPlotOwnedBy(UUID uuid) {
        for (Plot p : plots.values())
            if (p.isRented() && uuid.equals(p.getOwnerUUID())) return p;
        return null;
    }

    // ── Rent / Disband ────────────────────────────────────────────────────────

    /**
     * Attempt to rent a plot. Returns true on success.
     * Deducts currency, sets owner, refreshes sign.
     */
    public boolean rentPlot(Player player, Plot plot) {
        if (!plugin.getCurrencyManager().take(player, (int) plot.getRentPrice())) return false;
        plot.setOwnerUUID(player.getUniqueId());
        plot.setOwnerName(player.getName());
        plot.setTimeFromNow(plot.getRentDuration());
        SignManager.refreshSign(plot);
        saveAll();
        return true;
    }

    /**
     * Disband (leave) a plot – owner gives it up voluntarily.
     */
    public void disbandPlot(Plot plot) {
        plot.evict();
        SignManager.refreshSign(plot);
        saveAll();
    }

    // ── Bypass ────────────────────────────────────────────────────────────────

    public boolean isBypassing(UUID id) { return bypassers.contains(id); }

    public boolean toggleBypass(UUID id) {
        if (bypassers.remove(id)) return false;
        bypassers.add(id); return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Collection<Plot> getAllPlots() { return plots.values(); }
    public int getPlotCount()            { return plots.size(); }
}
