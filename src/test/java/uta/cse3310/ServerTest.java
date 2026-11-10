package uta.cse3310;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ServerTest {

    private Server server;
    private FakeWebSocket conn;
    private Gson gson;

    static class FakeWebSocket implements WebSocket {
        List<String> sentMessages = new ArrayList<>();
        private final InetSocketAddress addr;

        public FakeWebSocket(int port) { addr = new InetSocketAddress("127.0.0.1", port); }
        @Override public void send(String text) { sentMessages.add(text); }
        @Override public void send(byte[] bytes) {}
        @Override public void send(ByteBuffer bytes) {}
        @Override public void close() {}
        @Override public void close(int code) {}
        @Override public void close(int code, String message) {}
        @Override public void closeConnection(int code, String message) {}
        @Override public boolean isOpen() { return true; }
        @Override public boolean isClosing() { return false; }
        @Override public boolean isFlushAndClose() { return false; }
        @Override public boolean isClosed() { return false; }
        @Override public InetSocketAddress getRemoteSocketAddress() { return addr; }
        @Override public InetSocketAddress getLocalSocketAddress() { return null; }
        @Override public boolean hasBufferedData() { return false; }
        @Override public IProtocol getProtocol() { return null; }
        @Override public Draft getDraft() { return null; }
        @Override public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {}
        @Override public ReadyState getReadyState() { return ReadyState.OPEN; }
        @Override public <T> void setAttachment(T attachment) {}
        @Override public <T> T getAttachment() { return null; }
        @Override public void sendFrame(Framedata framedata) {}
        @Override public void sendFrame(Collection<Framedata> frames) {}
        @Override public String getResourceDescriptor() { return null; }
        @Override public boolean hasSSLSupport() { return false; }
        @Override public javax.net.ssl.SSLSession getSSLSession() { return null; }
        @Override public void sendPing() {}
    }

    @Before
    public void setUp() {
        server = new Server(9999);
        conn = new FakeWebSocket(8080);
        gson = new Gson();
    }

    // ── processMessage ────────────────────────────────────────────────────

    @Test
    public void testProcessMessage_nameMsg_returnsJoinMessage() {
        String result = server.processMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Alice\"}");
        assertTrue(result.contains("Alice has joined"));
    }

    @Test
    public void testProcessMessage_nameMsg_sendsDisplayMsgToConn() {
        server.processMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Alice\"}");
        assertEquals(1, conn.sentMessages.size());
        JsonObject json = gson.fromJson(conn.sentMessages.get(0), JsonObject.class);
        assertTrue(json.get("displayMain").getAsBoolean());
        assertFalse(json.get("displayName").getAsBoolean());
        assertTrue(json.get("displaySend").getAsBoolean());
    }

    @Test
    public void testProcessMessage_sendMsg_returnsMessageContent() {
        String result = server.processMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Hello\"}");
        assertTrue(result.contains("Hello"));
    }

    @Test
    public void testProcessMessage_sendMsg_returnsJsonWithSendToAllMsgType() {
        String result = server.processMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Hi\"}");
        JsonObject json = gson.fromJson(result, JsonObject.class);
        assertEquals("sendToAllMsg", json.get("msgType").getAsString());
    }

    @Test
    public void testProcessMessage_unknownMsgType_returnsError() {
        String result = server.processMessage(conn, "{\"msgType\":\"unknownMsg\"}");
        assertTrue(result.contains("Error"));
    }

    @Test
    public void testProcessMessage_noMsgType_returnsFormatError() {
        String result = server.processMessage(conn, "{\"data\":\"bad\"}");
        assertTrue(result.contains("Error"));
    }

    @Test
    public void testProcessMessage_returnsJsonString() {
        String result = server.processMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"test\"}");
        assertNotNull(result);
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
    }

    // ── broadcastToAll ────────────────────────────────────────────────────

    @Test
    public void testBroadcastToAll_sendsToAllConnectedClients() {
        FakeWebSocket c1 = new FakeWebSocket(9001);
        FakeWebSocket c2 = new FakeWebSocket(9002);
        server.getConnectedClients().add(c1);
        server.getConnectedClients().add(c2);

        server.broadcastToAll("hello");

        assertTrue(c1.sentMessages.contains("hello"));
        assertTrue(c2.sentMessages.contains("hello"));
    }

    @Test
    public void testBroadcastToAll_emptyClientList_doesNotThrow() {
        try {
            server.broadcastToAll("hello");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    public void testBroadcastToAll_singleClient_receivesMessage() {
        FakeWebSocket c1 = new FakeWebSocket(9001);
        server.getConnectedClients().add(c1);

        server.broadcastToAll("ping");
        assertTrue(c1.sentMessages.contains("ping"));
    }

    // ── getConnectedClients ───────────────────────────────────────────────

    @Test
    public void testGetConnectedClients_initiallyEmpty() {
        assertTrue(server.getConnectedClients().isEmpty());
    }

    @Test
    public void testGetConnectedClients_returnsAddedClients() {
        FakeWebSocket c1 = new FakeWebSocket(9001);
        server.getConnectedClients().add(c1);
        assertTrue(server.getConnectedClients().contains(c1));
    }
}
