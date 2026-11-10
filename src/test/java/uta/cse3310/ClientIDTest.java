package uta.cse3310;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;

public class ClientIDTest {

    // Reset the static counter before each test via reflection so tests are independent
    private void resetCounter(int value) throws Exception {
        Field counterField = ClientID.class.getDeclaredField("counter");
        counterField.setAccessible(true);
        counterField.set(null, value);

        Field usersField = ClientID.class.getDeclaredField("users");
        usersField.setAccessible(true);
        java.util.HashMap<?, ?> map = (java.util.HashMap<?, ?>) usersField.get(null);
        map.clear();
    }

    private int getId(ClientID client) throws Exception {
        Field idField = ClientID.class.getDeclaredField("id");
        idField.setAccessible(true);
        return (int) idField.get(client);
    }

    private String getUsername(ClientID client) throws Exception {
        Field nameField = ClientID.class.getDeclaredField("username");
        nameField.setAccessible(true);
        return (String) nameField.get(client);
    }

    // ── Constructor ───────────────────────────────────────────────────────

    @Test
    public void testConstructor_assignsUsername() throws Exception {
        resetCounter(1);
        ClientID client = new ClientID("alice");
        assertEquals("alice", getUsername(client));
    }

    @Test
    public void testConstructor_assignsId() throws Exception {
        resetCounter(1);
        ClientID client = new ClientID("alice");
        assertEquals(1, getId(client));
    }

    @Test
    public void testConstructor_incrementsCounter() throws Exception {
        resetCounter(1);
        ClientID c1 = new ClientID("alice");
        ClientID c2 = new ClientID("bob");
        assertEquals(1, getId(c1));
        assertEquals(2, getId(c2));
    }

    @Test
    public void testConstructor_threeUsersGetDistinctIds() throws Exception {
        resetCounter(1);
        ClientID c1 = new ClientID("alice");
        ClientID c2 = new ClientID("bob");
        ClientID c3 = new ClientID("charlie");
        assertNotEquals(getId(c1), getId(c2));
        assertNotEquals(getId(c2), getId(c3));
        assertNotEquals(getId(c1), getId(c3));
    }

    @Test
    public void testConstructor_counterStartsAtSpecifiedValue() throws Exception {
        resetCounter(10);
        ClientID client = new ClientID("dave");
        assertEquals(10, getId(client));
    }

    // ── displayID ─────────────────────────────────────────────────────────

    @Test
    public void testDisplayID_doesNotThrow() throws Exception {
        resetCounter(1);
        ClientID client = new ClientID("alice");
        try {
            client.displayID();
        } catch (Exception e) {
            fail("displayID should not throw: " + e.getMessage());
        }
    }
}
