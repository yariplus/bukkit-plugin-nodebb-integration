package com.radiofreederp.nodebbintegration;

import com.radiofreederp.nodebbintegration.utils.Helpers;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Yari on 4/17/2016.
 */
public abstract class MinecraftServerCommon implements IMinecraftServerCommon {

    // Base methods are implemented by the actual server.
    abstract public void sendMessage(Object receiver, String message);
    abstract public void sendConsoleMessage(String message);

    abstract public String translateColors(String string);
    abstract public String removeColors(String string);

    // TODO: Replace parameters with a message interface.
    // Send single message.
    @Override
    public final void sendMessage(Object receiver, String message, HashMap<String, String> vars) {
        message = Helpers.replaceMap(message, vars);
        sendMessage(receiver, message);
    }
    @Override
    public final void sendConsoleMessage(String message, HashMap<String, String> vars) {
        message = Helpers.replaceMap(message, vars);
        sendConsoleMessage(message);
    }
    @Override
    public final void sendMessageToOps(String message, HashMap<String, String> vars) {
        message = Helpers.replaceMap(message, vars);
        sendMessageToOps(message);
    }

    // Send message list.
    @Override
    public final void sendMessage(final Object receiver, List<String> messages) {
        for (String message : messages) {
            sendMessage(receiver, message);
        }
    }
    @Override
    public final void sendMessage(final Object receiver, List<String> messages, final HashMap<String, String> vars) {
        for (String message : messages) {
            sendMessage(receiver, message, vars);
        }
    }
    @Override
    public final void sendConsoleMessage(List<String> messages) {
        for (String message : messages) {
            sendConsoleMessage(message);
        }
    }
    @Override
    public final void sendConsoleMessage(List<String> messages, final HashMap<String, String> vars) {
        for (String message : messages) {
            sendConsoleMessage(message, vars);
        }
    }
    @Override
    public final void sendMessageToOps(List<String> messages) {
        for (String message : messages) {
            sendMessageToOps(message);
        }
    }
    @Override
    public final void sendMessageToOps(List<String> messages, final HashMap<String, String> vars) {
        for (String message : messages) {
            sendMessageToOps(message, vars);
        }
    }
}
