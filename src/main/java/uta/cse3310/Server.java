package uta.cse3310;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Server extends WebSocketServer 
{
    // List of all currently connected clients
    private List<WebSocket> connectedClients;
    private int connectionId = 0;

    public Server(int port) 
    {
        super(new InetSocketAddress(port));
        connectedClients = new ArrayList<>();
    }
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) 
    {
        connectionId++;
        connectedClients.add(conn);
        System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected");

        Gson gson = new Gson();

        // Send client their unique ID
        idMsg id = new idMsg(connectionId);
        conn.send(gson.toJson(id));

        // Show login div first
        displayMsg dm = new displayMsg();
        dm.displayName = true;
        dm.displayMain = false;
        dm.displaySend = false;

        conn.send(gson.toJson(dm));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) 
    {
        connectedClients.remove(conn);
        System.out.println(conn + " has disconnected");
    }
    @Override
    public void onMessage(WebSocket conn, String message) 
    {
        String response = processMessage(conn, message);
        broadcastToAll(response);
    }


    @Override
    public void onError(WebSocket conn, Exception ex) 
    {
        ex.printStackTrace();
    }

    @Override
    public void onStart() 
    {
        System.out.println("Server started");
    }

    // Processes incoming message and returns JSON string to broadcast
    public String processMessage(WebSocket conn, String message) 
    {
        Gson gson = new GsonBuilder().create();
        String reply;

        if (message.contains("msgType")) 
        {
            if (message.contains("nameMsg")) 
            {
                nameMsg N = gson.fromJson(message, nameMsg.class);
                reply = N.name + " has joined";

                // Switch client from login div to main div
                displayMsg dm = new displayMsg();
                dm.displayName = false;
                dm.displayMain = true;
                dm.displaySend = true;
                conn.send(gson.toJson(dm));

            } 
            else if (message.contains("sendMsg")) 
            {
                sendMsg S = gson.fromJson(message, sendMsg.class);
                reply = S.msg;

            } 
            else 
            {
                reply = "Error: unhandled message type - " + message;
            }
        } 
        else
        {
            reply = "Error: incorrect message format - " + message;
        }

        sendToAllMsg outgoing = new sendToAllMsg();
        outgoing.msg = reply;
        return gson.toJson(outgoing);
    }
    // Sends a message to every connected client
    public void broadcastToAll(String message) 
    {
        for (WebSocket client : connectedClients) 
        {
            if (client.isOpen()) 
            {
                client.send(message);
            }
        }
    }
    public List<WebSocket> getConnectedClients() 
    {
        return connectedClients;
    }
}
