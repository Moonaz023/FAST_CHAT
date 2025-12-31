// websocket.js
import { BACKEND_URL } from './config.js';
let stompClient = null;

export function connectWS() {
    const token = localStorage.getItem("authToken");
    if (!token) return console.warn("No token");

    const socket = new SockJS(`${BACKEND_URL}/ws`);
    stompClient = Stomp.over(socket);

    stompClient.connect(
        { Authorization: `Bearer ${token}` },
        () => {
            console.log("WebSocket connected");
            stompClient.subscribe("/user/queue/messages", handleIncomingMessage);
        },
        (err) => console.error("WS Error:", err)
    );
}

function handleIncomingMessage(message) {
    const msg = JSON.parse(message.body);
    const myId = JSON.parse(localStorage.getItem("user") || "{}").id;
    const isOpen = msg.conversationId === window.activeConversationId;

    if (msg.senderId === myId) return;

    if (isOpen) {
        import('./chat.js').then(mod => {
            mod.addMessage(msg.content, "them");
            mod.playNotificationSound();
            if (stompClient?.connected) {
                stompClient.send("/app/chat.read", {}, JSON.stringify({
                    conversationId: msg.conversationId,
                    messageId: msg.messageId
                }));
            }
        });
    } else {
        import('./badge.js').then(b => b.incrementUnread(msg.conversationId));
        import('./conversations.js').then(c => c.reorderConversationToTop(msg.conversationId));
    }
}

export { stompClient };
