
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

        // An expired session redirects to the HTML login page instead of returning JSON.
        if (response.redirected && new URL(response.url).pathname === "/login") {
            throw new Error("Your session has expired. Please refresh the page and log in again.");
        }

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

            // Show the list first: a hidden list has no height, so it could not scroll to the last message.
            chatEmptyState.style.display = "none";
            messagesContainer.style.display = "flex";
            displayMessages(conversation.messages || []);

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
                message.role === "USER" ? "You" : "DocuMind";

            const content = document.createElement("div");
            content.className = "message-content";

            const sourceNumbers = new Set((message.sources || []).map(source => source.number));
            if (message.role === "ASSISTANT" && sourceNumbers.size > 0) {
                renderWithCitations(content, message.content, sourceNumbers, message.id);
            } else {
                content.textContent = message.content;
            }

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
                    sourceElement.id = sourceElementId(message.id, source.number);

                    // Same number chip as the citation in the answer, so the two are easy to match.
                    const summary = document.createElement("summary");

                    const number = document.createElement("span");
                    number.className = "source-number";
                    number.textContent = String(source.number);

                    const title = document.createElement("span");
                    title.className = "source-title";
                    title.textContent = source.documentTitle;

                    const passage = document.createElement("span");
                    passage.className = "source-passage";
                    passage.textContent = `Passage ${source.chunkIndex + 1}`;

                    summary.append(number, title, passage);

                    const sourceContent = document.createElement("p");
                    sourceContent.textContent = source.content;

                    const footer = document.createElement("div");
                    footer.className = "source-footer";

                    const score = document.createElement("small");
                    score.textContent = `${Math.round(Number(source.score) * 100)}% match`;

                    const open = document.createElement("a");
                    open.className = "link-button";
                    open.href = `/documents/${encodeURIComponent(source.documentId)}`;
                    open.textContent = "Open document";

                    footer.append(score, open);
                    sourceElement.append(summary, sourceContent, footer);
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

        if (!question || isSending) {
            return;
        }

        let temporaryMessage = null;
        let loading = null;

        try {
            isSending = true;
            sendMessageBtn.disabled = true;
            messageInput.disabled = true;
            removeChatError();

            // No conversation selected yet: start one, so the first question is never lost.
            if (!currentConversationId) {
                const conversation = await apiRequest("/assistant/conversations", {
                    method: "POST",
                    body: JSON.stringify({})
                });
                currentConversationId = conversation.id;
                messagesContainer.innerHTML = "";
                chatEmptyState.style.display = "none";
                messagesContainer.style.display = "flex";
            }

            const conversationId = currentConversationId;

            // Show the user's question immediately
            const temporaryMessages = Array.from(
                messagesContainer.querySelectorAll(".message")
            );

            temporaryMessage = document.createElement("article");
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

            loading = document.createElement("article");
            loading.className = "message message-assistant loading-message";
            loading.textContent = "Reading your documents…";
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
            loading?.remove();
            temporaryMessage?.remove();
            messageInput.value = question;   // give the question back so it can be sent again
            showChatError(error.message || "Unable to send your message. Please try again.");
            await loadConversations();
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

    // Show an error inside the chat instead of a browser alert
    function showChatError(message) {
        removeChatError();
        const error = document.createElement("div");
        error.className = "chat-error";
        error.id = "chatError";
        error.setAttribute("role", "alert");
        error.textContent = message;
        chatForm.before(error);
    }

    function removeChatError() {
        document.getElementById("chatError")?.remove();
    }

    // Event listeners
    newConversationBtn.addEventListener("click", createConversation);
    chatForm.addEventListener("submit", sendMessage);

    // Enter sends, Shift+Enter adds a new line
    messageInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            chatForm.requestSubmit();
        }
    });

    // Initial loading
    messagesContainer.style.display = "none";

    // Links from other pages: /assistant?conversation=12 (History) and /assistant?ask=... (Search)
    const params = new URLSearchParams(window.location.search);
    const conversationParam = Number(params.get("conversation"));
    const askParam = params.get("ask");

    if (askParam) {
        messageInput.value = askParam.slice(0, 2000);
        messageInput.focus();
    }

    // Suggested questions in the empty chat: a click asks it.
    document.querySelectorAll(".chat-suggestions .suggestion").forEach((button) => {
        button.addEventListener("click", () => {
            messageInput.value = button.dataset.question;
            chatForm.requestSubmit();
        });
    });

    if (Number.isInteger(conversationParam) && conversationParam > 0) {
        selectConversation(conversationParam);
    } else {
        loadConversations();
    }

    // From the dashboard (?ask=...&send=1): the person already pressed Ask, so send it now.
    // The URL is cleaned first, so reloading the page does not send the question twice.
    if (askParam && params.get("send") === "1") {
        window.history.replaceState(null, "", window.location.pathname);
        chatForm.requestSubmit();
    }
});

/* =========================================
   CITATIONS: turn "[1]" and "[1, 3]" in an answer into buttons
   that open and highlight the matching source below the answer.
   Built with text nodes only, never innerHTML, so an answer can't inject HTML.
========================================= */

const CITATION = /\[(\d+(?:\s*,\s*\d+)*)\]/g;

function sourceElementId(messageId, number) {
    return `message-${messageId}-source-${number}`;
}

function renderWithCitations(container, text, sourceNumbers, messageId) {
    let last = 0;

    for (const match of text.matchAll(CITATION)) {
        const numbers = match[1].split(",").map(n => Number(n.trim()));

        // Leave "[7]" as plain text when there is no source 7 (the LLM invented it).
        if (!numbers.some(n => sourceNumbers.has(n))) {
            continue;
        }

        container.append(document.createTextNode(text.slice(last, match.index)));

        // "[1, 3]" becomes two chips side by side; the chip replaces the brackets.
        const group = document.createElement("span");
        group.className = "citation-group";

        numbers.forEach((number) => {
            if (sourceNumbers.has(number)) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "citation";
                button.textContent = String(number);
                button.title = `Show source ${number}`;
                button.addEventListener("click", () => openSource(messageId, number));
                group.append(button);
            }
            // A number without a source (invented by the LLM) is left out of the group.
        });

        container.append(group);
        last = match.index + match[0].length;
    }

    container.append(document.createTextNode(text.slice(last)));
}

function openSource(messageId, number) {
    const source = document.getElementById(sourceElementId(messageId, number));
    if (!source) return;

    source.open = true;
    source.scrollIntoView({ behavior: "smooth", block: "nearest" });
    source.classList.remove("source-highlight");
    void source.offsetWidth;            // restart the animation if clicked twice
    source.classList.add("source-highlight");
}
