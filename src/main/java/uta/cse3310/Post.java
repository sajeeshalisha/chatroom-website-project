package uta.cse3310;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Post {
    public int postId;
    public String username;
    public String msg;
    public int likes;
    public List<Reply> replies;
    public Set<String> likedBy;
    // liker username → set of usernames who should never see this like
    public transient Map<String, Set<String>> likeHiddenFrom = new HashMap<>();

    public Post(int postId, String username, String msg) {
        this.postId = postId;
        this.username = username;
        this.msg = msg;
        this.likes = 0;
        this.replies = new ArrayList<>();
        this.likedBy = new HashSet<>();
    }

    public static class Reply {
        public String username;
        public String msg;
        // users who should never see this reply (blocked at the time it was posted)
        public transient Set<String> hiddenFrom = new HashSet<>();

        public Reply(String username, String msg) {
            this.username = username;
            this.msg = msg;
        }
    }
}