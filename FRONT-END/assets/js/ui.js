// ui.js
import { closeConversation } from './chat.js';

export function initUI() {
    document.getElementById("sendMessageBtn")?.addEventListener("click", () => {
        import('./chat.js').then(c => c.sendMessage());
    });

    document.getElementById("messageInput")?.addEventListener("keypress", e => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            import('./chat.js').then(c => c.sendMessage());
        }
    });

    document.getElementById("closeChatBtn")?.addEventListener("click", closeConversation);
}