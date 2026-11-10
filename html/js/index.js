var connection = null;
var selectedImageData = null;
var emojiPickerOpen = false;
var blockedUsers = new Set();
var myUsername = "";

var serverUrl = "ws://" + window.location.hostname + ":" + (parseInt(location.port) + 1);

function connectToServer() {
    connection = new WebSocket(serverUrl);

    connection.onopen  = function () {};
    connection.onclose = function () {
        setTimeout(function () {
            connectToServer();
        }, 3000);
    };

    connection.onmessage = function (evt) {
        var obj = JSON.parse(evt.data);

        if (obj.msgType === "idMsg") {

        } else if (obj.msgType === "displayMsg") {
            document.getElementById("login-page").hidden = !obj.displayName;
            document.getElementById("main-app").hidden   = !obj.displayMain;

        } else if (obj.msgType === "sendToAllMsg") {
            renderTweet(obj);

        } else if (obj.msgType === "userListMsg") {
            renderUserList(obj.users);

        } else if (obj.msgType === "postUpdateMsg") {
            updateTweet(obj);

        } else if (obj.msgType === "errorMsg") {
            if (obj.context === "login") {
                var err = document.getElementById("login-error");
                err.textContent = obj.msg;
                err.style.display = "block";
            } else if (obj.context === "post") {
                showPostError(obj.msg);
            }
        }
    };
}

connectToServer();

/* ─── COLORS ─── */
var COLORS = ["#1d9bf0","#7c6cf5","#e0245e","#17bf63","#f45d22","#ff8c00","#9b59b6"];

function pickColor(name) {
    return COLORS[(name || " ").charCodeAt(0) % COLORS.length];
}

/* ─── RENDER TWEET ─── */
function renderTweet(obj) {
    var timeline = document.getElementById("timeline");

    if (!obj.username) {
        addActivity(obj.msg);
        return;
    }

    // Skip duplicate posts (same postId already rendered — can happen in history replay)
    if (obj.postId && document.querySelector('[data-post-id="' + obj.postId + '"]')) {
        return;
    }

    var card = document.createElement("div");
    card.className = "tweet-card";
    if (obj.postId) card.dataset.postId = obj.postId;
    if (obj.username) card.dataset.postAuthor = obj.username;

    var color = pickColor(obj.username);

    /* top row */
    var top = document.createElement("div");
    top.className = "tweet-top";

    var av = document.createElement("div");
    av.className = "tweet-av";
    av.style.background = color;
    av.textContent = obj.username.charAt(0).toUpperCase();
    top.appendChild(av);

    var body = document.createElement("div");
    body.className = "tweet-body";

    /* meta */
    var meta = document.createElement("div");
    meta.className = "tweet-meta";

    var nameEl = document.createElement("span");
    nameEl.className = "tweet-name";
    nameEl.textContent = obj.username;

    var handleEl = document.createElement("span");
    handleEl.className = "tweet-handle";
    handleEl.textContent = "@" + obj.username.toLowerCase().replace(/\s+/g, "_");

    meta.appendChild(nameEl);
    meta.appendChild(handleEl);
    body.appendChild(meta);

    /* text */
    if (obj.msg) {
        var textEl = document.createElement("div");
        textEl.className = "tweet-text";
        textEl.textContent = obj.msg;
        body.appendChild(textEl);
    }

    /* image */
    if (obj.imageData) {
        var img = document.createElement("img");
        img.src = obj.imageData;
        img.className = "tweet-img";
        img.title = "Click to expand";
        img.onclick = function () { window.open(obj.imageData, "_blank"); };
        body.appendChild(img);
    }

    /* action bar */
    var actions = document.createElement("div");
    actions.className = "tweet-actions";

    /* build reply section first so we can reference it directly */
    var replySection = null;
    if (obj.postId) {
        replySection = document.createElement("div");
        replySection.className = "replies-section";
    }

    if (obj.postId) {
        var isAuthorBlocked = blockedUsers.has(obj.username);

        /* reply button — holds direct reference to its own section */
        var replyBtn = document.createElement("button");
        replyBtn.className = "action-btn reply-btn";
        replyBtn.innerHTML = '<span class="action-icon">💬</span><span class="reply-count">0</span>';
        replyBtn.title = "Reply";
        (function (section) {
            replyBtn.onclick = function () { if (!replyBtn.disabled) toggleReplySection(section); };
        })(replySection);
        if (isAuthorBlocked) { replyBtn.disabled = true; replyBtn.style.opacity = "0.3"; replyBtn.style.cursor = "not-allowed"; }
        actions.appendChild(replyBtn);

        /* like button */
        var likeBtn = document.createElement("button");
        likeBtn.className = "action-btn like-btn";
        likeBtn.innerHTML = '<span class="action-icon">🤍</span><span class="like-count">0</span>';
        likeBtn.title = "Like";
        (function (pid) {
            likeBtn.onclick = function () {
                if (likeBtn.disabled) return;
                var isLiked = likeBtn.classList.contains("liked");
                var countEl = likeBtn.querySelector(".like-count");
                if (isLiked) {
                    likeBtn.classList.remove("liked");
                    likeBtn.querySelector(".action-icon").textContent = "🤍";
                    if (countEl) countEl.textContent = Math.max(0, (parseInt(countEl.textContent) || 0) - 1);
                    connection.send(JSON.stringify({ msgType: "likeMsg", postId: pid, unlike: true }));
                } else {
                    likeBtn.classList.add("liked");
                    likeBtn.querySelector(".action-icon").textContent = "❤️";
                    if (countEl) countEl.textContent = (parseInt(countEl.textContent) || 0) + 1;
                    connection.send(JSON.stringify({ msgType: "likeMsg", postId: pid, unlike: false }));
                }
            };
        })(obj.postId);
        if (isAuthorBlocked) { likeBtn.disabled = true; likeBtn.style.opacity = "0.3"; likeBtn.style.cursor = "not-allowed"; }
        actions.appendChild(likeBtn);
    }

    body.appendChild(actions);
    top.appendChild(body);
    card.appendChild(top);

    /* append reply section */
    if (obj.postId) {
        replySection.id = "replies-" + obj.postId;

        var replyCompose = document.createElement("div");
        replyCompose.className = "reply-compose";

        var rAv = document.createElement("div");
        rAv.className = "reply-av";
        rAv.id = "reply-av-" + obj.postId;
        rAv.textContent = myUsername.charAt(0).toUpperCase() || "U";

        var rInput = document.createElement("textarea");
        rInput.className = "reply-input";
        rInput.placeholder = "Post your reply";
        rInput.rows = 1;

        var rBtn = document.createElement("button");
        rBtn.className = "reply-send-btn";
        rBtn.textContent = "Reply";
        (function (pid, inp) {
            rBtn.onclick = function () {
                var text = inp.value.trim();
                if (!text) return;
                var payload = { msgType: "replyMsg", postId: pid, msg: text, username: myUsername };
                connection.send(JSON.stringify(payload));
                inp.value = "";
            };
        })(obj.postId, rInput);

        replyCompose.appendChild(rAv);
        replyCompose.appendChild(rInput);
        replyCompose.appendChild(rBtn);
        replySection.appendChild(replyCompose);

        var repliesList = document.createElement("div");
        repliesList.className = "replies-list";
        replySection.appendChild(repliesList);

        card.appendChild(replySection);
    }

    timeline.appendChild(card);
    timeline.scrollTop = timeline.scrollHeight;
}

/* ─── TOGGLE REPLY SECTION ─── */
function toggleReplySection(section) {
    if (!section) return;
    section.classList.toggle("open");
    if (section.classList.contains("open")) {
        section.querySelector(".reply-input").focus();
    }
}

/* ─── UPDATE TWEET (likes / replies) ─── */
function updateTweet(obj) {
    var card = document.querySelector('[data-post-id="' + obj.postId + '"]');
    if (!card) return;

    /* like count sync from server */
    if (obj.likes !== undefined) {
        var likeCount = card.querySelector(".like-count");
        if (likeCount) likeCount.textContent = obj.likes;
    }

    /* replies */
    if (obj.replies && obj.replies.length > 0) {
        var visibleReplies = obj.replies.filter(function (r) { return !blockedUsers.has(r.username); });
        var replyCount = card.querySelector(".reply-count");
        if (replyCount) replyCount.textContent = visibleReplies.length;

        if (visibleReplies.length > 0) {
            var section = card.querySelector(".replies-section");
            if (section) { section.classList.add("open"); }
        }

        var list = card.querySelector(".replies-list");
        if (list) {
            list.innerHTML = "";
            visibleReplies.forEach(function (r) {
                var item = document.createElement("div");
                item.className = "reply-item";

                var rAv = document.createElement("div");
                rAv.className = "reply-item-av";
                rAv.style.background = pickColor(r.username);
                rAv.textContent = (r.username || "?").charAt(0).toUpperCase();

                var rBody = document.createElement("div");
                rBody.className = "reply-item-body";

                var rName = document.createElement("div");
                rName.className = "reply-item-name";
                rName.textContent = r.username;

                var rText = document.createElement("div");
                rText.className = "reply-item-text";
                rText.textContent = r.msg;

                rBody.appendChild(rName);
                rBody.appendChild(rText);
                item.appendChild(rAv);
                item.appendChild(rBody);
                list.appendChild(item);
            });
        }
    }
}

/* ─── ACTIVITY SECTION ─── */
function addActivity(msg) {
    var list = document.getElementById("activity-list");

    // clear placeholder
    var placeholder = list.querySelector("div");
    if (placeholder && placeholder.style.color === "rgb(83, 100, 113)") {
        list.innerHTML = "";
    }

    var isJoin = msg.toLowerCase().includes("joined");
    var isLeft = msg.toLowerCase().includes("left");

    var item = document.createElement("div");
    item.className = "activity-item";

    var dot = document.createElement("div");
    dot.className = "activity-dot " + (isJoin ? "joined" : isLeft ? "left" : "joined");

    var text = document.createElement("span");

    // bold the username part
    var parts = msg.split(" has ");
    if (parts.length === 2) {
        var nameSpan = document.createElement("span");
        nameSpan.className = "activity-name";
        nameSpan.textContent = parts[0];
        text.appendChild(nameSpan);
        text.appendChild(document.createTextNode(" has " + parts[1]));
    } else {
        text.textContent = msg;
    }

    item.appendChild(dot);
    item.appendChild(text);

    // newest at top, keep max 8
    list.insertBefore(item, list.firstChild);
    while (list.children.length > 8) list.removeChild(list.lastChild);
}

/* ─── RENDER ONLINE USERS ─── */
function renderUserList(users) {
    var list = document.getElementById("online-users-list");
    list.innerHTML = "";

    users.forEach(function (username) {
        var row = document.createElement("div");
        row.className = "online-row";

        var left = document.createElement("div");
        left.className = "online-left";

        var wrap = document.createElement("div");
        wrap.className = "online-av-wrap";

        var av = document.createElement("div");
        av.className = "online-av";
        av.style.background = pickColor(username);
        av.textContent = username.charAt(0).toUpperCase();

        var dot = document.createElement("div");
        dot.className = "online-dot";

        wrap.appendChild(av);
        wrap.appendChild(dot);

        var name = document.createElement("div");
        name.className = "online-name";
        name.textContent = username;

        left.appendChild(wrap);
        left.appendChild(name);
        row.appendChild(left);

        if (username !== myUsername) {
            var isBlocked = blockedUsers.has(username);
            var btn = document.createElement("button");
            btn.id = "block-btn-" + username;
            btn.className = "block-btn " + (isBlocked ? "btn-unblock" : "btn-block");
            btn.textContent = isBlocked ? "Unblock" : "Block";
            (function (u) {
                btn.onclick = isBlocked ? function () { unblockUser(u); } : function () { blockUser(u); };
            })(username);
            row.appendChild(btn);
        }

        list.appendChild(row);
    });
}

/* ─── ACTIONS ─── */

nameButton = function () {
    var name = document.getElementById("username").value.trim();
    document.getElementById("login-error").style.display = "none";
    myUsername = name;

    var color = pickColor(name);
    var initial = name.charAt(0).toUpperCase() || "U";
    var grad = "linear-gradient(135deg, " + color + ", " + pickColor(name + "1") + ")";

    document.getElementById("nav-username").textContent = name;
    document.getElementById("nav-avatar").textContent = initial;
    document.getElementById("nav-avatar").style.background = grad;
    document.getElementById("compose-avatar").textContent = initial;
    document.getElementById("compose-avatar").style.background = grad;

    connection.send(JSON.stringify({ msgType: "nameMsg", name: name }));
    document.getElementById("username").value = "";
};

msgButton = function () {
    var msg = document.getElementById("sendMsg").value;
    if (!msg.trim() && !selectedImageData) { showPostError("Message cannot be empty."); return; }
    if (msg.length > 256) { showPostError("Message cannot exceed 256 characters."); return; }
    var obj = { msgType: "sendMsg", msg: msg, username: myUsername };
    if (selectedImageData) obj.imageData = selectedImageData;
    connection.send(JSON.stringify(obj));
    document.getElementById("sendMsg").value = "";
    updateCharCount();
    removeImage();
};

blockUser = function (targetUsername) {
    blockedUsers.add(targetUsername);
    connection.send(JSON.stringify({ msgType: "blockMsg", targetUsername: targetUsername, unblock: false }));
    var btn = document.getElementById("block-btn-" + targetUsername);
    if (btn) { btn.textContent = "Unblock"; btn.className = "block-btn btn-unblock"; btn.onclick = function () { unblockUser(targetUsername); }; }
    document.querySelectorAll('[data-post-author="' + targetUsername + '"] .action-btn').forEach(function (b) {
        b.disabled = true;
        b.style.opacity = "0.3";
        b.style.cursor = "not-allowed";
    });
};

unblockUser = function (targetUsername) {
    blockedUsers.delete(targetUsername);
    connection.send(JSON.stringify({ msgType: "blockMsg", targetUsername: targetUsername, unblock: true }));
    var btn = document.getElementById("block-btn-" + targetUsername);
    if (btn) { btn.textContent = "Block"; btn.className = "block-btn btn-block"; btn.onclick = function () { blockUser(targetUsername); }; }
    document.querySelectorAll('[data-post-author="' + targetUsername + '"] .action-btn').forEach(function (b) {
        b.disabled = false;
        b.style.opacity = "";
        b.style.cursor = "";
    });
};

logoutButton = function () {
    connection.close();
    myUsername = "";
    document.getElementById("login-page").hidden = false;
    document.getElementById("main-app").hidden   = true;
    document.getElementById("username").value    = "";
    document.getElementById("timeline").innerHTML = "";
    blockedUsers.clear();
    connectToServer();
};

/* ─── CHAR COUNT ─── */
updateCharCount = function () {
    var len = document.getElementById("sendMsg").value.length;
    var el  = document.getElementById("char-count");
    el.textContent = len + " / 256";
    el.className = len > 256 ? "over" : len > 220 ? "warn" : "";
};

function showPostError(msg) {
    var err = document.getElementById("post-error");
    err.textContent = msg;
    err.style.display = "inline";
    setTimeout(function () { err.style.display = "none"; }, 3000);
}

/* ─── IMAGE ─── */
function handleImageSelect(event) {
    var file = event.target.files[0];
    if (!file) return;
    loadImageFile(file);
    event.target.value = "";
}

function loadImageFile(file) {
    var reader = new FileReader();
    reader.onload = function (e) {
        selectedImageData = e.target.result;
        document.getElementById("image-preview").src = selectedImageData;
        document.getElementById("image-preview-area").style.display = "block";
    };
    reader.readAsDataURL(file);
}

document.addEventListener("paste", function (event) {
    var items = event.clipboardData && event.clipboardData.items;
    if (!items) return;
    for (var i = 0; i < items.length; i++) {
        if (items[i].type.startsWith("image/")) {
            event.preventDefault();
            loadImageFile(items[i].getAsFile());
            break;
        }
    }
});

function removeImage() {
    selectedImageData = null;
    document.getElementById("image-preview-area").style.display = "none";
    document.getElementById("image-preview").src = "";
}

/* ─── EMOJI PICKER ─── */
var emojiGroups = {
    "Smileys":  ["😀","😂","😍","🥰","😎","🤔","😢","😡","😅","🤯","😴","🤗","😊","🥳","😤","🤣","😇","🫠","😬","🫡"],
    "Gestures": ["👍","👎","👏","🙏","💪","🤞","✌️","🫶","🤙","👋"],
    "Symbols":  ["❤️","🔥","🎉","💯","✨","🌟","💀","🎊","💬","⚡","💥","🎯","🏆","💡","🫧"]
};

function buildEmojiPicker() {
    var picker = document.getElementById("emoji-picker");
    picker.innerHTML = "";
    var box = document.createElement("div");
    box.style.cssText = "background:#16181c;border:1px solid #2f3336;border-radius:12px;padding:12px;";
    Object.keys(emojiGroups).forEach(function (group) {
        var label = document.createElement("div");
        label.style.cssText = "font-size:10px;color:#536471;text-transform:uppercase;letter-spacing:.06em;margin:6px 0 4px;font-weight:600;";
        label.textContent = group;
        box.appendChild(label);
        var grid = document.createElement("div");
        grid.style.cssText = "display:flex;flex-wrap:wrap;gap:2px;";
        emojiGroups[group].forEach(function (emoji) {
            var btn = document.createElement("button");
            btn.textContent = emoji;
            btn.style.cssText = "font-size:21px;background:none;border:none;border-radius:6px;cursor:pointer;padding:4px 5px;line-height:1;";
            btn.onmouseover = function () { btn.style.background = "#2f3336"; };
            btn.onmouseout  = function () { btn.style.background = "none"; };
            btn.onclick     = function () { insertEmoji(emoji); };
            grid.appendChild(btn);
        });
        box.appendChild(grid);
    });
    picker.appendChild(box);
}

function toggleEmojiPicker() {
    emojiPickerOpen = !emojiPickerOpen;
    var picker = document.getElementById("emoji-picker");
    if (emojiPickerOpen) { buildEmojiPicker(); picker.style.display = "block"; }
    else                 { picker.style.display = "none"; }
}

function insertEmoji(emoji) {
    var input = document.getElementById("sendMsg");
    var s = input.selectionStart, e = input.selectionEnd;
    input.value = input.value.slice(0, s) + emoji + input.value.slice(e);
    input.selectionStart = input.selectionEnd = s + emoji.length;
    input.focus();
    updateCharCount();
}

document.addEventListener("click", function (e) {
    if (!emojiPickerOpen) return;
    var picker   = document.getElementById("emoji-picker");
    var emojiBtn = document.querySelector(".compose-btn[title='Emoji']");
    if (!picker.contains(e.target) && e.target !== emojiBtn) toggleEmojiPicker();
});
