
document.addEventListener("DOMContentLoaded", () => {

    // DOM elements
    const conversationsList = document.getElementById("conversationsList");
    const conversationCount = document.getElementById("conversationCount");
    const newConversationBtn = document.getElementById("newConversationBtn");
    const messagesContainer = document.getElementById("messagesContainer");
    const chatEmptyState = document.getElementById("chatEmptyState");
    const chatForm = document.getElementById("chatForm");
    const messageInput = document.getElementById("messageInput");
    const sendMessageBtn = document.getElementById("sendMessageBtn");

    // CSRF configuration
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let currentConversationId = null;
    let isSending = false;

    // Centralized HTTP requests
    async function apiRequest(url, options = {}) {
        const headers = {
            ...(options.headers || {})
        };

        if (options.body) {
            headers["Content-Type"] = "application/json";
        }

        if (csrfToken && csrfHeader && options.method &&
            !["GET", "HEAD", "OPTIONS"].includes(options.method.toUpperCase())) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || `HTTP error ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    }

    // Load the conversation list
    async function loadConversations() {
        try {
            const conversations = await apiRequest("/assistant/conversations");

            conversationsList.innerHTML = "";
            conversationCount.textContent = conversations.length;

            if (conversations.length === 0) {
                conversationsList.innerHTML = `
                    <div class="conversation-empty">
                        <p>No conversations yet.</p>
                    </div>
                `;
                return;
            }

            conversations.forEach(conversation => {
                const item = document.createElement("div");
                item.className = "conversation-item";

                if (conversation.id === currentConversationId) {
                    item.classList.add("active");
                }

                const title = document.createElement("button");
                title.type = "button";
                title.className = "conversation-title";
                title.textContent = conversation.title;

                title.addEventListener("click", () => {
                    selectConversation(conversation.id);
                });

                const deleteButton = document.createElement("button");
                deleteButton.type = "button";
                deleteButton.className = "delete-conversation-btn";
                deleteButton.textContent = "×";
                deleteButton.title = "Delete conversation";

                deleteButton.addEventListener("click", () => {
                    deleteConversation(conversation.id);
                });

                item.append(title, deleteButton);
                conversationsList.appendChild(item);
            });

        } catch (error) {
            console.error("Loading conversations failed:", error);
            conversationsList.innerHTML = `
                <div class="conversation-empty">
                    <p>Unable to load conversations.</p>
                </div>
            `;
        }
    }

    // Create a new conversation
    async function createConversation() {
        try {
            newConversationBtn.disabled = true;

            const conversation = await apiRequest("/assistant/conversations", {
                method: "POST",
                body: JSON.stringify({})
            });

            currentConversationId = conversation.id;

            await loadConversations();
            await selectConversation(conversation.id);

        } catch (error) {
            console.error("Creating conversation failed:", error);
            alert("Unable to create a conversation.");
        } finally {
            newConversationBtn.disabled = false;
        }
    }

    // Select and load a conversation
    async function selectConversation(conversationId) {
        try {
            currentConversationId = conversationId;

            const conversation = await apiRequest(
                `/assistant/conversations/${conversationId}`
            );

            displayMessages(conversation.messages || []);
            chatEmptyState.style.display = "none";
            messagesContainer.style.display = "flex";

            await loadConversations();

        } catch (error) {
            console.error("Loading conversation failed:", error);
            alert("Unable to load this conversation.");
        }
    }

    // Display messages and their sources
    function displayMessages(messages) {
        messagesContainer.innerHTML = "";

        messages.forEach(message => {
            const messageElement = document.createElement("article");
            messageElement.className =
                `message message-${String(message.role).toLowerCase()}`;

            const role = document.createElement("div");
            role.className = "message-role";
            role.textContent =
                message.role === "USER" ? "You" : "DocuMind AI";

            const content = document.createElement("div");
            content.className = "message-content";
            content.textContent = message.content;

            messageElement.append(role, content);

            if (message.sources && message.sources.length > 0) {
                const sourcesContainer = document.createElement("div");
                sourcesContainer.className = "message-sources";

                const sourcesTitle = document.createElement("h4");
                sourcesTitle.textContent = "Sources";
                sourcesContainer.appendChild(sourcesTitle);

                message.sources.forEach(source => {
                    const sourceElement = document.createElement("details");
                    sourceElement.className = "source-item";

                    const summary = document.createElement("summary");
                    summary.textContent =
                        `[${source.number}] ${source.documentTitle} — Chunk ${source.chunkIndex}`;

                    const sourceContent = document.createElement("p");
                    sourceContent.textContent = source.content;

                    const score = document.createElement("small");
                    score.textContent =
                        `Relevance score: ${Number(source.score).toFixed(3)}`;

                    sourceElement.append(summary, sourceContent, score);
                    sourcesContainer.appendChild(sourceElement);
                });

                messageElement.appendChild(sourcesContainer);
            }

            messagesContainer.appendChild(messageElement);
        });

        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Send a question to the assistant
    async function sendMessage(event) {
        event.preventDefault();

        const question = messageInput.value.trim();

        if (!question || !currentConversationId || isSending) {
            return;
        }

        try {
            isSending = true;
            sendMessageBtn.disabled = true;
            messageInput.disabled = true;

            const conversationId = currentConversationId;

            // Show the user's question immediately
            const temporaryMessages = Array.from(
                messagesContainer.querySelectorAll(".message")
            );

            const temporaryMessage = document.createElement("article");
            temporaryMessage.className = "message message-user";

            const role = document.createElement("div");
            role.className = "message-role";
            role.textContent = "You";

            const content = document.createElement("div");
            content.className = "message-content";
            content.textContent = question;

            temporaryMessage.append(role, content);
            messagesContainer.appendChild(temporaryMessage);

            messageInput.value = "";

            const loading = document.createElement("article");
            loading.className = "message message-assistant loading-message";
            loading.textContent = "DocuMind is thinking...";
            messagesContainer.appendChild(loading);

            messagesContainer.scrollTop = messagesContainer.scrollHeight;

            await apiRequest(
                `/assistant/conversations/${conversationId}/messages`,
                {
                    method: "POST",
                    body: JSON.stringify({ question })
                }
            );

            // Reload persisted messages, including the assistant answer
            await selectConversation(conversationId);

        } catch (error) {
            console.error("Sending message failed:", error);
            alert("Unable to send your message. Please try again.");
        } finally {
            isSending = false;
            sendMessageBtn.disabled = false;
            messageInput.disabled = false;
            messageInput.focus();
        }
    }

    // Delete a conversation
    async function deleteConversation(conversationId) {
        const confirmed = confirm(
            "Are you sure you want to delete this conversation?"
        );

        if (!confirmed) {
            return;
        }

        try {
            await apiRequest(`/assistant/conversations/${conversationId}`, {
                method: "DELETE"
            });

            if (currentConversationId === conversationId) {
                currentConversationId = null;
                messagesContainer.innerHTML = "";
                messagesContainer.style.display = "none";
                chatEmptyState.style.display = "flex";
            }

            await loadConversations();

        } catch (error) {
            console.error("Deleting conversation failed:", error);
            alert("Unable to delete this conversation.");
        }
    }

    // Event listeners
    newConversationBtn.addEventListener("click", createConversation);
    chatForm.addEventListener("submit", sendMessage);

    // Initial loading
    messagesContainer.style.display = "none";
    loadConversations();
});
