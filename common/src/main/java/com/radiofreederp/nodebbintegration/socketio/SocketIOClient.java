package com.radiofreederp.nodebbintegration.socketio;

import com.radiofreederp.nodebbintegration.NodeBBIntegrationPlugin;
import com.radiofreederp.nodebbintegration.configuration.PluginConfig;
import com.radiofreederp.nodebbintegration.tasks.TaskPing;
import com.radiofreederp.nodebbintegration.tasks.TaskStatus;
import com.radiofreederp.nodebbintegration.utils.Logger;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Level;

public final class SocketIOClient {

    // Singleton, for now.
    private static SocketIOClient instance;

    private Socket socket;
    private NodeBBIntegrationPlugin plugin;
    private String live;
    private List<String> transports;
    private String cookie;
    private String url;

    private static boolean hasSocket() {
        return instance != null && instance.socket != null;
    }

    public static void connect() { instance.connectSocket(); }
    public static boolean connected() { return hasSocket() && instance.socket.connected(); }
    public static boolean disconnected() { return !connected(); }
    public static void close() {
        if (hasSocket()) {
            try {
                instance.socket.close();
            } catch (Exception ignored) {} // TODO: Debug socket close errors.
        }
    }

    public static void emit(final String event, final JSONObject data, final Ack ack) {
        try {
            data.put("key", PluginConfig.instance.getForumAPIKey());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (connected()) {
            Logger.log("Sending socket request: " + PluginConfig.instance.getSocketNamespace() + "." + event);

            instance.socket.emit(PluginConfig.instance.getSocketNamespace() + "." + event, data, new Ack() {
                @Override
                public void call(Object... args) {
                    if (args[0] == null) {
                        Logger.log(PluginConfig.instance.getSocketNamespace() + "." + event + " callback received without error.");
                    } else {
                        // TODO: This throws with an invalid event name.
                        try {
                            Logger.error(PluginConfig.instance.getSocketNamespace() + "." + event + " callback received with error: " + ((JSONObject)args[0]).getString("message"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (ack != null) ack.call(args);
                }
            });
        }
    }

    // Create instance during plugin load.
    public static SocketIOClient create(NodeBBIntegrationPlugin plugin) {
        if (instance == null) instance = new SocketIOClient(plugin);

        return instance;
    }

    // Initial connection when created.
    private SocketIOClient(NodeBBIntegrationPlugin _plugin) {
        plugin = _plugin;

        connectSocket();
    }

    // Setup Socket Events
    private void setupSocket() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.log("Connected to the forum.");
                plugin.getMinecraftServer().sendMessageToOps("Connected to the forum.");
                plugin.runTask(TaskStatus.getTask());
                plugin.runTask(TaskPing.getTask());
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.error("Lost connection to the forum.");
                plugin.getMinecraftServer().sendMessageToOps("Lost connection to the forum.");
                if (args[0] != null) Logger.error(args[0].toString());
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Logger.error("Error connecting to the forum.");
                if (args[0] != null) Logger.error(args[0].toString());
            }
        });

        socket.on("eventWebChat", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                plugin.eventWebChat(args[0]);
            }
        });
        socket.on("eventGetPlayerVotes", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                plugin.eventGetPlayerVotes(args[0]);
            }
        });
    }

    // Disconnect the socket and reconnect asynchronously.
    private void connectSocket() {
        Logger.log("Reconnecting socket...");

        // TODO: Research. This thread may not be required. SIO and OKHttp may run on their own threads already.
        plugin.runTaskAsynchronously(new Thread() {
            @Override
            public void run() {
                try {
                    // Close previous sockets, and get the forum url and namespace.
                    if (socket != null && socket.connected()) socket.close();

                    // Get config.
                    live = plugin.getMinecraftServer().removeColors(PluginConfig.instance.getSocketAddress());
                    transports = PluginConfig.instance.getSocketTransports();
                    url = plugin.getMinecraftServer().removeColors(PluginConfig.instance.getForumURL());

                    // Get a session cookie.
                    getCookie(url);
                    if (cookie == null) return;

                    // Set SocketIO options.
                    IO.Options options = new IO.Options();
                    options.transports = transports.toArray(new String[transports.size()]);

                    // Create a new socket.
                    socket = IO.socket(live, options);

                    // Send the session cookie with requests.
                    socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Transport transport = (Transport) args[0];

                            transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                                @Override
                                public void call(Object... args2) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, List<String>> headers = (Map<String, List<String>>) args2[0];
                                    headers.put("Cookie", Arrays.asList(cookie));
                                    headers.put("referer", Arrays.asList(url));
                                }
                            });
                        }
                    });

                    // Setup events and such.
                    setupSocket();

                    // Connect to the forum.
                    socket.connect();

                } catch (URISyntaxException e) {
                    Logger.error("Socket The forum URL may be incorrectly formatted.");
                    e.printStackTrace();
                } catch (IOException e) {
                    Logger.error("The forum URL may be invalid.");
                    e.printStackTrace();
                }
            }
        });
    }

    // Get the express session cookie.
    private void getCookie(String _url) throws IOException {
        Logger.info("Getting Session Cookie from: " + _url);

        URL url = new URL(_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.connect();
            cookie = connection.getHeaderField("Set-Cookie");
            int response = connection.getResponseCode();

            if (response/100 == 4) {
                Logger.error("Forum returned a " + response + " Forbidden error, you may need to whitelist your server's IP address on your forum's firewall or cloudflare proxy.");
                cookie = null;
            }
            if (response/100 == 3) {
                Logger.error("Forum returned a " + response + " Redirect error, please use the actual forum address.");
                cookie = null;
            }

            Logger.info("Got Cookie: " + cookie);
        } catch (SSLHandshakeException e) {
            Logger.error("Failed to verify SSL certificates from your forum, you may need to manually add these to your Java trust store.");
            if (PluginConfig.getDebug()) e.printStackTrace();
            cookie = null;
        } catch (UnknownHostException e) {
            Logger.error("Can't connect to forum at " + _url);
            Logger.error("Use `/nodebb url URL` to set the forum address.");
            Logger.error("Use `/nodebb key KEY` to set the forum key.");
			plugin.getMinecraftServer().sendMessageToOps("Can't connect to forum at " + _url);
			plugin.getMinecraftServer().sendMessageToOps("Use `/nodebb url URL` to set the forum address.");
			plugin.getMinecraftServer().sendMessageToOps("Use `/nodebb key KEY` to set the forum key.");
            cookie = null;
        }
    }
}
