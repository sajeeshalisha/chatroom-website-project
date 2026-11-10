package uta.cse3310;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class ValidatorTest {

    // ── isUsernameEmpty ───────────────────────────────────────────────────

    @Test
    public void testUsernameEmpty_null() {
        assertTrue(Validator.isUsernameEmpty(null));
    }

    @Test
    public void testUsernameEmpty_emptyString() {
        assertTrue(Validator.isUsernameEmpty(""));
    }

    @Test
    public void testUsernameEmpty_whitespaceOnly() {
        assertTrue(Validator.isUsernameEmpty("   "));
    }

    @Test
    public void testUsernameEmpty_tabOnly() {
        assertTrue(Validator.isUsernameEmpty("\t"));
    }

    @Test
    public void testUsernameEmpty_validUsername() {
        assertFalse(Validator.isUsernameEmpty("alice"));
    }

    @Test
    public void testUsernameEmpty_singleChar() {
        assertFalse(Validator.isUsernameEmpty("a"));
    }

    @Test
    public void testUsernameEmpty_usernameWithSpaces() {
        assertFalse(Validator.isUsernameEmpty("alice bob"));
    }

    // ── isUsernameTaken ───────────────────────────────────────────────────

    @Test
    public void testUsernameTaken_isTaken() {
        assertTrue(Validator.isUsernameTaken("alice", Arrays.asList("alice", "bob")));
    }

    @Test
    public void testUsernameTaken_notTaken() {
        assertFalse(Validator.isUsernameTaken("charlie", Arrays.asList("alice", "bob")));
    }

    @Test
    public void testUsernameTaken_emptyList() {
        assertFalse(Validator.isUsernameTaken("alice", new ArrayList<>()));
    }

    @Test
    public void testUsernameTaken_caseSensitive() {
        assertFalse(Validator.isUsernameTaken("Alice", Arrays.asList("alice")));
    }

    @Test
    public void testUsernameTaken_exactMatch() {
        assertTrue(Validator.isUsernameTaken("bob", Arrays.asList("bob")));
    }

    // ── isPostTooLong ─────────────────────────────────────────────────────

    @Test
    public void testPostTooLong_null() {
        assertFalse(Validator.isPostTooLong(null));
    }

    @Test
    public void testPostTooLong_emptyString() {
        assertFalse(Validator.isPostTooLong(""));
    }

    @Test
    public void testPostTooLong_shortPost() {
        assertFalse(Validator.isPostTooLong("Hello"));
    }

    @Test
    public void testPostTooLong_exactly256Chars() {
        String post = "a".repeat(256);
        assertFalse(Validator.isPostTooLong(post));
    }

    @Test
    public void testPostTooLong_257Chars() {
        String post = "a".repeat(257);
        assertTrue(Validator.isPostTooLong(post));
    }

    @Test
    public void testPostTooLong_veryLong() {
        String post = "a".repeat(1000);
        assertTrue(Validator.isPostTooLong(post));
    }

    // ── isUsernameTooLong ─────────────────────────────────────────────────

    @Test
    public void testUsernameTooLong_null() {
        assertFalse(Validator.isUsernameTooLong(null));
    }

    @Test
    public void testUsernameTooLong_empty() {
        assertFalse(Validator.isUsernameTooLong(""));
    }

    @Test
    public void testUsernameTooLong_normalName() {
        assertFalse(Validator.isUsernameTooLong("alice"));
    }

    @Test
    public void testUsernameTooLong_exactly32Chars() {
        String name = "a".repeat(Validator.MAX_USERNAME_LENGTH);
        assertFalse(Validator.isUsernameTooLong(name));
    }

    @Test
    public void testUsernameTooLong_33Chars() {
        String name = "a".repeat(Validator.MAX_USERNAME_LENGTH + 1);
        assertTrue(Validator.isUsernameTooLong(name));
    }

    @Test
    public void testUsernameTooLong_veryLong() {
        String name = "a".repeat(100);
        assertTrue(Validator.isUsernameTooLong(name));
    }

    // ── Constants ─────────────────────────────────────────────────────────

    @Test
    public void testMaxPostLengthConstant() {
        assertEquals(256, Validator.MAX_POST_LENGTH);
    }

    @Test
    public void testMaxUsernameLengthConstant() {
        assertEquals(32, Validator.MAX_USERNAME_LENGTH);
    }

    @Test
    public void testMinConcurrentUsersConstant() {
        assertEquals(10, Validator.MIN_CONCURRENT_USERS);
    }

    @Test
    public void testHistoryReplayLimitConstant() {
        assertEquals(50, Validator.HISTORY_REPLAY_LIMIT);
    }
}
