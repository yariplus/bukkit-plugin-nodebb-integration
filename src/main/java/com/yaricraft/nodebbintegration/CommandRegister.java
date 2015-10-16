package com.yaricraft.nodebbintegration;

import com.github.nkzawa.socketio.client.Ack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Yari on 10/5/2015.
 */
public class CommandRegister implements CommandExecutor {
    private final NodeBBIntegration plugin;

    public CommandRegister(NodeBBIntegration plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Sender needs to be a player.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command needs to be run by a player.");
            return true;
        }

        // If we're not connected, don't do anything.
        if (SocketIOClient.getSocket() == null) {
            sender.sendMessage("Sorry! The server isn't currently connected to the forum.");
            return true;
        }

        // Assert parameters.
        if (args.length != 2) {
            sender.sendMessage("Please use \"/register [email] [password]\"");
            return true;
        }

        try {
            sender.sendMessage("Registering you on " + plugin.getConfig().getString("FORUMNAME") + "...");
        }catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("email", args[0]);
            obj.put("password", args[1]);
            obj.put("name", sender.getName());
            obj.put("id", ((Player) sender).getUniqueId().toString());
            obj.put("key", plugin.getConfig().getString("APIKEY"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Sending " + SocketIOClient.getNamespace() + "commandRegister");

        final CommandSender commandSender = sender;
        final String forum = plugin.getConfig().getString("FORUMNAME");

        SocketIOClient.getSocket().emit(SocketIOClient.getNamespace() + "commandRegister", obj, new Ack() {
            @Override
            public void call(Object... args) {
                if (args[0] == null) {
                    try {
                        JSONObject result = ((JSONObject) args[1]);
                        System.out.println("commandRegister success: " + result.getString("task"));

                        String message = result.getString("task");

                        if (message.equals("REGISTER")) {
                            message = "Success! Registered your account on " + forum;
                        } else if (message.equals("REREGISTER")) {
                            message = "Success! Updated your profile on " + forum;
                        }

                        commandSender.sendMessage(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.out.println("commandRegister error: Failed to parse result.");
                        commandSender.sendMessage("Registration error: Internal parsing error. Please inform an administrator.");
                    }
                }else{
                    try {
                        JSONObject err = ((JSONObject) args[0]);
                        System.out.println("commandRegister error: " + err.getString("message"));

                        String error = err.getString("message");

                        if (error.equals("FAILPASS")) {
                            error = "Invalid Password";
                        }

                        commandSender.sendMessage("Registration error: " + error);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.out.println("commandRegister error: Failed to parse error.");
                        commandSender.sendMessage("Registration error: Internal parsing error. Please inform an administrator.");
                    }
                }
            }
        });

        return true;
    }
}
