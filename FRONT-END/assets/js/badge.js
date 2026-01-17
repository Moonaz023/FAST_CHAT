// badge.js - Load once, then update locally
import { apiFetch } from './api.js';
const unreadCounts = {};
let notificationCount = 0;
let isInitialized = false;

// ✅ Call this ONCE when app starts
export async function initializeGlobalBadge() {
    if (isInitialized) return;

    try {
        const res = await apiFetch(`/api/conversation/unread/total`);
        if (res.ok) {
            notificationCount = await res.json();
            updateBadge();
            isInitialized = true;
        }
    } catch (e) {
        console.error("Failed to initialize global badge", e);
    }
}

export function incrementUnread(conversationId) {
    unreadCounts[conversationId] = (unreadCounts[conversationId] || 0) + 1;
    updateConversationBadge(conversationId);

    // ✅ Just increment locally - no server fetch!
    notificationCount++;
    updateBadge();
}

export function clearConversationUnread(conversationId) {
    const clearedCount = unreadCounts[conversationId] || 0;
    delete unreadCounts[conversationId];
    updateConversationBadge(conversationId);

    // ✅ Just decrement locally - no server fetch!
    notificationCount = Math.max(0, notificationCount - clearedCount);
    updateBadge();
}

function updateBadge() {
    const badge = document.getElementById("notifBadge");
    if (!badge) return;
    if (notificationCount > 0) {
        badge.textContent = notificationCount > 99 ? "99+" : notificationCount;
        badge.classList.add("show");
    } else {
        badge.classList.remove("show");
        badge.textContent = "";
    }
}

export function updateConversationBadge(conversationId) {
    document.querySelectorAll("#conversationList li").forEach(li => {
        if (li.dataset.convId == conversationId) {
            const badge = li.querySelector(".conv-unread-badge");
            const count = unreadCounts[conversationId] || 0;
            if (count > 0) {
                badge.textContent = count > 99 ? "99+" : count;
                badge.classList.add("show");
            } else {
                badge.classList.remove("show");
                badge.textContent = "";
            }
        }
    });
}

export function seedUnreadCounts(conversations) {
    if (Object.keys(unreadCounts).length === 0) {
        conversations.forEach(c => {
            if (c.unreadCount > 0) unreadCounts[c.conversationId] = c.unreadCount;
        });
    }
}

export { unreadCounts };
window.unreadCounts = unreadCounts;