package uta.cse3310;

import org.junit.Test;
import static org.junit.Assert.*;

public class PostTest {

    // ── Constructor ───────────────────────────────────────────────────────

    @Test
    public void testConstructorSetsPostId() {
        Post post = new Post(5, "alice", "Hello");
        assertEquals(5, post.postId);
    }

    @Test
    public void testConstructorSetsUsername() {
        Post post = new Post(1, "alice", "Hello");
        assertEquals("alice", post.username);
    }

    @Test
    public void testConstructorSetsMsg() {
        Post post = new Post(1, "alice", "Hello World");
        assertEquals("Hello World", post.msg);
    }

    @Test
    public void testInitialLikesZero() {
        Post post = new Post(1, "alice", "Hello");
        assertEquals(0, post.likes);
    }

    @Test
    public void testInitialRepliesEmpty() {
        Post post = new Post(1, "alice", "Hello");
        assertNotNull(post.replies);
        assertTrue(post.replies.isEmpty());
    }

    @Test
    public void testInitialLikedByEmpty() {
        Post post = new Post(1, "alice", "Hello");
        assertNotNull(post.likedBy);
        assertTrue(post.likedBy.isEmpty());
    }

    @Test
    public void testInitialLikeHiddenFromEmpty() {
        Post post = new Post(1, "alice", "Hello");
        assertNotNull(post.likeHiddenFrom);
        assertTrue(post.likeHiddenFrom.isEmpty());
    }

    // ── Likes ─────────────────────────────────────────────────────────────

    @Test
    public void testAddLike() {
        Post post = new Post(1, "alice", "Hello");
        post.likedBy.add("bob");
        post.likes++;
        assertEquals(1, post.likes);
        assertTrue(post.likedBy.contains("bob"));
    }

    @Test
    public void testDuplicateLikeNotAdded() {
        Post post = new Post(1, "alice", "Hello");
        post.likedBy.add("bob");
        boolean added = post.likedBy.add("bob");
        assertFalse(added);
        assertEquals(1, post.likedBy.size());
    }

    @Test
    public void testMultipleLikers() {
        Post post = new Post(1, "alice", "Hello");
        post.likedBy.add("bob");
        post.likedBy.add("charlie");
        post.likes = 2;
        assertEquals(2, post.likes);
        assertEquals(2, post.likedBy.size());
    }

    @Test
    public void testRemoveLike() {
        Post post = new Post(1, "alice", "Hello");
        post.likedBy.add("bob");
        post.likes = 1;
        post.likedBy.remove("bob");
        post.likes = Math.max(0, post.likes - 1);
        assertEquals(0, post.likes);
        assertFalse(post.likedBy.contains("bob"));
    }

    @Test
    public void testLikeHiddenFromTracksUser() {
        Post post = new Post(1, "alice", "Hello");
        java.util.Set<String> hidden = new java.util.HashSet<>();
        hidden.add("charlie");
        post.likeHiddenFrom.put("bob", hidden);
        assertTrue(post.likeHiddenFrom.get("bob").contains("charlie"));
    }

    // ── Replies ───────────────────────────────────────────────────────────

    @Test
    public void testAddReply() {
        Post post = new Post(1, "alice", "Hello");
        Post.Reply reply = new Post.Reply("bob", "Nice post!");
        post.replies.add(reply);
        assertEquals(1, post.replies.size());
    }

    @Test
    public void testReplyContent() {
        Post post = new Post(1, "alice", "Hello");
        Post.Reply reply = new Post.Reply("bob", "Great!");
        post.replies.add(reply);
        assertEquals("bob", post.replies.get(0).username);
        assertEquals("Great!", post.replies.get(0).msg);
    }

    @Test
    public void testMultipleReplies() {
        Post post = new Post(1, "alice", "Hello");
        post.replies.add(new Post.Reply("bob", "Reply 1"));
        post.replies.add(new Post.Reply("charlie", "Reply 2"));
        assertEquals(2, post.replies.size());
    }

    @Test
    public void testReplyOrderPreserved() {
        Post post = new Post(1, "alice", "Hello");
        post.replies.add(new Post.Reply("bob", "First"));
        post.replies.add(new Post.Reply("charlie", "Second"));
        assertEquals("First", post.replies.get(0).msg);
        assertEquals("Second", post.replies.get(1).msg);
    }

    // ── Post.Reply ────────────────────────────────────────────────────────

    @Test
    public void testReplyConstructorSetsUsername() {
        Post.Reply reply = new Post.Reply("charlie", "Hello");
        assertEquals("charlie", reply.username);
    }

    @Test
    public void testReplyConstructorSetsMsg() {
        Post.Reply reply = new Post.Reply("charlie", "Hello");
        assertEquals("Hello", reply.msg);
    }

    @Test
    public void testReplyHiddenFromInitiallyEmpty() {
        Post.Reply reply = new Post.Reply("bob", "Hello");
        assertNotNull(reply.hiddenFrom);
        assertTrue(reply.hiddenFrom.isEmpty());
    }

    @Test
    public void testReplyHiddenFromTracksUser() {
        Post.Reply reply = new Post.Reply("bob", "Hello");
        reply.hiddenFrom.add("charlie");
        assertTrue(reply.hiddenFrom.contains("charlie"));
        assertFalse(reply.hiddenFrom.contains("dave"));
    }

    @Test
    public void testReplyHiddenFromMultipleUsers() {
        Post.Reply reply = new Post.Reply("bob", "Hello");
        reply.hiddenFrom.add("charlie");
        reply.hiddenFrom.add("dave");
        assertEquals(2, reply.hiddenFrom.size());
    }
}
