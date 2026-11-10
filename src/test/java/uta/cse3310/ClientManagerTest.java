package uta.cse3310;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

public class ClientManagerTest {

    private ClientManager manager;
    private FakeWebSocket conn1;
    private FakeWebSocket conn2;
    private FakeWebSocket conn3;

    static class FakeWebSocket implements WebSocket {
        private final InetSocketAddress addr;
        public FakeWebSocket(int port) { addr = new InetSocketAddress("127.0.0.1", port); }
        public void send(String text) {}
        public void send(byte[] bytes) {}
        public void send(ByteBuffer bytes) {}
        public void close() {}
        public void close(int code) {}
        public void close(int code, String message) {}
        public void closeConnection(int code, String message) {}
        public boolean isOpen() { return true; }
        public boolean isClosing() { return false; }
        public boolean isFlushAndClose() { return false; }
        public boolean isClosed() { return false; }
        public InetSocketAddress getRemoteSocketAddress() { return addr; }
        public InetSocketAddress getLocalSocketAddress() { return null; }
        public boolean hasBufferedData() { return false; }
        public IProtocol getProtocol() { return null; }
        public Draft getDraft() { return null; }
        public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {}
        public ReadyState getReadyState() { return ReadyState.OPEN; }
        public <T> void setAttachment(T attachment) {}
        public <T> T getAttachment() { return null; }
        public void sendFrame(Framedata framedata) {}
        public void sendFrame(Collection<Framedata> frames) {}
        public String getResourceDescriptor() { return null; }
        public boolean hasSSLSupport() { return false; }
        public javax.net.ssl.SSLSession getSSLSession() { return null; }
        public void sendPing() {}
    }

    @Before
    public void setUp() {
        manager = new ClientManager();
        conn1 = new FakeWebSocket(8001);
        conn2 = new FakeWebSocket(8002);
        conn3 = new FakeWebSocket(8003);
    }

    // ── Add / Remove / Count ──────────────────────────────────────────────

    @Test
    public void testInitialClientCountIsZero() {
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testAddClientIncreasesCount() {
        manager.addClient(conn1);
        assertEquals(1, manager.getClientCount());
    }

    @Test
    public void testAddTwoClientsCount() {
        manager.addClient(conn1);
        manager.addClient(conn2);
        assertEquals(2, manager.getClientCount());
    }

    @Test
    public void testAddNullDoesNotIncreaseCount() {
        manager.addClient(null);
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testRemoveClientDecreasesCount() {
        manager.addClient(conn1);
        manager.removeClient(conn1);
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testRemoveNullDoesNotThrow() {
        manager.removeClient(null);
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testRemoveNonExistentClientDoesNotThrow() {
        manager.removeClient(conn1);
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testRemoveOneOfTwoClients() {
        manager.addClient(conn1);
        manager.addClient(conn2);
        manager.removeClient(conn1);
        assertEquals(1, manager.getClientCount());
    }

    // ── isClientConnected ─────────────────────────────────────────────────

    @Test
    public void testIsClientConnected_true() {
        manager.addClient(conn1);
        assertTrue(manager.isClientConnected(conn1));
    }

    @Test
    public void testIsClientConnected_false() {
        assertFalse(manager.isClientConnected(conn1));
    }

    @Test
    public void testIsClientConnected_afterRemove() {
        manager.addClient(conn1);
        manager.removeClient(conn1);
        assertFalse(manager.isClientConnected(conn1));
    }

    @Test
    public void testIsClientConnectedWithNull() {
        assertFalse(manager.isClientConnected(null));
    }

    @Test
    public void testGetConnectedClientsEmptyByDefault() {
        assertTrue(manager.getConnectedClients().isEmpty());
    }

    @Test
    public void testGetConnectedClientsContainsAdded() {
        manager.addClient(conn1);
        assertTrue(manager.getConnectedClients().contains(conn1));
    }

    // ── Username ──────────────────────────────────────────────────────────

    @Test
    public void testSetAndGetUsername() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        assertEquals("alice", manager.getUsername(conn1));
    }

    @Test
    public void testGetUsernameForUnknownConn() {
        assertNull(manager.getUsername(conn1));
    }

    @Test
    public void testGetUsernameNull() {
        assertNull(manager.getUsername(null));
    }

    @Test
    public void testSetUsernameNullConnDoesNotThrow() {
        manager.setUsername(null, "alice");
    }

    @Test
    public void testSetUsernameNullNameDoesNotThrow() {
        manager.addClient(conn1);
        manager.setUsername(conn1, null);
    }

    @Test
    public void testGetConnectedUsernamesContainsBoth() {
        manager.addClient(conn1);
        manager.addClient(conn2);
        manager.setUsername(conn1, "alice");
        manager.setUsername(conn2, "bob");
        List<String> names = manager.getConnectedUsernames();
        assertTrue(names.contains("alice"));
        assertTrue(names.contains("bob"));
        assertEquals(2, names.size());
    }

    @Test
    public void testGetConnectedUsernamesEmptyByDefault() {
        assertTrue(manager.getConnectedUsernames().isEmpty());
    }

    @Test
    public void testUsernameRemovedWithClient() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        manager.removeClient(conn1);
        assertNull(manager.getUsername(conn1));
        assertFalse(manager.getConnectedUsernames().contains("alice"));
    }

    // ── Block / Unblock ───────────────────────────────────────────────────

    @Test
    public void testBlockPreventsDeliveryToBlocker() {
        manager.blockUser("alice", "bob");
        assertFalse(manager.canReceive("bob", "alice"));
    }

    @Test
    public void testBlockPreventsDeliveryFromBlocker() {
        manager.blockUser("alice", "bob");
        assertFalse(manager.canReceive("alice", "bob"));
    }

    @Test
    public void testUnblockRestoresDeliveryToBlocker() {
        manager.blockUser("alice", "bob");
        manager.unblockUser("alice", "bob");
        assertTrue(manager.canReceive("bob", "alice"));
    }

    @Test
    public void testUnblockRestoresDeliveryFromBlocker() {
        manager.blockUser("alice", "bob");
        manager.unblockUser("alice", "bob");
        assertTrue(manager.canReceive("alice", "bob"));
    }

    @Test
    public void testUnblockNonBlockedUserDoesNotThrow() {
        manager.unblockUser("alice", "bob");
        assertTrue(manager.canReceive("alice", "bob"));
    }

    @Test
    public void testBlockDoesNotAffectThirdParty() {
        manager.blockUser("alice", "bob");
        assertTrue(manager.canReceive("charlie", "alice"));
        assertTrue(manager.canReceive("alice", "charlie"));
    }

    @Test
    public void testMultipleBlocksFromSameUser() {
        manager.blockUser("alice", "bob");
        manager.blockUser("alice", "charlie");
        assertFalse(manager.canReceive("bob", "alice"));
        assertFalse(manager.canReceive("charlie", "alice"));
    }

    @Test
    public void testUnblockOneKeepsOtherBlock() {
        manager.blockUser("alice", "bob");
        manager.blockUser("alice", "charlie");
        manager.unblockUser("alice", "bob");
        assertTrue(manager.canReceive("bob", "alice"));
        assertFalse(manager.canReceive("charlie", "alice"));
    }

    @Test
    public void testBlockPersistsAfterClientDisconnects() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        manager.blockUser("alice", "bob");
        manager.removeClient(conn1);
        assertFalse(manager.canReceive("bob", "alice"));
    }

    // ── canReceive ────────────────────────────────────────────────────────

    @Test
    public void testCanReceive_sameUserAlwaysTrue() {
        assertTrue(manager.canReceive("alice", "alice"));
    }

    @Test
    public void testCanReceive_nullSenderTrue() {
        assertTrue(manager.canReceive(null, "alice"));
    }

    @Test
    public void testCanReceive_nullRecipientTrue() {
        assertTrue(manager.canReceive("alice", null));
    }

    @Test
    public void testCanReceive_bothNullTrue() {
        assertTrue(manager.canReceive(null, null));
    }

    @Test
    public void testCanReceive_noBlocks() {
        assertTrue(manager.canReceive("alice", "bob"));
    }

    @Test
    public void testCanReceive_recipientBlockedSender() {
        manager.blockUser("bob", "alice");
        assertFalse(manager.canReceive("alice", "bob"));
    }

    @Test
    public void testCanReceive_senderBlockedRecipient() {
        manager.blockUser("alice", "bob");
        assertFalse(manager.canReceive("alice", "bob"));
    }

    // ── getClientNames ────────────────────────────────────────────────────

    @Test
    public void testGetClientNames_emptyByDefault() {
        assertTrue(manager.getClientNames().isEmpty());
    }

    @Test
    public void testGetClientNames_containsSetUsername() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        assertEquals("alice", manager.getClientNames().get(conn1));
    }

    @Test
    public void testGetClientNames_reflectsMultipleUsers() {
        manager.addClient(conn1);
        manager.addClient(conn2);
        manager.setUsername(conn1, "alice");
        manager.setUsername(conn2, "bob");
        assertEquals(2, manager.getClientNames().size());
        assertEquals("alice", manager.getClientNames().get(conn1));
        assertEquals("bob", manager.getClientNames().get(conn2));
    }

    @Test
    public void testGetClientNames_removedAfterClear() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        manager.clear();
        assertTrue(manager.getClientNames().isEmpty());
    }

    // ── Clear ─────────────────────────────────────────────────────────────

    @Test
    public void testClearRemovesAllClients() {
        manager.addClient(conn1);
        manager.addClient(conn2);
        manager.clear();
        assertEquals(0, manager.getClientCount());
    }

    @Test
    public void testClearRemovesUsernames() {
        manager.addClient(conn1);
        manager.setUsername(conn1, "alice");
        manager.clear();
        assertNull(manager.getUsername(conn1));
    }

    @Test
    public void testClearRemovesBlockLists() {
        manager.blockUser("alice", "bob");
        manager.clear();
        assertTrue(manager.canReceive("bob", "alice"));
    }

    @Test
    public void testClearOnEmptyManager() {
        manager.clear();
        assertEquals(0, manager.getClientCount());
    }
}
