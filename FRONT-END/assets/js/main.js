// main.js
import { loadAiConversationList } from './ai_conversations.js';
import { initializeGlobalBadge } from './badge.js';
import { loadConversations } from './conversations.js';
import { initUI } from './ui.js';
import { initUserSearch } from './userSearch.js';
import { connectWS } from './websocket.js';

// Global variable to hold the current active send function
let currentSendFunction = null;

// Expose it globally so chat.js and ai_chat.js can set it
window.setSendFunction = function(sendFn) {
    currentSendFunction = sendFn;
};

// Optional: expose a way to clear it (useful on close)
window.clearSendFunction = function() {
    currentSendFunction = null;
};

document.addEventListener("DOMContentLoaded", () => {
    // Toggle regular conversations
    document.getElementById("conversationsBtn")?.addEventListener("click", () => {
        const list = document.getElementById("conversationList");
        list.classList.toggle("hidden");
        if (!list.classList.contains("hidden")) {
            loadConversations();
        }
    });

    // Toggle AI conversations
    document.getElementById("aiConversationsBtn")?.addEventListener("click", () => {
        const list = document.getElementById("aiConversationList");
        list.classList.toggle("hidden");
        if (!list.classList.contains("hidden")) {
            loadAiConversationList();  // ← Fixed: was calling loadConversations()
        }
    });

    // Single shared click handler for Send button
    document.getElementById("sendMessageBtn")?.addEventListener("click", () => {
        if (currentSendFunction) {
            currentSendFunction();
        }
    });

    // Enter key to send (without Shift)
    document.getElementById("messageInput")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            if (currentSendFunction) {
                currentSendFunction();
            }
        }
    });

    // Initialize everything
    initUI();
    connectWS();
    initializeGlobalBadge();
    initUserSearch();
    loadConversations();
    loadAiConversationList();
});