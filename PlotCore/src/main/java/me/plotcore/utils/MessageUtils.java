package me.plotcore.utils;

import me.plotcore.PlotCore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    public static String c(String s) {
        return s == null ? "" : s.replace("&", "\u00a7");
    }

    public static void send(CommandSender sender, String key, String... replacements) {
        String prefix = PlotCore.getInstance().getConfig().getString("messages.prefix", "&8[&aPlotCore&8] &r");
        String msg = PlotCore.getInstance().getConfig().getString("messages." + key, "&cMissing: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i+1]);
        }
        sender.sendMessage(c(prefix + msg));
    }

    public static void sendRaw(CommandSender sender, String text) {
        sender.sendMessage(c(text));
    }

    public static void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(c("&8[&aPlotCore&8] &r" + msg));
    }

    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return -1;
        try { return Long.parseLong(input); } catch (NumberFormatException ignored) {}
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char ch : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(ch)) { num.append(ch); }
            else {
                if (num.length() == 0) return -1;
                long n = Long.parseLong(num.toString()); num.setLength(0);
                long multiplier = switch (ch) {
                    case 'd' -> 86400L;
                    case 'h' -> 3600L;
                    case 'm' -> 60L;
                    case 's' -> 1L;
                    default  -> -1L;
                };
                if (multiplier == -1) return -1;
                total += n * multiplier;
            }
        }
        if (num.length() > 0) total += Long.parseLong(num.toString());
        return total <= 0 ? -1 : total;
    }
}
