package uta.cse3310;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class ClientSessionTest {

    @Test
    public void testGetPrevMessages_returnsNonNullList() {
        ClientSession session = new ClientSession();
        List<String> messages = session.getPrevMessages();
        assertNotNull(messages);
    }

    @Test
    public void testSaveAndRetrieveMessage() {
        ClientSession session = new ClientSession();
        String unique = "{\"msgType\":\"sendToAllMsg\",\"msg\":\"unit_test_" + System.nanoTime() + "\"}";
        int before = session.getPrevMessages().size();
        session.saveMessage(unique);
        List<String> after = session.getPrevMessages();
        assertTrue(after.size() > before);
        assertTrue(after.contains(unique));
    }

    @Test
    public void testSaveMultipleMessages() {
        ClientSession session = new ClientSession();
        long ts = System.nanoTime();
        String msg1 = "{\"msg\":\"test_a_" + ts + "\"}";
        String msg2 = "{\"msg\":\"test_b_" + ts + "\"}";
        int before = session.getPrevMessages().size();
        session.saveMessage(msg1);
        session.saveMessage(msg2);
        List<String> after = session.getPrevMessages();
        assertEquals(before + 2, after.size());
        assertTrue(after.contains(msg1));
        assertTrue(after.contains(msg2));
    }

    @Test
    public void testMessagesPreserveOrder() {
        ClientSession session = new ClientSession();
        long ts = System.nanoTime();
        String first  = "{\"msg\":\"first_"  + ts + "\"}";
        String second = "{\"msg\":\"second_" + ts + "\"}";
        session.saveMessage(first);
        session.saveMessage(second);
        List<String> messages = session.getPrevMessages();
        int i1 = messages.indexOf(first);
        int i2 = messages.indexOf(second);
        assertTrue(i1 >= 0 && i2 >= 0);
        assertTrue(i1 < i2);
    }

    @Test
    public void testGetPrevMessages_persistsAcrossInstances() {
        long ts = System.nanoTime();
        String unique = "{\"msg\":\"persist_" + ts + "\"}";
        new ClientSession().saveMessage(unique);
        List<String> messages = new ClientSession().getPrevMessages();
        assertTrue(messages.contains(unique));
    }
}
