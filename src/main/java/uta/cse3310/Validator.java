package uta.cse3310;

import java.util.List;

public class Validator {

    public static final int MAX_USERNAME_LENGTH = 32;
    public static final int MAX_POST_LENGTH = 256;
    public static final int MIN_CONCURRENT_USERS = 10;
    public static final int HISTORY_REPLAY_LIMIT = 50;

    public static boolean isUsernameEmpty(String username) {
        return username == null || username.trim().isEmpty();
    }

    public static boolean isUsernameTooLong(String username) {
        return username != null && username.length() > MAX_USERNAME_LENGTH;
    }

    public static boolean isUsernameTaken(String username, List<String> connectedUsers) {
        return connectedUsers.contains(username);
    }

    public static boolean isPostTooLong(String post) {
        return post != null && post.length() > MAX_POST_LENGTH;
    }
}
