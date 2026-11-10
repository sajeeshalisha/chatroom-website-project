package uta.cse3310;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.framing.Framedata;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.drafts.Draft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MessageHandlerTest {

    private FakeWebSocketServer server;
    private FakeWebSocket conn;
    private MessageHandler handler;
    private Gson gson;

    // ── Fakes ─────────────────────────────────────────────────────────────

    static class FakeWebSocketServer extends WebSocketServer {
        List<String> broadcastMessages = new ArrayList<>();

        public FakeWebSocketServer() {
            super(new InetSocketAddress("localhost", 9051));
        }

        @Override
        public void broadcast(String text) {
            broadcastMessages.add(text);
        }

        @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {}
        @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {}
        @Override public void onMessage(WebSocket conn, String message) {}
        @Override public void onError(WebSocket conn, Exception ex) {}
        @Override public void onStart() {}
    }

    static class FakeWebSocket implements WebSocket {
        List<String> sentMessages = new ArrayList<>();
        private final InetSocketAddress addr;

        public FakeWebSocket(int port) {
            addr = new InetSocketAddress("127.0.0.1", port);
        }
        public FakeWebSocket() { this(8080); }

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

    /** Empty ClientSession that never reads or writes disk. */
    private static ClientSession emptySession() {
        return new ClientSession() {
            @Override public List<String> getPrevMessages() { return new ArrayList<>(); }
            @Override public void saveMessage(String msg) {}
        };
    }

    @Before
    public void setUp() {
        server = new FakeWebSocketServer();
        conn = new FakeWebSocket(8080);
        handler = new MessageHandler(server, null, emptySession());
        gson = new Gson();
    }

    // ── onClientConnected ─────────────────────────────────────────────────

    @Test
    public void testOnClientConnected_sendsTwoMessages() {
        handler.onClientConnected(conn, 5);
        assertEquals(2, conn.sentMessages.size());
    }

    @Test
    public void testOnClientConnected_firstMessageIsIdMsg() {
        handler.onClientConnected(conn, 5);
        assertTrue(conn.sentMessages.get(0).contains("idMsg"));
    }

    @Test
    public void testOnClientConnected_idMsgContainsConnectionId() {
        handler.onClientConnected(conn, 42);
        JsonObject json = gson.fromJson(conn.sentMessages.get(0), JsonObject.class);
        assertEquals(42, json.get("connectionId").getAsInt());
    }

    @Test
    public void testOnClientConnected_secondMessageShowsNameScreen() {
        handler.onClientConnected(conn, 1);
        JsonObject json = gson.fromJson(conn.sentMessages.get(1), JsonObject.class);
        assertTrue(json.get("displayName").getAsBoolean());
        assertFalse(json.get("displayMain").getAsBoolean());
        assertFalse(json.get("displaySend").getAsBoolean());
    }

    // ── nameMsg ───────────────────────────────────────────────────────────

    @Test
    public void testNameMsg_broadcastsJoinMessage() {
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Alice\"}");
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("Alice has joined"));
    }

    @Test
    public void testNameMsg_sendsDisplayMsgShowingMain() {
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Alice\"}");
        assertEquals(1, conn.sentMessages.size());
        JsonObject json = gson.fromJson(conn.sentMessages.get(0), JsonObject.class);
        assertTrue(json.get("displayMain").getAsBoolean());
        assertFalse(json.get("displayName").getAsBoolean());
        assertTrue(json.get("displaySend").getAsBoolean());
    }

    @Test
    public void testNameMsg_emptyName_sendsErrorNotBroadcast() {
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"\"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("empty"));
    }

    @Test
    public void testNameMsg_whitespaceName_sendsError() {
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"   \"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("empty"));
    }

    @Test
    public void testNameMsg_takenUsername_sendsError() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket other = new FakeWebSocket(9001);
        mgr.addClient(other);
        mgr.setUsername(other, "Alice");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Alice\"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("taken"));
    }

    @Test
    public void testNameMsg_withClientManager_setsUsername() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Bob\"}");
        assertEquals("Bob", mgr.getUsername(conn));
    }

    @Test
    public void testNameMsg_withClientManager_broadcastsJoinAndUserList() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"Bob\"}");
        // join message + userList broadcast
        assertEquals(2, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("Bob has joined"));
        assertTrue(server.broadcastMessages.get(1).contains("userListMsg"));
    }

    @Test
    public void testNameMsg_errorHasLoginContext() {
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"\"}");
        assertTrue(conn.sentMessages.get(0).contains("login"));
    }

    // ── sendMsg ───────────────────────────────────────────────────────────

    @Test
    public void testSendMsg_broadcastsMessage() {
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Hello world\"}");
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("Hello world"));
    }

    @Test
    public void testSendMsg_tooLong_sendsError() {
        String longMsg = "a".repeat(257);
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"" + longMsg + "\"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("256"));
    }

    @Test
    public void testSendMsg_exactly256Chars_isAccepted() {
        String exactMsg = "a".repeat(256);
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"" + exactMsg + "\"}");
        assertEquals(1, server.broadcastMessages.size());
    }

    @Test
    public void testSendMsg_tooLong_errorHasPostContext() {
        String longMsg = "a".repeat(257);
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"" + longMsg + "\"}");
        assertTrue(conn.sentMessages.get(0).contains("post"));
    }

    @Test
    public void testSendMsg_withClientManager_sendsToConnectedUsers() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket sender = new FakeWebSocket(9001);
        FakeWebSocket recipient = new FakeWebSocket(9002);
        mgr.addClient(sender);
        mgr.addClient(recipient);
        mgr.setUsername(sender, "Alice");
        mgr.setUsername(recipient, "Bob");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(sender, "{\"msgType\":\"sendMsg\",\"msg\":\"Hi\"}");
        assertTrue(sender.sentMessages.stream().anyMatch(m -> m.contains("Hi")));
        assertTrue(recipient.sentMessages.stream().anyMatch(m -> m.contains("Hi")));
    }

    @Test
    public void testSendMsg_withBlock_doesNotDeliverToBlockedUser() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket sender = new FakeWebSocket(9001);
        FakeWebSocket blocked = new FakeWebSocket(9002);
        mgr.addClient(sender);
        mgr.addClient(blocked);
        mgr.setUsername(sender, "Alice");
        mgr.setUsername(blocked, "Bob");
        mgr.blockUser("Alice", "Bob");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(sender, "{\"msgType\":\"sendMsg\",\"msg\":\"Secret\"}");
        assertFalse(blocked.sentMessages.stream().anyMatch(m -> m.contains("Secret")));
    }

    // ── likeMsg ───────────────────────────────────────────────────────────

    @Test
    public void testLikeMsg_incrementsLikes() {
        // Post a message first so post exists
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Post1\"}");
        server.broadcastMessages.clear();
        // postId 1 should now exist
        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":false}");
        // Should trigger a postUpdateMsg broadcast
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("postUpdateMsg"));
    }

    @Test
    public void testLikeMsg_noDoubleLike() {
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Post1\"}");
        server.broadcastMessages.clear();

        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":false}");
        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":false}");
        // Only 1 broadcast (first like); second like is ignored
        assertEquals(1, server.broadcastMessages.size());
    }

    @Test
    public void testLikeMsg_unlike_decrementsLikes() {
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Post1\"}");
        server.broadcastMessages.clear();

        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":false}");
        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":true}");
        // Both like and unlike should broadcast
        assertEquals(2, server.broadcastMessages.size());
    }

    @Test
    public void testLikeMsg_unlikeNonLiked_doesNotBroadcast() {
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Post1\"}");
        server.broadcastMessages.clear();

        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":true}");
        // User never liked, so unlike should be ignored
        assertEquals(0, server.broadcastMessages.size());
    }

    @Test
    public void testLikeMsg_nonExistentPost_doesNothing() {
        handler.onClientMessage(conn, "{\"msgType\":\"likeMsg\",\"postId\":9999,\"unlike\":false}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(0, conn.sentMessages.size());
    }

    @Test
    public void testLikeMsg_withClientManager_usesUsername() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket poster = new FakeWebSocket(9001);
        mgr.addClient(poster);
        mgr.setUsername(poster, "Alice");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(poster, "{\"msgType\":\"sendMsg\",\"msg\":\"Post\"}");
        server.broadcastMessages.clear();
        poster.sentMessages.clear();

        h.onClientMessage(poster, "{\"msgType\":\"likeMsg\",\"postId\":1,\"unlike\":false}");
        // Alice liked her own post; postUpdateMsg should be sent to Alice
        assertTrue(poster.sentMessages.stream().anyMatch(m -> m.contains("postUpdateMsg")));
    }

    // ── replyMsg ──────────────────────────────────────────────────────────

    @Test
    public void testReplyMsg_addsReplyAndBroadcasts() {
        handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"Post1\"}");
        server.broadcastMessages.clear();

        handler.onClientMessage(conn, "{\"msgType\":\"replyMsg\",\"postId\":1,\"msg\":\"Nice!\"}");
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("postUpdateMsg"));
    }

    @Test
    public void testReplyMsg_nonExistentPost_doesNothing() {
        handler.onClientMessage(conn, "{\"msgType\":\"replyMsg\",\"postId\":9999,\"msg\":\"Hello\"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(0, conn.sentMessages.size());
    }

    @Test
    public void testReplyMsg_withClientManager_replyVisibleToSender() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket poster = new FakeWebSocket(9001);
        FakeWebSocket replier = new FakeWebSocket(9002);
        mgr.addClient(poster);
        mgr.addClient(replier);
        mgr.setUsername(poster, "Alice");
        mgr.setUsername(replier, "Bob");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(poster, "{\"msgType\":\"sendMsg\",\"msg\":\"Post\"}");
        poster.sentMessages.clear();
        replier.sentMessages.clear();

        h.onClientMessage(replier, "{\"msgType\":\"replyMsg\",\"postId\":1,\"msg\":\"Hey!\"}");
        // Both Alice and Bob should receive the postUpdateMsg
        assertTrue(poster.sentMessages.stream().anyMatch(m -> m.contains("postUpdateMsg")));
        assertTrue(replier.sentMessages.stream().anyMatch(m -> m.contains("postUpdateMsg")));
    }

    @Test
    public void testReplyMsg_withBlock_replyHiddenFromBlockedUser() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket poster = new FakeWebSocket(9001);
        FakeWebSocket replier = new FakeWebSocket(9002);
        mgr.addClient(poster);
        mgr.addClient(replier);
        mgr.setUsername(poster, "Alice");
        mgr.setUsername(replier, "Bob");
        mgr.blockUser("Alice", "Bob");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(poster, "{\"msgType\":\"sendMsg\",\"msg\":\"Post\"}");
        poster.sentMessages.clear();
        replier.sentMessages.clear();

        h.onClientMessage(replier, "{\"msgType\":\"replyMsg\",\"postId\":1,\"msg\":\"Reply\"}");
        // Bob is blocked by Alice, so Alice should not receive an update from Bob
        assertFalse(poster.sentMessages.stream().anyMatch(m -> m.contains("postUpdateMsg")));
    }

    // ── blockMsg ──────────────────────────────────────────────────────────

    @Test
    public void testBlockMsg_blocksUser() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket blocker = new FakeWebSocket(9001);
        mgr.addClient(blocker);
        mgr.setUsername(blocker, "Alice");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(blocker, "{\"msgType\":\"blockMsg\",\"targetUsername\":\"Bob\",\"unblock\":false}");
        assertFalse(mgr.canReceive("Bob", "Alice"));
    }

    @Test
    public void testBlockMsg_unblocksUser() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket blocker = new FakeWebSocket(9001);
        mgr.addClient(blocker);
        mgr.setUsername(blocker, "Alice");
        mgr.blockUser("Alice", "Bob");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(blocker, "{\"msgType\":\"blockMsg\",\"targetUsername\":\"Bob\",\"unblock\":true}");
        assertTrue(mgr.canReceive("Bob", "Alice"));
    }

    @Test
    public void testBlockMsg_cannotBlockSelf() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket blocker = new FakeWebSocket(9001);
        mgr.addClient(blocker);
        mgr.setUsername(blocker, "Alice");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(blocker, "{\"msgType\":\"blockMsg\",\"targetUsername\":\"Alice\",\"unblock\":false}");
        // canReceive("Alice","Alice") should still be true since self-block is ignored
        assertTrue(mgr.canReceive("Alice", "Alice"));
    }

    @Test
    public void testBlockMsg_withoutClientManager_doesNotThrow() {
        // handler has null clientManager — should silently do nothing
        handler.onClientMessage(conn, "{\"msgType\":\"blockMsg\",\"targetUsername\":\"Bob\",\"unblock\":false}");
        assertEquals(0, conn.sentMessages.size());
        assertEquals(0, server.broadcastMessages.size());
    }

    // ── onClientDisconnected ──────────────────────────────────────────────

    @Test
    public void testDisconnect_broadcastsLeaveMessage() {
        handler.onClientDisconnected(conn);
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("has left"));
    }

    @Test
    public void testDisconnect_usesUsername_whenAvailable() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        mgr.setUsername(conn, "Charlie");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientDisconnected(conn);
        assertTrue(server.broadcastMessages.get(0).contains("Charlie has left"));
    }

    @Test
    public void testDisconnect_fallsBackToAddress_whenNoUsername() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        // no username set
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientDisconnected(conn);
        assertTrue(server.broadcastMessages.get(0).contains("has left"));
    }

    @Test
    public void testDisconnect_removesClientFromManager() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        mgr.setUsername(conn, "Dave");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientDisconnected(conn);
        assertFalse(mgr.isClientConnected(conn));
        assertNull(mgr.getUsername(conn));
    }

    @Test
    public void testDisconnect_withClientManager_broadcastsLeaveAndUserList() {
        ClientManager mgr = new ClientManager();
        mgr.addClient(conn);
        mgr.setUsername(conn, "Eve");
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientDisconnected(conn);
        assertEquals(2, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("Eve has left"));
        assertTrue(server.broadcastMessages.get(1).contains("userListMsg"));
    }

    // ── System limits ─────────────────────────────────────────────────────

    /** REQ-40: system shall support MIN_CONCURRENT_USERS (10) simultaneous clients */
    @Test
    public void testTenConcurrentClients_allReceiveMessage() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket[] clients = new FakeWebSocket[Validator.MIN_CONCURRENT_USERS];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new FakeWebSocket(9100 + i);
            mgr.addClient(clients[i]);
            mgr.setUsername(clients[i], "user" + i);
        }
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        h.onClientMessage(clients[0], "{\"msgType\":\"sendMsg\",\"msg\":\"broadcast\"}");

        for (int i = 0; i < clients.length; i++) {
            assertTrue("Client " + i + " should receive broadcast",
                clients[i].sentMessages.stream().anyMatch(m -> m.contains("broadcast")));
        }
    }

    /** REQ-40: 10 clients can connect, set usernames, and disconnect without errors */
    @Test
    public void testTenConcurrentClients_connectAndDisconnect() {
        ClientManager mgr = new ClientManager();
        FakeWebSocket[] clients = new FakeWebSocket[Validator.MIN_CONCURRENT_USERS];
        MessageHandler h = new MessageHandler(server, mgr, emptySession());

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new FakeWebSocket(9200 + i);
            mgr.addClient(clients[i]);
            h.onClientMessage(clients[i], "{\"msgType\":\"nameMsg\",\"name\":\"user" + i + "\"}");
        }
        assertEquals(Validator.MIN_CONCURRENT_USERS, mgr.getClientCount());

        for (FakeWebSocket c : clients) {
            h.onClientDisconnected(c);
        }
        assertEquals(0, mgr.getClientCount());
    }

    /** REQ-41: history replay sends at most HISTORY_REPLAY_LIMIT (50) messages */
    @Test
    public void testHistoryReplay_sendsLast50WhenMoreExist() {
        List<String> history = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            history.add("{\"msgType\":\"sendToAllMsg\",\"msg\":\"msg" + i + "\",\"username\":\"alice\",\"postId\":" + i + "}");
        }
        ClientSession session = new ClientSession() {
            @Override public List<String> getPrevMessages() { return history; }
            @Override public void saveMessage(String msg) {}
        };
        MessageHandler h = new MessageHandler(server, null, session);

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"bob\"}");

        // conn gets: 1 displayMsg + 50 history messages (not 60)
        long historyCount = conn.sentMessages.stream()
            .filter(m -> m.contains("sendToAllMsg")).count();
        assertEquals(Validator.HISTORY_REPLAY_LIMIT, historyCount);
    }

    /** REQ-41: history replay sends all messages when fewer than 50 exist */
    @Test
    public void testHistoryReplay_sendsAllWhenFewerThan50() {
        List<String> history = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            history.add("{\"msgType\":\"sendToAllMsg\",\"msg\":\"msg" + i + "\",\"username\":\"alice\",\"postId\":" + i + "}");
        }
        ClientSession session = new ClientSession() {
            @Override public List<String> getPrevMessages() { return history; }
            @Override public void saveMessage(String msg) {}
        };
        MessageHandler h = new MessageHandler(server, null, session);

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"bob\"}");

        long historyCount = conn.sentMessages.stream()
            .filter(m -> m.contains("sendToAllMsg")).count();
        assertEquals(20, historyCount);
    }

    /** REQ-41: history replay deduplicates posts with the same postId */
    @Test
    public void testHistoryReplay_deduplicatesDuplicatePostIds() {
        List<String> history = new ArrayList<>();
        // same postId 1 appears twice (e.g. after a server restart wrote it twice)
        history.add("{\"msgType\":\"sendToAllMsg\",\"msg\":\"original\",\"username\":\"alice\",\"postId\":1}");
        history.add("{\"msgType\":\"sendToAllMsg\",\"msg\":\"duplicate\",\"username\":\"alice\",\"postId\":1}");
        ClientSession session = new ClientSession() {
            @Override public List<String> getPrevMessages() { return history; }
            @Override public void saveMessage(String msg) {}
        };
        MessageHandler h = new MessageHandler(server, null, session);

        h.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"bob\"}");

        long historyCount = conn.sentMessages.stream()
            .filter(m -> m.contains("sendToAllMsg")).count();
        assertEquals(1, historyCount);
    }

    /** REQ-42: username longer than MAX_USERNAME_LENGTH (32) is rejected */
    @Test
    public void testNameMsg_tooLongUsername_sendsError() {
        String longName = "a".repeat(Validator.MAX_USERNAME_LENGTH + 1);
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"" + longName + "\"}");
        assertEquals(0, server.broadcastMessages.size());
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("32"));
    }

    /** REQ-42: username of exactly MAX_USERNAME_LENGTH (32) chars is accepted */
    @Test
    public void testNameMsg_exactly32CharUsername_isAccepted() {
        String maxName = "a".repeat(Validator.MAX_USERNAME_LENGTH);
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"" + maxName + "\"}");
        assertEquals(1, server.broadcastMessages.size());
        assertTrue(server.broadcastMessages.get(0).contains("has joined"));
    }

    /** REQ-42: username-too-long error has login context */
    @Test
    public void testNameMsg_tooLongUsername_errorHasLoginContext() {
        String longName = "a".repeat(Validator.MAX_USERNAME_LENGTH + 1);
        handler.onClientMessage(conn, "{\"msgType\":\"nameMsg\",\"name\":\"" + longName + "\"}");
        assertTrue(conn.sentMessages.get(0).contains("login"));
    }

    /** REQ-43: a burst of 100 messages is processed without loss */
    @Test
    public void testMessageBurst_100MessagesAllBroadcast() {
        int burst = 100;
        for (int i = 0; i < burst; i++) {
            handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"msg" + i + "\"}");
        }
        assertEquals(burst, server.broadcastMessages.size());
    }

    /** REQ-43: burst messages preserve content integrity */
    @Test
    public void testMessageBurst_contentsAreCorrect() {
        for (int i = 0; i < 10; i++) {
            handler.onClientMessage(conn, "{\"msgType\":\"sendMsg\",\"msg\":\"item" + i + "\"}");
        }
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            assertTrue("item" + idx + " should appear in broadcasts",
                server.broadcastMessages.stream().anyMatch(m -> m.contains("item" + idx)));
        }
    }

    // ── Invalid messages ──────────────────────────────────────────────────

    @Test
    public void testInvalidMessage_sendsErrorToClient() {
        handler.onClientMessage(conn, "{\"bad\":\"data\"}");
        assertEquals(1, conn.sentMessages.size());
        assertTrue(conn.sentMessages.get(0).contains("msg incorrect format"));
    }

    @Test
    public void testInvalidMessage_doesNotBroadcast() {
        handler.onClientMessage(conn, "{\"bad\":\"data\"}");
        assertEquals(0, server.broadcastMessages.size());
    }

    @Test
    public void testMalformedJson_doesNotCrash() {
        try {
            handler.onClientMessage(conn, "not json at all");
        } catch (Exception e) {
            fail("Handler should not throw on malformed input: " + e.getMessage());
        }
    }
}
