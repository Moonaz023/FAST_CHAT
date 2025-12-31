let currentPage = 0;
const PAGE_SIZE = 10;
let hasMore = true;
let isLoading = false;
import { BACKEND_URL } from './config.js';
export async function loadAiConversationList(append = false) {
    if (isLoading) return;

    if (!append) {
        currentPage = 0;
        hasMore = true;
        document.getElementById("aiConversationList").innerHTML =
            `<li class="px-6 py-3 text-gray-400">Loading...</li>`;
    }

    isLoading = true;

    const token = localStorage.getItem("authToken");
    const list = document.getElementById("aiConversationList");

    try {
        const res = await fetch(
            `${BACKEND_URL}/api/conversation/getAiConversionList?page=${currentPage}&size=${PAGE_SIZE}`,
            { headers: { Authorization: `Bearer ${token}` } }
        );

        const data = await res.json();
        const convs = data.content || data || [];

        if (!append) {
            list.innerHTML = "";

            // ===== NEW CHAT ITEM =====
            const newChatLi = document.createElement("li");
            newChatLi.className =
                "px-6 py-3 hover:bg-white/5 cursor-pointer flex items-center gap-4 min-w-0 border-b border-white/10";

            const iconWrapper = document.createElement("div");
            iconWrapper.className =
                "w-8 h-8 bg-gradient-to-br from-purple-500 to-pink-500 rounded-lg flex items-center justify-center flex-shrink-0";

            const icon = document.createElement("i");
            icon.className =
                "fa-solid fa-comment-medical text-white text-xs";

            iconWrapper.appendChild(icon);

            const label = document.createElement("span");
            label.textContent = "New Chat";
            label.className = "flex-1 text-white/90 font-medium";

            newChatLi.append(iconWrapper, label);

            newChatLi.onclick = () => {
                import("./ai_chat.js").then(chat =>
                    chat.openConversation("Gemini AI", null)
                );
            };

            list.appendChild(newChatLi);
            // ===== END NEW CHAT ITEM =====
        }

        // ===== CONVERSATIONS =====
        convs
            .sort((a, b) => {
                const ua = unreadCounts[a.conversationId] || 0;
                const ub = unreadCounts[b.conversationId] || 0;

                if (ua > 0 && ub === 0) return -1;
                if (ub > 0 && ua === 0) return 1;

                return (
                    new Date(b.lastMessageTime || b.updatedAt || 0) -
                    new Date(a.lastMessageTime || a.updatedAt || 0)
                );
            })
            .forEach(c => createConversationItem(c, list));

        handleLoadMore(convs.length, data.totalPages);
        currentPage++;
    } catch (err) {
        console.error(err);

        if (!append) {
            list.innerHTML =
                `<li class="px-6 py-3 text-red-400">Failed to load conversations</li>`;
        }
    } finally {
        isLoading = false;
    }
}


function createConversationItem(c, list) {
    const li = document.createElement("li");
    li.dataset.convId = c.conversationId;
    li.className = "px-6 py-3 hover:bg-white/5 cursor-pointer flex items-center gap-4 min-w-0";

    const name = document.createElement("span");
    name.textContent = c.useName;
    name.className = "flex-1 text-white/90 font-medium truncate";

    const badge = document.createElement("span");
    badge.className = "conv-unread-badge";
    const count = unreadCounts[c.conversationId] || 0;
    if (count > 0) {
        badge.textContent = count > 99 ? "99+" : count;
        badge.classList.add("show");
    }

    li.append(name, badge);
    li.onclick = () => {
        badge.classList.remove("show");
        badge.textContent = "";
        
        import('./ai_chat.js').then(chat => chat.openConversation(c.useName, c.conversationId));
    };

    list.appendChild(li);
}

function handleLoadMore(length, totalPages) {
    if (length === PAGE_SIZE && (!totalPages || currentPage + 1 < totalPages)) {
        if (!document.getElementById("loadMoreBtn")) {
            const more = document.createElement("li");
            more.id = "loadMoreBtn";
            more.textContent = "Load more...";
            more.className = "px-6 py-4 text-center text-white/50 hover:text-white/80 cursor-pointer text-sm";
            more.onclick = () => { more.remove(); loadConversations(true); };
            document.getElementById("conversationList").appendChild(more);
        }
    } else {
        document.getElementById("loadMoreBtn")?.remove();
        hasMore = false;
    }
}