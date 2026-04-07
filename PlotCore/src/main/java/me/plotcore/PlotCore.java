package me.plotcore;

import me.plotcore.commands.PlotCommand;
import me.plotcore.listeners.PlayerInteractListener;
import me.plotcore.listeners.BlockProtectListener;
import me.plotcore.listeners.PlayerQuitListener;
import me.plotcore.managers.CurrencyManager;
import me.plotcore.managers.PlotManager;
import me.plotcore.managers.SelectionManager;
import me.plotcore.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class PlotCore extends JavaPlugin {

    private static PlotCore instance;
    private PlotManager plotManager;
    private CurrencyManager currencyManager;
    private SelectionManager selectionManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("plots.yml", false);

        this.currencyManager = new CurrencyManager(this);
        this.selectionManager = new SelectionManager();
        this.plotManager = new PlotManager(this);

        PlotCommand plotCmd = new PlotCommand(this);
        getCommand("plot").setExecutor(plotCmd);
        getCommand("plot").setTabCompleter(plotCmd);

        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        plotManager.startExpiryTask();

        MessageUtils.log("&av2.0.0 enabled. &e" + plotManager.getPlotCount() + " &aplots loaded.");
    }

    @Override
    public void onDisable() {
        if (plotManager != null) plotManager.saveAll();
        MessageUtils.log("&cDisabled. Data saved.");
    }

    public void reload() {
        reloadConfig();
        plotManager.reload();
        currencyManager.reload();
    }

    public static PlotCore getInstance()          { return instance; }
    public PlotManager getPlotManager()         { return plotManager; }
    public CurrencyManager getCurrencyManager() { return currencyManager; }
    public SelectionManager getSelectionManager() { return selectionManager; }
}
