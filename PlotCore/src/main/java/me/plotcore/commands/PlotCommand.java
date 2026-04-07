package me.plotcore.commands;

import me.plotcore.PlotCore;
import me.plotcore.gui.PlotMenuGUI;
import me.plotcore.managers.PlotManager;
import me.plotcore.managers.SelectionManager;
import me.plotcore.managers.SignManager;
import me.plotcore.models.Plot;
import me.plotcore.models.PlotSelection;
import me.plotcore.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlotCommand implements CommandExecutor, TabCompleter {

    private final PlotCore plugin;
    private final PlotManager pm;
    private final SelectionManager sm;

    public PlotCommand(PlotCore plugin) {
        this.plugin = plugin;
        this.pm = plugin.getPlotManager();
        this.sm = plugin.getSelectionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "help"         -> sendHelp(sender);
            case "wand"         -> cmdWand(sender);
            case "create"       -> cmdCreate(sender, args);
            case "delete"       -> cmdDelete(sender, args);
            case "time"         -> cmdTime(sender, args);
            case "evict"        -> cmdEvict(sender, args);
            case "sign"         -> cmdSign(sender, args);
            case "setprice"     -> cmdSetPrice(sender, args);
            case "currencyset"  -> cmdCurrencySet(sender);
            case "currencyget"  -> cmdCurrencyGet(sender, args);
            case "bypass"       -> cmdBypass(sender);
            case "trust"        -> cmdTrust(sender, args);
            case "info"         -> cmdInfo(sender, args);
            case "menu"         -> cmdMenu(sender);
            case "reload"       -> cmdReload(sender);
            default             -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        sendRaw(s, "&8&m────────────────────────────────");
        sendRaw(s, " &aPlotCore &7v2.0 &8| &7Commands");
        sendRaw(s, "&8&m────────────────────────────────");
        sendRaw(s, " &a/plot wand &8- &7Get selection wand");
        sendRaw(s, " &a/plot create <name> &8- &7Create plot from selection");
        sendRaw(s, " &a/plot delete <name> &8- &7Delete a plot");
        sendRaw(s, " &a/plot time <set|add|remove> <name> <time> &8- &7Manage time");
        sendRaw(s, " &a/plot evict <name> &8- &7Evict player");
        sendRaw(s, " &a/plot sign <name> &8- &7Link sign to plot");
        sendRaw(s, " &a/plot setprice <name> <amount> &8- &7Set rent price");
        sendRaw(s, " &a/plot currencyset &8- &7Set currency to held item");
        sendRaw(s, " &a/plot currencyget <amount> &8- &7Get currency items");
        sendRaw(s, " &a/plot bypass &8- &7Toggle bypass mode");
        sendRaw(s, " &a/plot trust <add|remove|list> <player> &8- &7Trust");
        sendRaw(s, " &a/plot info [name] &8- &7Plot info");
        sendRaw(s, " &a/plot menu &8- &7Open GUI");
        sendRaw(s, " &a/plot reload &8- &7Reload config");
        sendRaw(s, "&8&m────────────────────────────────");
    }

    // ── /plot wand ────────────────────────────────────────────────────────────
    private void cmdWand(CommandSender s) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.create")) { msg(s, "no-permission"); return; }
        p.getInventory().addItem(sm.getWand());
        msg(s, "wand-given");
    }

    // ── /plot create <name> ───────────────────────────────────────────────────
    private void cmdCreate(CommandSender s, String[] args) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.create")) { msg(s, "no-permission"); return; }
        if (args.length < 2) { sendRaw(s, "&cUsage: /plot create <name>"); return; }

        String name = args[1];
        PlotSelection sel = sm.getSelection(p.getUniqueId());
        if (sel == null || !sel.isComplete()) { msg(s, "selection-incomplete"); return; }

        Plot plot = new Plot(name);
        plot.setWorldName(sel.getCorner1().getWorld().getName());
        plot.setCorner1(sel.getCorner1().getBlockX(), sel.getCorner1().getBlockY(), sel.getCorner1().getBlockZ());
        plot.setCorner2(sel.getCorner2().getBlockX(), sel.getCorner2().getBlockY(), sel.getCorner2().getBlockZ());
        plot.setRentPrice(plugin.getConfig().getDouble("settings.default-price", 100));
        plot.setRentDuration(plugin.getConfig().getLong("settings.default-duration", 86400));

        if (pm.overlapsAny(plot)) { msg(s, "selection-overlap"); return; }

        // Auto-place sign at player's location
        Material signMat = Material.getMaterial(
            plugin.getConfig().getString("settings.sign-material", "OAK_SIGN").toUpperCase());
        if (signMat == null) signMat = Material.OAK_SIGN;

        Location signLoc = SignManager.placeSign(plot, p.getLocation(), signMat);
        if (signLoc != null) plot.setSignLocation(signLoc);

        pm.addPlot(plot);
        sm.clear(p.getUniqueId());
        msg(s, "plot-created", "{name}", name);
    }

    // ── /plot delete <name> ───────────────────────────────────────────────────
    private void cmdDelete(CommandSender s, String[] args) {
        if (!s.hasPermission("jplots.create")) { msg(s, "no-permission"); return; }
        if (args.length < 2) { sendRaw(s, "&cUsage: /plot delete <name>"); return; }
        if (!pm.removePlot(args[1])) { msg(s, "plot-not-found", "{name}", args[1]); return; }
        msg(s, "plot-deleted", "{name}", args[1]);
    }

    // ── /plot time <set|add|remove> <name> <time> ─────────────────────────────
    private void cmdTime(CommandSender s, String[] args) {
        if (!s.hasPermission("jplots.admin")) { msg(s, "no-permission"); return; }
        if (args.length < 4) { sendRaw(s, "&cUsage: /plot time <set|add|remove> <name> <time>"); return; }
        Plot plot = pm.getPlot(args[2]);
        if (plot == null) { msg(s, "plot-not-found", "{name}", args[2]); return; }
        long secs = MessageUtils.parseTime(args[3]);
        if (secs < 0) { msg(s, "invalid-time"); return; }
        switch (args[1].toLowerCase()) {
            case "set"    -> plot.setTimeFromNow(secs);
            case "add"    -> plot.addTime(secs);
            case "remove" -> plot.removeTime(secs);
            default       -> { sendRaw(s, "&cUse: set, add, remove"); return; }
        }
        SignManager.refreshSign(plot);
        pm.saveAll();
        msg(s, "time-updated", "{name}", plot.getName());
    }

    // ── /plot evict <name> ────────────────────────────────────────────────────
    private void cmdEvict(CommandSender s, String[] args) {
        if (!s.hasPermission("jplots.evict")) { msg(s, "no-permission"); return; }
        if (args.length < 2) { sendRaw(s, "&cUsage: /plot evict <name>"); return; }
        Plot plot = pm.getPlot(args[1]);
        if (plot == null) { msg(s, "plot-not-found", "{name}", args[1]); return; }
        if (plot.getOwnerUUID() != null) {
            Player evicted = Bukkit.getPlayer(plot.getOwnerUUID());
            if (evicted != null) msg(evicted, "plot-evicted", "{name}", plot.getName());
        }
        pm.disbandPlot(plot);
        msg(s, "player-evicted", "{name}", args[1]);
    }

    // ── /plot sign <name> ─────────────────────────────────────────────────────
    private void cmdSign(CommandSender s, String[] args) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.admin")) { msg(s, "no-permission"); return; }
        if (args.length < 2) { sendRaw(s, "&cUsage: /plot sign <name>"); return; }
        Plot plot = pm.getPlot(args[1]);
        if (plot == null) { msg(s, "plot-not-found", "{name}", args[1]); return; }
        Block target = p.getTargetBlockExact(5);
        if (target == null) { sendRaw(s, "&cLook at a sign block!"); return; }
        plot.setSignLocation(target.getLocation());
        SignManager.refreshSign(plot);
        pm.saveAll();
        msg(s, "sign-set", "{name}", args[1]);
    }

    // ── /plot setprice <name> <amount> ────────────────────────────────────────
    private void cmdSetPrice(CommandSender s, String[] args) {
        if (!s.hasPermission("jplots.admin")) { msg(s, "no-permission"); return; }
        if (args.length < 3) { sendRaw(s, "&cUsage: /plot setprice <name> <amount>"); return; }
        Plot plot = pm.getPlot(args[1]);
        if (plot == null) { msg(s, "plot-not-found", "{name}", args[1]); return; }
        try {
            double amount = Double.parseDouble(args[2]);
            plot.setRentPrice(amount);
            SignManager.refreshSign(plot);
            pm.saveAll();
            sendRaw(s, "&aSet price for &e" + args[1] + " &ato &e" + amount + "&a.");
        } catch (NumberFormatException e) { msg(s, "invalid-number"); }
    }

    // ── /plot currencyset ─────────────────────────────────────────────────────
    private void cmdCurrencySet(CommandSender s) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.admin")) { msg(s, "no-permission"); return; }
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            sendRaw(s, "&cHold an item in your main hand!"); return;
        }
        plugin.getCurrencyManager().setCurrencyItem(held);
        msg(s, "currency-set", "{item}", held.getType().name());
    }

    // ── /plot currencyget <amount> ────────────────────────────────────────────
    private void cmdCurrencyGet(CommandSender s, String[] args) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.admin")) { msg(s, "no-permission"); return; }
        int amount = 1;
        if (args.length >= 2) {
            try { amount = Integer.parseInt(args[1]); } catch (Exception e) { msg(s, "invalid-number"); return; }
        }
        plugin.getCurrencyManager().give(p, amount);
        msg(s, "currency-given", "{amount}", String.valueOf(amount));
    }

    // ── /plot bypass ──────────────────────────────────────────────────────────
    private void cmdBypass(CommandSender s) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.bypass")) { msg(s, "no-permission"); return; }
        boolean now = pm.toggleBypass(p.getUniqueId());
        msg(s, now ? "bypass-on" : "bypass-off");
    }

    // ── /plot trust <add|remove|list> <player> ────────────────────────────────
    private void cmdTrust(CommandSender s, String[] args) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.use")) { msg(s, "no-permission"); return; }
        if (args.length < 2) { sendRaw(s, "&cUsage: /plot trust <add|remove|list> <player>"); return; }

        // Find the player's plot
        Plot plot = pm.getPlotOwnedBy(p.getUniqueId());
        if (plot == null && p.hasPermission("jplots.admin") && args.length >= 4) {
            plot = pm.getPlot(args[3]);
        }
        if (plot == null) plot = pm.getPlotAt(p.getLocation());
        if (plot == null || (!p.getUniqueId().equals(plot.getOwnerUUID()) && !p.hasPermission("jplots.admin"))) {
            sendRaw(s, "&cYou don't own a plot (or stand in one)!"); return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) { sendRaw(s, "&cSpecify player name!"); return; }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { sendRaw(s, "&cPlayer not online!"); return; }
                int max = plugin.getConfig().getInt("settings.max-trusted", 4);
                if (plot.getTrusted().size() >= max) { msg(s, "trust-max", "{max}", String.valueOf(max)); return; }
                if (plot.isTrusted(target.getUniqueId())) { msg(s, "trust-already", "{player}", target.getName()); return; }
                plot.addTrusted(target.getUniqueId());
                pm.saveAll();
                msg(s, "trust-added", "{player}", target.getName(), "{name}", plot.getName());
            }
            case "remove" -> {
                if (args.length < 3) { sendRaw(s, "&cSpecify player name!"); return; }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { sendRaw(s, "&cPlayer not online!"); return; }
                if (!plot.isTrusted(target.getUniqueId())) { msg(s, "trust-not-found", "{player}", target.getName()); return; }
                plot.removeTrusted(target.getUniqueId());
                pm.saveAll();
                msg(s, "trust-removed", "{player}", target.getName(), "{name}", plot.getName());
            }
            case "list" -> {
                sendRaw(s, "&aTrusted on &e" + plot.getName() + "&a:");
                if (plot.getTrusted().isEmpty()) { sendRaw(s, "  &7None."); return; }
                plot.getTrusted().forEach(uuid -> {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    sendRaw(s, "  &7- &f" + (name != null ? name : uuid.toString()));
                });
            }
            default -> sendRaw(s, "&cUse: add, remove, list");
        }
    }

    // ── /plot info [name] ─────────────────────────────────────────────────────
    private void cmdInfo(CommandSender s, String[] args) {
        Plot plot;
        if (args.length >= 2) {
            plot = pm.getPlot(args[1]);
            if (plot == null) { msg(s, "plot-not-found", "{name}", args[1]); return; }
        } else {
            Player p = requirePlayer(s); if (p == null) return;
            plot = pm.getPlotAt(p.getLocation());
            if (plot == null) { sendRaw(s, "&cYou are not standing in a plot!"); return; }
        }
        sendRaw(s, "&8&m─────────────────────────");
        sendRaw(s, " &6Plot: &e" + plot.getName());
        sendRaw(s, " &7World: &f" + plot.getWorldName());
        sendRaw(s, " &7Corners: &f(" + plot.getX1() + "," + plot.getY1() + "," + plot.getZ1() + ") - (" + plot.getX2() + "," + plot.getY2() + "," + plot.getZ2() + ")");
        sendRaw(s, " &7Status: " + (plot.isRented() ? "&cRented" : "&aAvailable"));
        if (plot.isRented()) {
            sendRaw(s, " &7Owner: &f" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Unknown"));
            sendRaw(s, " &7Time left: &f" + plot.getTimeLeftFormatted());
        }
        sendRaw(s, " &7Price: &f" + (int) plot.getRentPrice() + " &7/ &f" + Plot.formatSeconds(plot.getRentDuration()));
        sendRaw(s, " &7Trusted: &f" + plot.getTrusted().size() + "&7/&f" + plugin.getConfig().getInt("settings.max-trusted", 4));
        sendRaw(s, "&8&m─────────────────────────");
    }

    // ── /plot menu ────────────────────────────────────────────────────────────
    private void cmdMenu(CommandSender s) {
        Player p = requirePlayer(s); if (p == null) return;
        if (!p.hasPermission("jplots.use")) { msg(s, "no-permission"); return; }
        new PlotMenuGUI(plugin, p).open();
    }

    // ── /plot reload ──────────────────────────────────────────────────────────
    private void cmdReload(CommandSender s) {
        if (!s.hasPermission("jplots.reload")) { msg(s, "no-permission"); return; }
        plugin.reload();
        msg(s, "config-reloaded");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Player requirePlayer(CommandSender s) {
        if (s instanceof Player p) return p;
        msg(s, "must-be-player"); return null;
    }

    private void msg(CommandSender s, String key, String... kv) {
        MessageUtils.send(s, key, kv);
    }

    private void sendRaw(CommandSender s, String text) {
        MessageUtils.sendRaw(s, text);
    }

    // ── Tab Completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("help","wand","create","delete","time","evict","sign",
            "setprice","currencyset","currencyget","bypass","trust","info","menu","reload"), args[0]);
        if (args.length == 2) switch (args[0].toLowerCase()) {
            case "delete","evict","sign","setprice","info" -> { return filterPlots(args[1]); }
            case "time"  -> { return filter(List.of("set","add","remove"), args[1]); }
            case "trust" -> { return filter(List.of("add","remove","list"), args[1]); }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("time"))  return filterPlots(args[2]);
            if (args[0].equalsIgnoreCase("trust")) return filterPlayers(args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("time")) return List.of("1d","12h","1h","30m","1d12h");
        return Collections.emptyList();
    }

    private List<String> filterPlots(String prefix) {
        List<String> list = new ArrayList<>();
        pm.getAllPlots().forEach(p -> list.add(p.getName()));
        return filter(list, prefix);
    }

    private List<String> filterPlayers(String prefix) {
        List<String> list = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
        return filter(list, prefix);
    }

    private List<String> filter(List<String> src, String prefix) {
        List<String> result = new ArrayList<>();
        src.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).forEach(result::add);
        return result;
    }
}
