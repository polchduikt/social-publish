import { HISTORY_KEY, STATE_KEY, MAX_HISTORY, getI18n } from './ai-assistant/config.js';
import { inferIntentFromText } from './ai-assistant/parser.js';
import { applyIntent } from './ai-assistant/intent.js';
import { 
    shouldSuppressBoilerplateReply, 
    looksLikeMessagePlacementChoice, 
    looksLikeChatPlacementChoice, 
    looksLikeAffirmative, 
    applyToPostCreatorMessage 
} from './ai-assistant/ui.js';

(function () {
    const root = document.getElementById("ai-assistant-root");
    if (!root || root.dataset.bound === "true") {
        return;
    }
    root.dataset.bound = "true";

    const styleExists = document.querySelector('link[data-ai-assistant-style="1"]');
    if (!styleExists) {
        const styleLink = document.createElement("link");
        styleLink.rel = "stylesheet";
        styleLink.href = "/css/ai-assistant.css";
        styleLink.setAttribute("data-ai-assistant-style", "1");
        styleLink.onload = () => {
            root.classList.add("is-ready");
            root.style.display = "";
        };
        document.head.appendChild(styleLink);
    } else {
        root.classList.add("is-ready");
        root.style.display = "";
    }

    const toggleButton = document.getElementById("ai-assistant-toggle");
    const panel = document.getElementById("ai-assistant-panel");
    const closeButton = document.getElementById("ai-assistant-close");
    const clearButton = document.getElementById("ai-assistant-clear");
    const form = document.getElementById("ai-assistant-form");
    const input = document.getElementById("ai-assistant-input");
    const messagesEl = document.getElementById("ai-assistant-messages");

    if (!toggleButton || !panel || !form || !input || !messagesEl) {
        return;
    }

    const endpoint = root.dataset.endpoint || "/api/ai-assistant/chat";
    const csrfToken = root.dataset.csrfToken || "";
    const csrfHeader = root.dataset.csrfHeader || "";

    let pendingGeneratedText = "";
    let pendingIntent = null;
    let waitingForPlacement = false;
    let history = [];
    const i18n = getI18n();

    function t(key) {
        return i18n[key] || key;
    }

    function safeReadJson(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            if (!raw) return fallback;
            return JSON.parse(raw);
        } catch (e) {
            return fallback;
        }
    }

    function saveState() {
        try {
            localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
            localStorage.setItem(STATE_KEY, JSON.stringify({
                waitingForPlacement,
                pendingGeneratedText,
                pendingIntent
            }));
        } catch (e) {}
    }

    function renderMessage(role, text) {
        const message = document.createElement("div");
        message.className = `ai-msg ai-msg-${role}`;
        message.textContent = text;
        messagesEl.appendChild(message);
    }

    function addMessage(role, text) {
        if (!text) return;
        const normalized = String(text).trim();
        if (!normalized) return;
        history.push({ role, text: normalized });
        if (history.length > MAX_HISTORY) {
            history = history.slice(history.length - MAX_HISTORY);
        }
        renderMessage(role, normalized);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        saveState();
    }

    function addTyping() {
        const typing = document.createElement("div");
        typing.className = "ai-msg ai-msg-assistant ai-msg-typing";
        typing.textContent = t("typing");
        typing.id = "ai-assistant-typing";
        messagesEl.appendChild(typing);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function removeTyping() {
        const typing = document.getElementById("ai-assistant-typing");
        if (typing) typing.remove();
    }

    function togglePanel(open) {
        const shouldOpen = typeof open === "boolean" ? open : panel.classList.contains("is-hidden");
        panel.classList.toggle("is-hidden", !shouldOpen);
        if (shouldOpen) input.focus();
    }

    function handlePlacementToMessage(generatedText, intent) {
        if (applyToPostCreatorMessage(generatedText)) {
            addMessage("assistant", t("inserted"));
        } else {
            addMessage("assistant", t("noMessageField"));
            addMessage("assistant", generatedText);
        }
        applyIntent(intent);
    }

    function clearPendingState() {
        waitingForPlacement = false;
        pendingGeneratedText = "";
        pendingIntent = null;
        saveState();
    }

    function tryHandlePlacementChoice(userText) {
        if (!waitingForPlacement || !pendingGeneratedText) return false;

        if (looksLikeMessagePlacementChoice(userText) || looksLikeAffirmative(userText)) {
            handlePlacementToMessage(pendingGeneratedText, pendingIntent);
            clearPendingState();
            return true;
        }

        if (looksLikeChatPlacementChoice(userText)) {
            addMessage("assistant", pendingGeneratedText);
            applyIntent(pendingIntent);
            clearPendingState();
            return true;
        }

        return false;
    }

    async function sendToAssistant(userText, intent) {
        addTyping();
        const body = {
            message: userText,
            currentPostMessage: (document.getElementById("content") || {}).value || ""
        };
        const headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers,
                credentials: "same-origin",
                body: JSON.stringify(body)
            });
            removeTyping();

            if (!response.ok) {
                addMessage("assistant", t("requestFailed"));
                return;
            }

            const data = await response.json();
            const reply = (data.reply || "").trim();
            const generatedText = (data.generatedText || "").trim();
            const needsPlacementChoice = !!data.needsPlacementChoice;

            if (reply && !shouldSuppressBoilerplateReply(reply, generatedText)) {
                addMessage("assistant", reply);
            }

            if (generatedText && needsPlacementChoice) {
                if (looksLikeMessagePlacementChoice(userText)) {
                    handlePlacementToMessage(generatedText, intent);
                    return;
                }
                if (looksLikeChatPlacementChoice(userText)) {
                    addMessage("assistant", generatedText);
                    applyIntent(intent);
                    return;
                }

                pendingGeneratedText = generatedText;
                pendingIntent = intent;
                waitingForPlacement = true;
                saveState();
                addMessage("assistant", t("askPlacement"));
            } else {
                applyIntent(intent);
            }
        } catch (e) {
            removeTyping();
            addMessage("assistant", t("networkError"));
        }
    }

    function restoreFromStorage() {
        const savedHistory = safeReadJson(HISTORY_KEY, []);
        if (Array.isArray(savedHistory)) {
            history = savedHistory
                .filter(item => item && item.role && item.text)
                .slice(-MAX_HISTORY);
        } else {
            history = [];
        }

        const savedState = safeReadJson(STATE_KEY, null);
        if (savedState && typeof savedState === "object") {
            waitingForPlacement = !!savedState.waitingForPlacement;
            pendingGeneratedText = typeof savedState.pendingGeneratedText === "string" ? savedState.pendingGeneratedText : "";
            pendingIntent = savedState.pendingIntent && typeof savedState.pendingIntent === "object" ? savedState.pendingIntent : null;
        }

        messagesEl.innerHTML = "";
        if (history.length) {
            history.forEach(entry => {
                renderMessage(entry.role, entry.text);
            });
            messagesEl.scrollTop = messagesEl.scrollHeight;
        } else {
            addMessage("assistant", t("greeting"));
        }
    }

    function clearChat() {
        history = [];
        messagesEl.innerHTML = "";
        clearPendingState();
        try {
            localStorage.removeItem(HISTORY_KEY);
            localStorage.removeItem(STATE_KEY);
        } catch (e) {}
        renderMessage("assistant", t("clearDone"));
        addMessage("assistant", t("greeting"));
    }

    toggleButton.addEventListener("click", () => {
        togglePanel();
    });

    if (closeButton) {
        closeButton.addEventListener("click", () => {
            togglePanel(false);
        });
    }

    if (clearButton) {
        clearButton.addEventListener("click", () => {
            clearChat();
        });
    }

    form.addEventListener("submit", event => {
        event.preventDefault();
        const userText = input.value.trim();
        if (!userText) return;

        addMessage("user", userText);
        input.value = "";

        if (tryHandlePlacementChoice(userText)) return;

        const intent = inferIntentFromText(userText);
        sendToAssistant(userText, intent);
    });

    input.addEventListener("keydown", event => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    restoreFromStorage();
})();
