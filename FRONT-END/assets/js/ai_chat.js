// ai_chat.js - Updated with conversation creation handling
import { BACKEND_URL } from './config.js';
import { stompClient } from './websocket.js';

let currentEventSource = null;

export async function openConversation(name, id, targetUserId = null) {
    if (currentEventSource) {
        currentEventSource.close();
        currentEventSource = null;
    }

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
    } else {
        console.log("AI_Chat : Opening a new chat — no conversation yet");
    }
}

export function closeConversation() {
    if (currentEventSource) {
        currentEventSource.close();
        currentEventSource = null;
    }

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

    addMessage(text, "you");
    input.value = "";
    input.disabled = true;

    // Streaming for AI conversations (both new and existing)
    if (currentEventSource !== null) {
        currentEventSource.close();
        currentEventSource = null;
    }

    const token = localStorage.getItem("authToken");
    if (!token) {
        alert("Not authenticated");
        input.disabled = false;
        return;
    }

    const url = `${BACKEND_URL}/api/ai/stream-story`;
        
        let aiBubble = null;
        let accumulatedText = "";
        let isError = false;
        let errorMessage = "";
        let newConversationId = null;
        let conversationName = null;

        fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            body: JSON.stringify({ 
                prompt: text,
                conversationId: window.activeConversationId || null
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server returned ${response.status}: ${response.statusText}`);
            }
            
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = "";
            
            function readStream() {
                reader.read().then(({ done, value }) => {
                    if (done) {
                        console.log("✅ Stream done. Total accumulated:", accumulatedText.length, "chars");
                        input.disabled = false;
                        
                        // After stream completes, update the active conversation and UI
                        if (newConversationId && !isError) {
                            window.activeConversationId = newConversationId;
                            
                            if (conversationName) {
                                document.getElementById("waHeaderName").textContent = conversationName;
                            }
                            
                            // Add the new conversation to the sidebar immediately
                            addConversationToSidebar(newConversationId, conversationName);
                            
                            console.log("✅ New conversation created:", newConversationId);
                        } else if (window.activeConversationId && !isError) {
                            // ✅ For existing conversations, move to top
                            moveConversationToTop(window.activeConversationId);
                        }
                        
                        if (isError) {
                            const errorDiv = document.createElement("div");
                            errorDiv.style.color = "#d32f2f";
                            errorDiv.style.background = "#ffebee";
                            errorDiv.style.padding = "15px";
                            errorDiv.style.borderRadius = "8px";
                            errorDiv.style.marginTop = "10px";
                            errorDiv.style.border = "1px solid #ef5350";
                            errorDiv.textContent = errorMessage;
                            document.getElementById("waMessages").appendChild(errorDiv);
                            
                            if (aiBubble && !accumulatedText.trim()) {
                                aiBubble.remove();
                            }
                        } else if (aiBubble && accumulatedText.trim()) {
                            const completeMsg = document.createElement("div");
                            completeMsg.style.color = "#4caf50";
                            completeMsg.style.marginTop = "10px";
                            completeMsg.style.fontSize = "0.9em";
                            completeMsg.textContent = "✓ Response complete";
                            document.getElementById("waMessages").appendChild(completeMsg);
                        }
                        return;
                    }
                    
                    const chunk = decoder.decode(value, { stream: true });
                    console.log("📦 Raw chunk received:", chunk.substring(0, 100));
                    buffer += chunk;
                    
                    const messages = buffer.split('\n\n');
                    console.log("📨 Messages in buffer:", messages.length);
                    
                    buffer = messages.pop() || "";
                    
                    messages.forEach(message => {
                        if (!message.trim()) return;
                        
                        const lines = message.split('\n');
                        lines.forEach(line => {
                            const trimmedLine = line.trim();
                            if (!trimmedLine) return;
                            
                            let data;
                            if (trimmedLine.startsWith('data:')) {
                                data = trimmedLine.substring(5).trim();
                            } else {
                                data = trimmedLine;
                            }
                            
                            if (!data) return;
                            
                            console.log("📝 Data extracted:", data.substring(0, 50));
                            
                            // Handle CONVERSATION_ID
                            if (data.startsWith('[CONVERSATION_ID:')) {
                                const match = data.match(/\[CONVERSATION_ID:([^\]]+)\]/);
                                if (match) {
                                    newConversationId = match[1];
                                    console.log("🆔 Received conversation ID:", newConversationId);
                                }
                                return;
                            }
                            
                            // Handle CONVERSATION_NAME
                            if (data.startsWith('[CONVERSATION_NAME:')) {
                                const match = data.match(/\[CONVERSATION_NAME:([^\]]+)\]/);
                                if (match) {
                                    conversationName = match[1];
                                    console.log("📝 Received conversation name:", conversationName);
                                }
                                return;
                            }
                            
                            if (data === '[ERROR_START]') {
                                isError = true;
                                errorMessage = "";
                                return;
                            }
                            
                            if (data === '[ERROR_END]') {
                                return;
                            }
                            
                            if (isError) {
                                errorMessage += data + ' ';
                                return;
                            }
                            
                            accumulatedText += data;

                            if (!aiBubble) {
                                console.log("🎨 Creating AI bubble");
                                aiBubble = document.createElement("div");
                                aiBubble.className = "wa-bubble wa-them";
                                document.getElementById("waMessages").appendChild(aiBubble);
                            }

                            aiBubble.textContent = accumulatedText;
                            const messagesBox = document.getElementById("waMessages");
                            messagesBox.scrollTop = messagesBox.scrollHeight;
                        });
                    });
                    
                    readStream();
                })
                .catch(error => {
                    console.error('Stream reading error:', error);
                    input.disabled = false;
                    
                    const errorDiv = document.createElement("div");
                    errorDiv.style.color = "#d32f2f";
                    errorDiv.style.background = "#ffebee";
                    errorDiv.style.padding = "15px";
                    errorDiv.style.borderRadius = "8px";
                    errorDiv.style.marginTop = "10px";
                    errorDiv.style.border = "1px solid #ef5350";
                    errorDiv.textContent = "⚠️ Connection lost. Please try again.";
                    document.getElementById("waMessages").appendChild(errorDiv);
                    
                    if (aiBubble && !accumulatedText.trim()) {
                        aiBubble.remove();
                    }
                });
            }
            
            readStream();
        })
        .catch(error => {
            console.error('Fetch error:', error);
            input.disabled = false;
            
            const errorDiv = document.createElement("div");
            errorDiv.style.color = "#d32f2f";
            errorDiv.style.background = "#ffebee";
            errorDiv.style.padding = "15px";
            errorDiv.style.borderRadius = "8px";
            errorDiv.style.marginTop = "10px";
            errorDiv.style.border = "1px solid #ef5350";
            errorDiv.textContent = error.message.includes('401') || error.message.includes('403') 
                ? "🔒 Authentication failed. Please login again." 
                : "⚠️ Failed to connect to AI service. Please try again later.";
            document.getElementById("waMessages").appendChild(errorDiv);
        });
}

// Function to add conversation to sidebar instantly
function addConversationToSidebar(conversationId, conversationName) {
    const list = document.getElementById("aiConversationList");
    if (!list) return;

    // Check if conversation already exists in the list
    const existingItem = list.querySelector(`[data-conv-id="${conversationId}"]`);
    if (existingItem) {
        // Move existing item to top (after "New Chat")
        const newChatItem = list.querySelector('li'); // First item is "New Chat"
        if (newChatItem && newChatItem.nextSibling) {
            list.insertBefore(existingItem, newChatItem.nextSibling);
        }
        return;
    }

    // Create new conversation item
    const li = document.createElement("li");
    li.dataset.convId = conversationId;
    li.className = "px-6 py-3 hover:bg-white/5 cursor-pointer flex items-center gap-4 min-w-0";

    const name = document.createElement("span");
    name.textContent = conversationName || "Gemini AI";
    name.className = "flex-1 text-white/90 font-medium truncate";

    const badge = document.createElement("span");
    badge.className = "conv-unread-badge";

    li.append(name, badge);
    li.onclick = () => {
        badge.classList.remove("show");
        badge.textContent = "";
        openConversation(conversationName, conversationId);
    };

    // Insert after "New Chat" item (first item in the list)
    const newChatItem = list.querySelector('li');
    if (newChatItem && newChatItem.nextSibling) {
        list.insertBefore(li, newChatItem.nextSibling);
    } else {
        list.appendChild(li);
    }

    console.log("✅ Added conversation to sidebar:", conversationId);
}

// Move existing conversation to top
function moveConversationToTop(conversationId) {
    const list = document.getElementById("aiConversationList");
    if (!list) return;

    // Find the conversation item
    const conversationItem = list.querySelector(`[data-conv-id="${conversationId}"]`);
    if (!conversationItem) {
        console.warn("⚠️ Conversation not found in sidebar:", conversationId);
        return;
    }

    // Find "New Chat" button (first item)
    const newChatItem = list.querySelector('li');
    
    // Move conversation right after "New Chat"
    if (newChatItem && newChatItem.nextSibling !== conversationItem) {
        list.insertBefore(conversationItem, newChatItem.nextSibling);
        console.log("📌 Moved conversation to top:", conversationId);
    }
}

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
    const token = localStorage.getItem("authToken");
    const box = document.getElementById("waMessages");

    try {
        const res = await fetch(
            `${BACKEND_URL}/api/conversation/ai/${conversationId}/messages?page=${messagePage}&size=${MESSAGE_PAGE_SIZE}`,
            { headers: { Authorization: `Bearer ${token}` } }
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
            div.className = `wa-bubble wa-${m.senderName === "user" ? "you" : "them"}`;
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
    new Audio("/sounds/notification.mp3").play().catch(() => {});
}