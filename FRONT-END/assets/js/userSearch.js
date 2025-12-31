// userSearch.js
import { BACKEND_URL } from './config.js';
let searchTimeout = null;

export function initUserSearch() {
    const input = document.getElementById("userSearchInput");
    const results = document.getElementById("userSearchResults");

    if (!input || !results) return;

    input.addEventListener("input", () => {
        const q = input.value.trim();

        clearTimeout(searchTimeout);

        if (!q) {
            results.innerHTML = "";
            results.classList.add("hidden");
            return;
        }

        // ⏳ Debounce 300ms
        searchTimeout = setTimeout(() => searchUsers(q, results), 300);
    });
}

async function searchUsers(query, resultsBox) {
    const token = localStorage.getItem("authToken");

    try {
        const res = await fetch(
            `${BACKEND_URL}/api/users/search?query=${encodeURIComponent(query)}`,
            { headers: { Authorization: `Bearer ${token}` } }
        );

        if (!res.ok) throw new Error("Failed to search");

        const users = await res.json();

        resultsBox.innerHTML = "";
        resultsBox.classList.remove("hidden");

        if (!users.length) {
            resultsBox.innerHTML = `<li>No users found</li>`;
            return;
        }

        users.forEach(u => {
            const li = document.createElement("li");
            li.textContent = u.name;
            li.dataset.userId = u.id;

            // OPEN DIRECT CHAT
            //li.onclick = () => openNewDirectChat(u.name, u.id);
            li.onclick = () => openNewDirectChat(u);

            resultsBox.appendChild(li);
        });

    } catch (err) {
        console.error("Search error:", err);
        resultsBox.innerHTML = `<li class="text-red-400">Error</li>`;
        resultsBox.classList.remove("hidden");
    }
}

/**
 * Opens a chat with a user that has NO conversation created yet.
 * Conversation will be created on first message.
 */
/*export function openNewDirectChat(username, userId) {
    import('./chat.js')
        .then(chat => {
            chat.openConversation(username, null, userId);
        })
        .catch(err => console.error("Failed to open direct chat:", err));
}*/

export async function openNewDirectChat(user) {

    const token = localStorage.getItem("authToken");
    let conversationId = null;

    try {
        const res = await fetch(
            `${BACKEND_URL}/api/conversation/findOrNull/${user.id}`,
            {
                headers: { Authorization: `Bearer ${token}` }
            }
        );

        const data = await res.json();
        conversationId = data.conversationId;

    } catch (err) {
        console.error("Failed to check conversation:", err);
    }

    import("./chat.js")
        .then(chat => {
            chat.openConversation(
                user.name,
                conversationId,
                user.id
            );
        })
        .catch(err => console.error("Failed to open new chat:", err));
}
