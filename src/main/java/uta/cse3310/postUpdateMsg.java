package uta.cse3310;

import java.util.List;

public class postUpdateMsg {
    public String msgType = "postUpdateMsg";
    public int postId;
    public int likes;
    public List<Post.Reply> replies;
}
