package com.radiofreederp.nodebbintegration;

import com.radiofreederp.nodebbintegration.bukkit.commands.CommandLinkBukkit;
import com.radiofreederp.nodebbintegration.bukkit.commands.CommandNodeBBBukkit;
import com.radiofreederp.nodebbintegration.bukkit.configuration.PluginConfigBukkit;
import com.radiofreederp.nodebbintegration.bukkit.hooks.OnTimeHook;
import com.radiofreederp.nodebbintegration.bukkit.hooks.VanishNoPacketHook;
import com.radiofreederp.nodebbintegration.bukkit.hooks.VaultHook;
import com.radiofreederp.nodebbintegration.bukkit.hooks.VotifierHook;
import com.radiofreederp.nodebbintegration.bukkit.listeners.*;
import com.radiofreederp.nodebbintegration.socketio.ESocketEvent;
import com.radiofreederp.nodebbintegration.socketio.SocketIOClient;
import com.radiofreederp.nodebbintegration.tasks.TaskPing;
import com.radiofreederp.nodebbintegration.tasks.TaskStatus;
import com.radiofreederp.nodebbintegration.utils.Logger;
import com.radiofreederp.nodebbintegration.utils.NBBPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.logging.Level;

public class NodeBBIntegrationBukkit extends JavaPlugin implements NodeBBIntegrationPlugin {

    private PluginLogger logger;

    private MinecraftServerCommon minecraftServer = new BukkitServer(this);
    @Override
    public MinecraftServerCommon getMinecraftServer() {
        return minecraftServer;
    }

    @Override
    public void log(String message, Level level) {
        logger.log(level, message);
    }

    @Override
    public void runTaskAsynchronously(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(this, task);
    }

    @Override
    public void runTaskTimerAsynchronously(Runnable task, int delay, int interval) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, task, delay, interval);
    }

    @Override
    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }

	@Override
	public void runTaskTimer(Runnable task, int delay, int interval) {
		Bukkit.getScheduler().runTaskTimer(this, task, delay, interval);
	}

    @Override
    public void initTaskTick() {
        new TaskStatus(this);
        new TaskPing(this);
    }

    @Override
    public void eventWebChat(Object... args) {
        // Interpret message.
        if (args[0] != null)
        {
            try
            {
                final String name = ((JSONObject)args[0]).getString("name");
                final String message = ((JSONObject)args[0]).getString("message");
                Bukkit.broadcastMessage("[" + ChatColor.GOLD + "WEB" + ChatColor.RESET + "] <" + name + "> " + message);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void eventGetPlayerVotes(Object... req) {
        Logger.log("Got eventGetPlayerVotes");

        // Interpret message.
        // TODO: Should be an error object.
        if (req[0] == null) return;

        Logger.log("Compiling votes...");
        //JSONObject res = getPluginConfig().getPlayerVotes((JSONObject)req[0]);

        Logger.log("Sending votes...");
        //SocketIOClient.emit("PlayerVotes", res, cb -> {});
    }

    @Override
    public void onEnable() {
        // Set static instance.
        NBBPlugin.instance = this;

        // Start logger.
        logger = new PluginLogger(this);
        Logger.init(this);

        // Loads config and updates if necessary.
		new PluginConfigBukkit();

        // Start the socket client.
        SocketIOClient.create(this);

        // Monitor the TPS.
        initTaskTick();

        // Initialize NBBPlugin Hooks
        VaultHook.hook(this);
        OnTimeHook.hook(this);
        VotifierHook.hook(this);
        VanishNoPacketHook.hook(this);

        // Register listeners.
        getServer().getPluginManager().registerEvents(new ListenerPlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPlayerQuit(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPlayerChat(this), this);
        getServer().getPluginManager().registerEvents(new ListenerServerListPing(this), this);
        getServer().getPluginManager().registerEvents(new ListenerWorldSave(this), this);

        // Register commands.
        this.getCommand("nodebb").setExecutor(new CommandNodeBBBukkit());
        this.getCommand("register").setExecutor(new CommandLinkBukkit()); // TODO
        this.getCommand("link").setExecutor(new CommandLinkBukkit());

        // Sync Players
        new BukkitRunnable(){
            @Override
            public void run() {
                SocketIOClient.emit(ESocketEvent.RANKS_WITH_MEMBERS, minecraftServer.getGroupsWithMembers(), args -> Logger.log("Received " + ESocketEvent.RANKS_WITH_MEMBERS + " callback."));
            }
        }.runTaskLater(this, 100);

        // Turn off debug messages after setup.
        PluginConfigBukkit.setDebug(false);
    }

    @Override
    public void onDisable() {
        try {
            // Try to close the connection gracefully.
            SocketIOClient.close();
        } catch (Exception e) {
            // Any error doesn't matter.
        }
    }
}
