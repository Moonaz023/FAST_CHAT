// chat.js - FIXED (removed refreshGlobalBadge call)
import { apiFetch } from './api.js';
import { reorderConversationToTop } from './conversations.js';
import { stompClient } from './websocket.js';

export async function openConversation(name, id, targetUserId = null) {
    window.activeConversationId = id;
    window.targetUserId = targetUserId;

    messagePage = 0;
    hasMoreMessages = true;
    isLoadingMessages = false;
    document.getElementById("loadOlderBtn")?.remove();

    document.getElementById("waEmpty").classList.add("hidden");
    document.getElementById("waHeader").classList.remove("hidden");
    document.getElementById("waMessages").classList.remove("hidden");
    document.getElementById("waInput").classList.remove("hidden");
    document.getElementById("waHeaderName").textContent = name;
    document.getElementById("waMessages").innerHTML = "";
    window.setSendFunction(sendMessage);
    if (id) {
        await loadMessages(id);
        reorderConversationToTop(id);

        // Just notify backend - the clearConversationUnread() already updated the badge
        if (stompClient?.connected) {
            stompClient.send("/app/chat.read", {}, JSON.stringify({
                conversationId: id,
                messageId: null
            }));
        }
    } else {
        // New chat — empty screen
        console.log("Opening a new chat — no conversation yet");
    }
}

export function closeConversation() {
    window.activeConversationId = null;
    document.getElementById("waEmpty").classList.remove("hidden");
    document.getElementById("waHeader").classList.add("hidden");
    document.getElementById("waMessages").classList.add("hidden");
    document.getElementById("waInput").classList.add("hidden");
    document.getElementById("waMessages").innerHTML = "";
    document.getElementById("messageInput").value = "";
    window.clearSendFunction();
}
export async function sendMessage() {
    if (!stompClient?.connected) return alert("Not connected");
    const input = document.getElementById("messageInput");
    const text = input.value.trim();
    if (!text) return;


    const targetUserId = window.targetUserId || document.getElementById("chatContainer")?.dataset.targetUserId;

    // Step 1: ask backend to create conversation if missing
    if (!window.activeConversationId) {
        if (!targetUserId) {
            return alert("Chat.js:No user selected for conversation");
        }

        try {
            const res = await apiFetch(`/api/conversation/create`, {
                method: "POST",
                body: JSON.stringify({ targetUserId }),
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(`Failed to create conversation: ${res.status} - ${errorText}`);
            }

            const data = await res.json();
            window.activeConversationId = data.id; // set new id
        } catch (err) {
            console.error("Error creating conversation:", err);
            return alert("Failed to create conversation");
        }
    }

    // Step 2: Send the message
    stompClient.send("/app/chat.send", {}, JSON.stringify({
        conversationId: window.activeConversationId,
        content: text,
        tempId: Date.now()
    }));

    addMessage(text, "you");
    input.value = "";
    reorderConversationToTop(window.activeConversationId);
}
/*export function sendMessage() {
    if (!stompClient?.connected) return alert("Not connected");
    const input = document.getElementById("messageInput");
    const text = input.value.trim();
    if (!text) return;

    stompClient.send("/app/chat.send", {}, JSON.stringify({
        conversationId: window.activeConversationId,
        content: text,
        tempId: Date.now()
    }));

    addMessage(text, "you");
    input.value = "";
    reorderConversationToTop(window.activeConversationId);
}*/

export function addMessage(text, who) {
    const box = document.getElementById("waMessages");
    const div = document.createElement("div");
    div.className = `wa-bubble wa-${who}`;
    div.textContent = text;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

let messagePage = 0;
const MESSAGE_PAGE_SIZE = 30;
let hasMoreMessages = true;
let isLoadingMessages = false;

export async function loadMessages(conversationId, append = false) {
    if (isLoadingMessages || (!append && !hasMoreMessages)) return;

    if (!append) {
        messagePage = 0;
        hasMoreMessages = true;
        document.getElementById("waMessages").innerHTML = "";
        document.getElementById("loadOlderBtn")?.remove();
    }

    isLoadingMessages = true;

    const box = document.getElementById("waMessages");

    try {
        const res = await apiFetch(
            `/api/conversation/${conversationId}/messages?page=${messagePage}&size=${MESSAGE_PAGE_SIZE}`
        );
        const page = await res.json();
        const messages = page.content || [];

        if (messages.length === 0) {
            hasMoreMessages = false;
            document.getElementById("loadOlderBtn")?.remove();
            isLoadingMessages = false;
            return;
        }

        const myId = JSON.parse(localStorage.getItem("user") || "{}").id;
        const fragment = document.createDocumentFragment();

        messages.reverse().forEach(m => {
            const div = document.createElement("div");
            div.className = `wa-bubble wa-${m.senderId === myId ? "you" : "them"}`;
            div.textContent = m.content;
            fragment.appendChild(div);
        });

        if (append) {
            box.prepend(fragment);
        } else {
            box.appendChild(fragment);
            box.scrollTop = box.scrollHeight;
        }

        const totalLoaded = (messagePage + 1) * MESSAGE_PAGE_SIZE;
        hasMoreMessages = page.totalElements > totalLoaded;

        messagePage++;

        document.getElementById("loadOlderBtn")?.remove();

        if (hasMoreMessages) {
            const btn = document.createElement("div");
            btn.id = "loadOlderBtn";
            btn.textContent = "↑ Load older messages";
            btn.className = "text-center py-3 text-white/50 text-sm cursor-pointer hover:text-white/80";
            btn.onclick = () => loadMessages(conversationId, true);
            box.prepend(btn);
        }

    } catch (err) {
        console.error("Failed to load messages", err);
    } finally {
        isLoadingMessages = false;
    }
}

export function playNotificationSound() {
    new Audio("/sounds/notification.mp3").play().catch(() => { });
}