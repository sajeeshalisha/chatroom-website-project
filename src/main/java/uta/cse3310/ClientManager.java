package uta.cse3310;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.java_websocket.WebSocket;

public class ClientManager {

    private final Set<WebSocket> connectedClients =
            Collections.newSetFromMap(new ConcurrentHashMap<WebSocket, Boolean>());
    
    private final ConcurrentHashMap<WebSocket, String> clientNames = new ConcurrentHashMap<>();
    // maps each username to the set of usernames they have blocked
    private final ConcurrentHashMap<String, Set<String>> blockLists = new ConcurrentHashMap<>();

    public ClientManager() {
    }

    public void addClient(WebSocket conn) {
        if (conn != null) {
            connectedClients.add(conn);
        }
    }

    public void removeClient(WebSocket conn) {
        if (conn != null) {
            clientNames.remove(conn);
            connectedClients.remove(conn);
        }
    }

    public boolean isClientConnected(WebSocket conn) {
        return conn != null && connectedClients.contains(conn);
    }

    public int getClientCount() {
        return connectedClients.size();
    }

    public Set<WebSocket> getConnectedClients() {
        return Collections.unmodifiableSet(new HashSet<>(connectedClients));
    }

    public void clear() {
        connectedClients.clear();
        clientNames.clear();
        blockLists.clear();
    }

    public void setUsername(WebSocket conn, String name) {
        if (conn != null && name != null) {
            clientNames.put(conn, name);
        }
    }

    public String getUsername(WebSocket conn) {
        return conn != null ? clientNames.getOrDefault(conn, null) : null;
    }

    public List<String> getConnectedUsernames() {
        return new ArrayList<>(clientNames.values());
    }

    public void blockUser(String blocker, String target) {
        blockLists.computeIfAbsent(blocker, k -> ConcurrentHashMap.newKeySet()).add(target);
    }

    public void unblockUser(String blocker, String target) {
        Set<String> list = blockLists.get(blocker);
        if (list != null) {
            list.remove(target);
        }
    }

    // returns true if the message from sender should be delivered to recipient
    public boolean canReceive(String sender, String recipient) {
        if (sender == null || recipient == null || sender.equals(recipient)) return true;
        Set<String> recipientBlocks = blockLists.get(recipient);
        if (recipientBlocks != null && recipientBlocks.contains(sender)) return false;
        Set<String> senderBlocks = blockLists.get(sender);
        if (senderBlocks != null && senderBlocks.contains(recipient)) return false;
        return true;
    }

    public Map<WebSocket, String> getClientNames() {
        return Collections.unmodifiableMap(clientNames);
    }
}

