// ai_chat.js — unified & stream-safe markdown rendering

import { BACKEND_URL } from './config.js';
import { stompClient } from './websocket.js';

let currentEventSource = null;

/* ─────────────────────────────────────────────────────────────
   MARKDOWN RENDERING (SINGLE SOURCE OF TRUTH)
───────────────────────────────────────────────────────────── */

const md = window.markdownit({
    html: true,
    breaks: true,
    linkify: true
});

function renderMarkdown(raw) {
    if (!raw) return "";

    let rendered = md.render(raw);

    rendered = rendered
        .replace(/<\/strong>\s*\*(\s|$)/g, '</strong>$1')
        .replace(/\*\s*$/gm, '')
        .replace(/^\*\s/gm, '• ');

    return DOMPurify.sanitize(rendered);
}

/* ─────────────────────────────────────────────────────────────
   STREAM HELPERS (CRITICAL FIX)
───────────────────────────────────────────────────────────── */

function normalizeStreamChunk(chunk) {
    if (!chunk) return "";

    if (chunk.includes("\n")) return chunk;

    if (!chunk.startsWith(" ") && !chunk.startsWith("\n")) {
        return " " + chunk;
    }

    return chunk;
}

function needsParagraphBreak(text) {
    return (
        text.endsWith(".") ||
        text.endsWith("!") ||
        text.endsWith("?") ||
        text.endsWith(":")
    );
}

/* ─────────────────────────────────────────────────────────────
   OPEN / CLOSE CONVERSATION
───────────────────────────────────────────────────────────── */

export async function openConversation(name, id, targetUserId = null) {
    currentEventSource?.close();
    currentEventSource = null;

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

        if (stompClient?.connected) {
            stompClient.send("/app/chat.read", {}, JSON.stringify({
                conversationId: id,
                messageId: null
            }));
        }
    }
}

export function closeConversation() {
    currentEventSource?.close();
    currentEventSource = null;

    window.activeConversationId = null;
    document.getElementById("waEmpty").classList.remove("hidden");
    document.getElementById("waHeader").classList.add("hidden");
    document.getElementById("waMessages").classList.add("hidden");
    document.getElementById("waInput").classList.add("hidden");
    document.getElementById("waMessages").innerHTML = "";
    document.getElementById("messageInput").value = "";
    window.clearSendFunction();
}

/* ─────────────────────────────────────────────────────────────
   SEND MESSAGE + STREAM AI RESPONSE
───────────────────────────────────────────────────────────── */

export async function sendMessage() {
    if (!stompClient?.connected) return alert("Not connected");

    const input = document.getElementById("messageInput");
    const text = input.value.trim();
    if (!text) return;

    addMessage(text, "you");
    input.value = "";
    input.disabled = true;

    currentEventSource?.close();
    currentEventSource = null;

    const token = localStorage.getItem("authToken");
    if (!token) {
        input.disabled = false;
        return alert("Not authenticated");
    }

    const url = `${BACKEND_URL}/api/ai/stream-story`;

    let aiBubble = null;
    let accumulatedMarkdown = "";
    let isError = false;
    let newConversationId = null;
    let conversationName = null;

    fetch(url, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
            Accept: "text/event-stream"
        },
        body: JSON.stringify({
            prompt: text,
            conversationId: window.activeConversationId || null
        })
    })
    .then(res => {
        if (!res.ok) throw new Error("Stream failed");

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        function read() {
            reader.read().then(({ done, value }) => {
                if (done) {
                    input.disabled = false;

                    if (newConversationId) {
                        window.activeConversationId = newConversationId;
                        if (conversationName) {
                            document.getElementById("waHeaderName").textContent = conversationName;
                        }
                        addConversationToSidebar(newConversationId, conversationName);
                    }
                    return;
                }

                buffer += decoder.decode(value, { stream: true });
                const parts = buffer.split("\n\n");
                buffer = parts.pop() || "";

                parts.forEach(block => {
                    block.split("\n").forEach(line => {
                        let data = line.trim();
                        if (!data) return;
                        if (data.startsWith("data:")) data = data.slice(5).trim();

                        if (data.startsWith("[CONVERSATION_ID:")) {
                            newConversationId = data.match(/\[CONVERSATION_ID:(.+?)\]/)?.[1];
                            return;
                        }

                        if (data.startsWith("[CONVERSATION_NAME:")) {
                            conversationName = data.match(/\[CONVERSATION_NAME:(.+?)\]/)?.[1];
                            return;
                        }

                        if (data === "[ERROR_START]") {
                            isError = true;
                            return;
                        }

                        if (data === "[ERROR_END]") return;
                        if (isError) return;

                        // ───── STREAM FIX ─────
                        const normalized = normalizeStreamChunk(data);
                        accumulatedMarkdown += normalized;

                        if (needsParagraphBreak(accumulatedMarkdown.trim())) {
                            accumulatedMarkdown += "\n\n";
                        }

                        accumulatedMarkdown = accumulatedMarkdown
                            .replace(/###(?=\S)/g, "### ")
                            .replace(/##(?=\S)/g, "## ")
                            .replace(/#(?=\S)/g, "# ");

                        if (!aiBubble) {
                            aiBubble = document.createElement("div");
                            aiBubble.className = "wa-bubble wa-them markdown-body";
                            document.getElementById("waMessages").appendChild(aiBubble);
                        }

                        aiBubble.innerHTML = renderMarkdown(accumulatedMarkdown);
                        const box = document.getElementById("waMessages");
                        box.scrollTop = box.scrollHeight;
                    });
                });

                read();
            });
        }
        read();
    })
    .catch(() => {
        input.disabled = false;
        alert("AI stream failed");
    });
}

/* ─────────────────────────────────────────────────────────────
   MESSAGE HELPERS
───────────────────────────────────────────────────────────── */

export function addMessage(text, who) {
    const box = document.getElementById("waMessages");
    const div = document.createElement("div");
    div.className = `wa-bubble wa-${who}`;
    div.textContent = text;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

/* ─────────────────────────────────────────────────────────────
   LOAD MESSAGES FROM DB (FIXED)
───────────────────────────────────────────────────────────── */

let messagePage = 0;
const MESSAGE_PAGE_SIZE = 30;
let hasMoreMessages = true;
let isLoadingMessages = false;

export async function loadMessages(conversationId, append = false) {
    if (isLoadingMessages) return;
    isLoadingMessages = true;

    const token = localStorage.getItem("authToken");
    const box = document.getElementById("waMessages");

    try {
        const res = await fetch(
            `${BACKEND_URL}/api/conversation/ai/${conversationId}/messages?page=${messagePage}&size=${MESSAGE_PAGE_SIZE}`,
            { headers: { Authorization: `Bearer ${token}` } }
        );

        const page = await res.json();
        const messages = page.content || [];

        const fragment = document.createDocumentFragment();

        messages.reverse().forEach(m => {
            const isUser = m.senderName === "user";
            const div = document.createElement("div");
            div.className = `wa-bubble wa-${isUser ? "you" : "them"}`;

            if (isUser) {
                div.textContent = m.content;
            } else {
                div.classList.add("markdown-body");
                div.innerHTML = renderMarkdown(m.content);
            }

            fragment.appendChild(div);
        });

        append ? box.prepend(fragment) : box.appendChild(fragment);
        box.scrollTop = box.scrollHeight;

        messagePage++;
        hasMoreMessages = page.totalElements > messagePage * MESSAGE_PAGE_SIZE;

    } catch (e) {
        console.error("Failed to load messages", e);
    } finally {
        isLoadingMessages = false;
    }
}

/* ───────────────────────────────────────────────────────────── */

export function playNotificationSound() {
    new Audio("/sounds/notification.mp3").play().catch(() => {});
}
