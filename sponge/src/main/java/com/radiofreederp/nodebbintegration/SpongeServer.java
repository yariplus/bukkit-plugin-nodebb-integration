package com.radiofreederp.nodebbintegration;

import com.google.common.io.Files;
import com.radiofreederp.nodebbintegration.utils.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

public class SpongeServer extends MinecraftServerCommon {

    private final NodeBBIntegrationPlugin plugin;
    public SpongeServer(NodeBBIntegrationPlugin plugin) {
        this.plugin = plugin;
    }

    // Handle messaging.
    @Override
    public void sendMessage(Object receiver, String message) {
        ((MessageReceiver)receiver).sendMessage(TextSerializers.FORMATTING_CODE.deserialize(message));
    }
    @Override
    public void sendConsoleMessage(String message) {
        Logger.log(removeColors(message));
    }
    @Override
    public void sendMessageToOps(String message) {
        Sponge.getServer().getOnlinePlayers().stream().filter(player->player.hasPermission("nodebb.admin")).forEach(op->sendMessage(op, message));
    }

    // Handle colors. No translation necessary for sponge.
    @Override
    public String translateColors(String string) {
        return string;
    }

    @Override
    public String removeColors(String string) {
        return TextSerializers.FORMATTING_CODE.deserialize(string).toPlain();
    }

    @Override
    public String getTPS() {
        return String.valueOf(Sponge.getServer().getTicksPerSecond());
    }

    @Override
    public ArrayList<JSONObject> getPlayerList() {

        final ArrayList<JSONObject> playerList = new ArrayList<>();

        for (Player player : Sponge.getServer().getOnlinePlayers()) {

            // TODO: Vanish support.

            JSONObject playerObj = new JSONObject();

            try {
                playerObj.put("name", player.getName());
                playerObj.put("id", player.getUniqueId());

                playerList.add(playerObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return playerList;
    }

    @Override
    public ArrayList<JSONObject> getPluginList() {
        final ArrayList<JSONObject> pluginList = new ArrayList<>();

        for (PluginContainer plugin : Sponge.getPluginManager().getPlugins()) {

            JSONObject pluginObj = new JSONObject();

            try {
                pluginObj.put("name", plugin.getName());
                pluginObj.put("version", plugin.getVersion().orElse(""));

                pluginList.add(pluginObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return pluginList;
    }

    @Override
    public JSONObject getPlayerJSON(Object _player) {
        return null;
    }

    @Override
    public String getVersion() {
        // TODO: ???
        return "";
    }

    @Override
    public String getServerName() {
        return Sponge.getServer().getMotd().toString();
    }

    @Override
    public String getServerIcon() {
        String icon = "";

        File file = new File("server-icon.png");
        if (file.isFile()) {
            try {
                icon = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.toByteArray(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return icon;
    }

    @Override
    public String getWorldType() {
        return Sponge.getServer().getDefaultWorld().get().getGeneratorType().toString();
    }

    @Override
    public String getWorldName() {
        return Sponge.getServer().getDefaultWorld().get().getWorldName();
    }

    @Override
    public String getMotd() {
        return Sponge.getServer().getMotd().toString();
    }

    @Override
    public String getPlayerPrefix(Object player) {
        return null;
    }

    @Override
    public int getOnlinePlayers() {
        return Sponge.getServer().getOnlinePlayers().size();
    }

    @Override
    public int getMaxPlayers() {
        return Sponge.getServer().getMaxPlayers();
    }

    @Override
    public JSONObject getGroups() {
        return new JSONObject();
    }

    @Override
    public JSONObject getGroupsWithMembers() {
        return new JSONObject();
    }

    @Override
    public JSONObject getOfflinePlayers() {
        return new JSONObject();
    }

    @Override
    public JSONArray getScoreboards() {
        return null;
    }
}
