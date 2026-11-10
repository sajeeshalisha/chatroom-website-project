package uta.cse3310;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

public class MessageHandler {

    private final Gson gson = new Gson();
    private final WebSocketServer server;
    private final ClientManager clientManager;
    private final ClientSession clientSession;

    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();
    private final AtomicInteger nextPostId = new AtomicInteger(1);

    public MessageHandler(WebSocketServer server) {
        this.server = server;
        this.clientManager = null;
        this.clientSession = new ClientSession();
    }

    public MessageHandler(WebSocketServer server, ClientManager clientManager) {
        this.server = server;
        this.clientManager = clientManager;
        this.clientSession = new ClientSession();
        loadHistoryPosts();
    }

    public MessageHandler(WebSocketServer server, ClientManager clientManager, ClientSession clientSession) {
        this.server = server;
        this.clientManager = clientManager;
        this.clientSession = clientSession;
    }

    private void loadHistoryPosts() {
        List<String> history = clientSession.getPrevMessages();
        for (String msg : history) {
            try {
                sendToAllMsg m = gson.fromJson(msg, sendToAllMsg.class);
                if (m.username != null && m.postId > 0 && !posts.containsKey(m.postId)) {
                    Post post = new Post(m.postId, m.username, m.msg);
                    posts.put(m.postId, post);
                    if (m.postId >= nextPostId.get()) {
                        nextPostId.set(m.postId + 1);
                    }
                }
            } catch (Exception e) {
                // skip malformed history entries
            }
        }
        System.out.println("Loaded " + posts.size() + " posts from history");
    }

    public void onClientConnected(WebSocket conn, int connectionId) {
        idMsg id = new idMsg(connectionId);
        conn.send(gson.toJson(id));

        displayMsg dm = new displayMsg();
        dm.displayName = true;
        dm.displayMain = false;
        dm.displaySend = false;
        conn.send(gson.toJson(dm));

    }

    public void onClientMessage(WebSocket conn, String message) {
        if (message.contains("nameMsg")) {
            nameMsg N = gson.fromJson(message, nameMsg.class);

            if (Validator.isUsernameEmpty(N.name)) {
                errorMsg err = new errorMsg();
                err.context = "login";
                err.msg = "Username cannot be empty.";
                conn.send(gson.toJson(err));
                return;
            }

            if (Validator.isUsernameTooLong(N.name)) {
                errorMsg err = new errorMsg();
                err.context = "login";
                err.msg = "Username cannot exceed " + Validator.MAX_USERNAME_LENGTH + " characters.";
                conn.send(gson.toJson(err));
                return;
            }

            if (clientManager != null && Validator.isUsernameTaken(N.name, clientManager.getConnectedUsernames())) {
                errorMsg err = new errorMsg();
                err.context = "login";
                err.msg = "Username is already taken.";
                conn.send(gson.toJson(err));
                return;
            }

            displayMsg dm = new displayMsg();
            dm.displayMain = true;
            dm.displayName = false;
            dm.displaySend = true;
            conn.send(gson.toJson(dm));

            sendToAllMsg broadcast = new sendToAllMsg();
            broadcast.msg = N.name + " has joined";
            server.broadcast(gson.toJson(broadcast));

            if (clientManager != null) {
                clientManager.setUsername(conn, N.name);
                broadcastUserList();
            }

            List<String> history = clientSession.getPrevMessages();
            int start = Math.max(0, history.size() - Validator.HISTORY_REPLAY_LIMIT);
            java.util.Set<Integer> sentPostIds = new java.util.HashSet<>();
            for (int i = start; i < history.size(); i++) {
                String line = history.get(i);
                try {
                    sendToAllMsg m = gson.fromJson(line, sendToAllMsg.class);
                    if (m.postId > 0) {
                        if (!sentPostIds.add(m.postId)) continue; // skip duplicate postId
                    }
                } catch (Exception ignored) {}
                conn.send(line);
            }

            for (Post post : posts.values()) {
                if (clientManager == null || clientManager.canReceive(post.username, N.name)) {
                    postUpdateMsg update = buildFilteredUpdate(post, N.name);
                    if (update.likes > 0 || !update.replies.isEmpty()) {
                        conn.send(gson.toJson(update));
                    }
                }
            }

        } else if (message.contains("sendMsg")) {
            sendMsg S = gson.fromJson(message, sendMsg.class);
            String sender = clientManager != null ? clientManager.getUsername(conn) : S.username;

            if (Validator.isPostTooLong(S.msg)) {
                errorMsg err = new errorMsg();
                err.context = "post";
                err.msg = "Post exceeds 256 character limit.";
                conn.send(gson.toJson(err));
                return;
            }

            int postId = nextPostId.getAndIncrement();
            Post post = new Post(postId, sender, S.msg);
            posts.put(postId, post);

            sendToAllMsg broadcast = new sendToAllMsg();
            broadcast.msg = S.msg;
            broadcast.username = sender;
            broadcast.postId = postId;
            broadcast.imageData = S.imageData;

            clientSession.saveMessage(gson.toJson(broadcast));

            if (clientManager != null) {
                for (java.util.Map.Entry<org.java_websocket.WebSocket, String> entry : clientManager.getClientNames().entrySet()) {
                    if (clientManager.canReceive(sender, entry.getValue())) {
                        entry.getKey().send(gson.toJson(broadcast));
                    }
                }
            } else {
                server.broadcast(gson.toJson(broadcast));
            }

        } else if (message.contains("likeMsg")) {
            likeMsg L = gson.fromJson(message, likeMsg.class);
            Post p = posts.get(L.postId);
            if (p != null) {
                String liker = clientManager != null ? clientManager.getUsername(conn) : "unknown";
                if (L.unlike) {
                    if (p.likedBy.remove(liker)) {
                        p.likes = Math.max(0, p.likes - 1);
                        p.likeHiddenFrom.remove(liker);
                        broadcastPostUpdate(p, liker);
                    }
                } else if (!p.likedBy.contains(liker)) {
                    p.likedBy.add(liker);
                    p.likes++;
                    // mark which currently-connected users should never see this like
                    if (clientManager != null) {
                        java.util.Set<String> hiddenFor = new java.util.HashSet<>();
                        for (java.util.Map.Entry<org.java_websocket.WebSocket, String> e : clientManager.getClientNames().entrySet()) {
                            String r = e.getValue();
                            if (!clientManager.canReceive(p.username, r) || !clientManager.canReceive(liker, r)) {
                                hiddenFor.add(r);
                            }
                        }
                        if (!hiddenFor.isEmpty()) p.likeHiddenFrom.put(liker, hiddenFor);
                    }
                    broadcastPostUpdate(p, liker);
                }
            }

        } else if (message.contains("replyMsg")) {
            replyMsg R = gson.fromJson(message, replyMsg.class);
            Post p = posts.get(R.postId);
            if (p != null) {
                String sender = clientManager != null ? clientManager.getUsername(conn) : R.username;
                Post.Reply reply = new Post.Reply(sender, R.msg);
                // mark which currently-connected users should never see this reply
                if (clientManager != null) {
                    for (java.util.Map.Entry<org.java_websocket.WebSocket, String> e : clientManager.getClientNames().entrySet()) {
                        String r = e.getValue();
                        if (!clientManager.canReceive(p.username, r) || !clientManager.canReceive(sender, r)) {
                            reply.hiddenFrom.add(r);
                        }
                    }
                }
                p.replies.add(reply);
                broadcastPostUpdate(p, sender);
            }

        } else if (message.contains("blockMsg")) {
            blockMsg B = gson.fromJson(message, blockMsg.class);

            if (clientManager != null) {
                String blocker = clientManager.getUsername(conn);
                if (blocker != null && B.targetUsername != null && !B.targetUsername.equals(blocker)) {
                    if (B.unblock) {
                        clientManager.unblockUser(blocker, B.targetUsername);
                    } else {
                        clientManager.blockUser(blocker, B.targetUsername);
                    }
                }
            }

        } else {
            sendToAllMsg err = new sendToAllMsg();
            err.msg = "msg incorrect format: " + message;
            conn.send(gson.toJson(err));
        }
    }

    public void onClientDisconnected(WebSocket conn) {
        String displayName;
        if (clientManager != null) {
            String username = clientManager.getUsername(conn);
            displayName = (username != null) ? username : conn.getRemoteSocketAddress().toString();
            clientManager.removeClient(conn);
        } else {
            displayName = String.valueOf(conn.getRemoteSocketAddress());
        }

        sendToAllMsg broadcast = new sendToAllMsg();
        broadcast.msg = displayName + " has left";
        server.broadcast(gson.toJson(broadcast));

        if (clientManager != null) {
            broadcastUserList();
        }
    }

    private void broadcastPostUpdate(Post p, String actor) {
        if (clientManager != null) {
            for (java.util.Map.Entry<org.java_websocket.WebSocket, String> entry : clientManager.getClientNames().entrySet()) {
                String recipient = entry.getValue();
                if (!clientManager.canReceive(p.username, recipient) ||
                        (actor != null && !clientManager.canReceive(actor, recipient))) {
                    continue;
                }
                postUpdateMsg filtered = buildFilteredUpdate(p, recipient);
                entry.getKey().send(gson.toJson(filtered));
            }
        } else {
            postUpdateMsg update = new postUpdateMsg();
            update.postId = p.postId;
            update.likes = p.likes;
            update.replies = p.replies;
            server.broadcast(gson.toJson(update));
        }
    }

    private postUpdateMsg buildFilteredUpdate(Post p, String recipient) {
        postUpdateMsg update = new postUpdateMsg();
        update.postId = p.postId;
        update.likes = 0;
        for (String liker : p.likedBy) {
            java.util.Set<String> hidden = p.likeHiddenFrom.get(liker);
            if (hidden == null || !hidden.contains(recipient)) update.likes++;
        }
        update.replies = new java.util.ArrayList<>();
        for (Post.Reply r : p.replies) {
            if (!r.hiddenFrom.contains(recipient)) update.replies.add(r);
        }
        return update;
    }

    private void broadcastUserList() {
        userListMsg ulm = new userListMsg();
        ulm.users = clientManager.getConnectedUsernames();
        server.broadcast(gson.toJson(ulm));
    }
}