(function () {
    if (!document.querySelector('link[data-ai-assistant-style="1"]')) {
        var styleLink = document.createElement("link");
        styleLink.rel = "stylesheet";
        styleLink.href = "/css/ai-assistant.css";
        styleLink.setAttribute("data-ai-assistant-style", "1");
        document.head.appendChild(styleLink);
    }

    var root = document.getElementById("ai-assistant-root");
    if (!root || root.dataset.bound === "true") {
        return;
    }
    root.dataset.bound = "true";

    var toggleButton = document.getElementById("ai-assistant-toggle");
    var panel = document.getElementById("ai-assistant-panel");
    var closeButton = document.getElementById("ai-assistant-close");
    var clearButton = document.getElementById("ai-assistant-clear");
    var form = document.getElementById("ai-assistant-form");
    var input = document.getElementById("ai-assistant-input");
    var messagesEl = document.getElementById("ai-assistant-messages");

    if (!toggleButton || !panel || !form || !input || !messagesEl) {
        return;
    }

    var HISTORY_KEY = "sp.aiAssistant.history.v1";
    var STATE_KEY = "sp.aiAssistant.state.v1";
    var MAX_HISTORY = 120;

    var endpoint = root.dataset.endpoint || "/api/ai-assistant/chat";
    var csrfToken = root.dataset.csrfToken || "";
    var csrfHeader = root.dataset.csrfHeader || "";

    var pendingGeneratedText = "";
    var pendingIntent = null;
    var waitingForPlacement = false;
    var history = [];

    function data(name) {
        return root.dataset[name] || "";
    }

    var i18n = {
        greeting: data("i18nGreeting"),
        typing: data("i18nTyping"),
        askPlacement: data("i18nAskPlacement"),
        inserted: data("i18nInserted"),
        noMessageField: data("i18nNoMessageField"),
        requestFailed: data("i18nRequestFailed"),
        networkError: data("i18nNetworkError"),
        clearDone: data("i18nClearDone")
    };

    function t(key) {
        return i18n[key] || key;
    }

    function normalizeText(value) {
        return (value || "")
            .normalize("NFKC")
            .toLowerCase()
            .replace(/\s+/g, " ")
            .trim();
    }

    function safeReadJson(key, fallback) {
        try {
            var raw = localStorage.getItem(key);
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
                waitingForPlacement: waitingForPlacement,
                pendingGeneratedText: pendingGeneratedText,
                pendingIntent: pendingIntent
            }));
        } catch (e) {
            // ignore storage errors
        }
    }

    function renderMessage(role, text) {
        var message = document.createElement("div");
        message.className = "ai-msg ai-msg-" + role;
        message.textContent = text;
        messagesEl.appendChild(message);
    }

    function addMessage(role, text) {
        if (!text) return;
        var normalized = String(text).trim();
        if (!normalized) return;
        history.push({ role: role, text: normalized });
        if (history.length > MAX_HISTORY) {
            history = history.slice(history.length - MAX_HISTORY);
        }
        renderMessage(role, normalized);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        saveState();
    }

    function addTyping() {
        var typing = document.createElement("div");
        typing.className = "ai-msg ai-msg-assistant ai-msg-typing";
        typing.textContent = t("typing");
        typing.id = "ai-assistant-typing";
        messagesEl.appendChild(typing);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function removeTyping() {
        var typing = document.getElementById("ai-assistant-typing");
        if (typing) typing.remove();
    }

    function shouldSuppressBoilerplateReply(reply, generatedText) {
        if (!generatedText) return false;
        var text = normalizeText(reply);
        if (!text) return false;
        return /^ось текст( поста)?/.test(text)
            || /^ось готовий текст/.test(text)
            || /^ось варіант/.test(text)
            || /^here('?s| is) (the )?text/.test(text)
            || /^here('?s| is) (a )?post/.test(text)
            || /^here('?s| is) your/.test(text);
    }

    function togglePanel(open) {
        var shouldOpen = typeof open === "boolean" ? open : panel.classList.contains("is-hidden");
        panel.classList.toggle("is-hidden", !shouldOpen);
        if (shouldOpen) input.focus();
    }

    function looksLikeMessagePlacementChoice(value) {
        var text = normalizeText(value);
        return [
            "message", "в message", "у message", "поле message", "в поле message",
            "insert into message", "paste in message", "в повідомлення", "у повідомлення"
        ].some(function (token) { return text.indexOf(token) !== -1; });
    }

    function looksLikeChatPlacementChoice(value) {
        var text = normalizeText(value);
        return ["chat", "в чат", "у чат", "тут", "here"].some(function (token) {
            return text.indexOf(token) !== -1;
        });
    }

    function looksLikeAffirmative(value) {
        var text = normalizeText(value);
        return ["так", "yes", "ok", "okay", "ок", "добре", "звісно"].some(function (token) {
            return text === token || text.indexOf(token + " ") === 0;
        });
    }

    function applyToPostCreatorMessage(text) {
        var contentField = document.getElementById("content");
        if (!contentField) return false;
        contentField.value = text;
        contentField.dispatchEvent(new Event("input", { bubbles: true }));
        contentField.focus();
        return true;
    }

    function toIsoLocal(dateValue) {
        var y = dateValue.getFullYear();
        var m = String(dateValue.getMonth() + 1).padStart(2, "0");
        var d = String(dateValue.getDate()).padStart(2, "0");
        var h = String(dateValue.getHours()).padStart(2, "0");
        var i = String(dateValue.getMinutes()).padStart(2, "0");
        return y + "-" + m + "-" + d + "T" + h + ":" + i;
    }

    function parseScheduleDate(text) {
        var normalized = normalizeText(text);
        var now = new Date();
        var target = null;

        var hour = 10;
        var minute = 0;
        var timeMatch = normalized.match(/(?:о|на|at)\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?/);
        if (timeMatch) {
            hour = parseInt(timeMatch[1], 10);
            minute = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
            if (timeMatch[3] === "pm" && hour < 12) hour += 12;
            if (timeMatch[3] === "am" && hour === 12) hour = 0;
        } else {
            var shortTimeMatch = normalized.match(/\b(\d{1,2}):(\d{2})\b/);
            if (shortTimeMatch) {
                hour = parseInt(shortTimeMatch[1], 10);
                minute = parseInt(shortTimeMatch[2], 10);
            }
        }

        if (normalized.indexOf("завтра") !== -1 || normalized.indexOf("tomorrow") !== -1) {
            target = new Date(now.getTime());
            target.setDate(now.getDate() + 1);
        } else if (normalized.indexOf("сьогодні") !== -1 || normalized.indexOf("today") !== -1) {
            target = new Date(now.getTime());
        }

        var monthMap = {
            "січня": 1, "січень": 1, "january": 1,
            "лютого": 2, "лютий": 2, "february": 2,
            "березня": 3, "березень": 3, "march": 3,
            "квітня": 4, "квітень": 4, "april": 4,
            "травня": 5, "травень": 5, "may": 5,
            "червня": 6, "червень": 6, "june": 6,
            "липня": 7, "липень": 7, "july": 7,
            "серпня": 8, "серпень": 8, "august": 8,
            "вересня": 9, "вересень": 9, "september": 9,
            "жовтня": 10, "жовтень": 10, "october": 10,
            "листопада": 11, "листопад": 11, "november": 11,
            "грудня": 12, "грудень": 12, "december": 12
        };

        var dayMonthMatch = normalized.match(/\b(\d{1,2})\s+([a-zа-яіїєґ]+)(?:\s+(\d{4}))?\b/);
        if (dayMonthMatch && monthMap[dayMonthMatch[2]]) {
            var day = parseInt(dayMonthMatch[1], 10);
            var month = monthMap[dayMonthMatch[2]];
            var year = dayMonthMatch[3] ? parseInt(dayMonthMatch[3], 10) : now.getFullYear();
            target = new Date(year, month - 1, day, hour, minute, 0, 0);
            if (!dayMonthMatch[3] && target.getTime() < now.getTime()) {
                target.setFullYear(target.getFullYear() + 1);
            }
        }

        var numericDateMatch = normalized.match(/\b(\d{1,2})[.\/-](\d{1,2})(?:[.\/-](\d{4}))?\b/);
        if (!target && numericDateMatch) {
            var dayNum = parseInt(numericDateMatch[1], 10);
            var monthNum = parseInt(numericDateMatch[2], 10);
            var yearNum = numericDateMatch[3] ? parseInt(numericDateMatch[3], 10) : now.getFullYear();
            target = new Date(yearNum, monthNum - 1, dayNum, hour, minute, 0, 0);
            if (!numericDateMatch[3] && target.getTime() < now.getTime()) {
                target.setFullYear(target.getFullYear() + 1);
            }
        }

        if (!target) return "";
        target.setHours(hour, minute, 0, 0);
        return toIsoLocal(target);
    }

    function inferIntentFromText(userText) {
        var text = normalizeText(userText);
        var platformTypes = [];

        var platformMatchers = [
            { type: "TELEGRAM", pattern: /(telegram|телеграм)/ },
            { type: "DISCORD", pattern: /(discord|дискорд)/ },
            { type: "SLACK", pattern: /(slack|слак)/ },
            { type: "LINKEDIN", pattern: /(linkedin|лінкедін|линкедин)/ },
            { type: "NOTION", pattern: /(notion|ноушн|нотіон)/ }
        ];

        platformMatchers.forEach(function (item) {
            if (item.pattern.test(text)) platformTypes.push(item.type);
        });

        var accountHint = "";
        var accountMatch = text.match(/\b(?:для|for)\s+([a-zа-яіїєґ0-9 _-]{2,30})/);
        if (accountMatch) {
            accountHint = accountMatch[1].split(/(?:\bна\b|\bо\b|\bat\b|,|\.|!|\?)/)[0].trim();
        }

        return {
            platformTypes: platformTypes,
            accountHint: accountHint,
            scheduleAt: parseScheduleDate(text)
        };
    }

    function applyPlatformsFromIntent(intent) {
        var checkboxes = Array.prototype.slice.call(document.querySelectorAll('input[name="platforms"]'));
        if (!checkboxes.length) return false;
        if ((!intent.platformTypes || !intent.platformTypes.length) && !intent.accountHint) return false;

        var hint = normalizeText(intent.accountHint || "");
        var selectedCount = 0;
        checkboxes.forEach(function (checkbox) { checkbox.checked = false; });

        checkboxes.forEach(function (checkbox) {
            if (checkbox.disabled) return;
            var valueType = (checkbox.value || "").split(":")[0].toUpperCase();
            var labelEl = checkbox.closest("label") ? checkbox.closest("label").querySelector(".platform-chip-inner span:last-child") : null;
            var label = normalizeText(labelEl ? labelEl.textContent : "");
            var matchesType = !intent.platformTypes.length || intent.platformTypes.indexOf(valueType) !== -1;
            var matchesHint = !hint || label.indexOf(hint) !== -1;
            if (matchesType && matchesHint) {
                checkbox.checked = true;
                selectedCount++;
            }
        });

        if (selectedCount > 0) {
            checkboxes.forEach(function (checkbox) {
                checkbox.dispatchEvent(new Event("change", { bubbles: true }));
            });
            return true;
        }
        return false;
    }

    function applyScheduleFromIntent(scheduleAt) {
        if (!scheduleAt) return false;
        var scheduledInput = document.getElementById("scheduledAt");
        if (!scheduledInput) return false;

        var onceRadio = document.querySelector('input[name="recurring"][value="false"]');
        if (onceRadio && !onceRadio.checked) {
            onceRadio.checked = true;
            if (typeof window.updateScheduleMode === "function") window.updateScheduleMode(false);
            onceRadio.dispatchEvent(new Event("change", { bubbles: true }));
        }

        if (scheduledInput._flatpickr) {
            scheduledInput._flatpickr.setDate(scheduleAt, true, "Y-m-d\\TH:i");
        } else {
            scheduledInput.value = scheduleAt;
            scheduledInput.dispatchEvent(new Event("input", { bubbles: true }));
            scheduledInput.dispatchEvent(new Event("change", { bubbles: true }));
        }
        return true;
    }

    function applyIntent(intent) {
        if (!intent) return;
        applyPlatformsFromIntent(intent);
        applyScheduleFromIntent(intent.scheduleAt);
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
        var body = {
            message: userText,
            currentPostMessage: (document.getElementById("content") || {}).value || ""
        };
        var headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        try {
            var response = await fetch(endpoint, {
                method: "POST",
                headers: headers,
                credentials: "same-origin",
                body: JSON.stringify(body)
            });
            removeTyping();

            if (!response.ok) {
                addMessage("assistant", t("requestFailed"));
                return;
            }

            var data = await response.json();
            var reply = (data.reply || "").trim();
            var generatedText = (data.generatedText || "").trim();
            var needsPlacementChoice = !!data.needsPlacementChoice;

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
        var savedHistory = safeReadJson(HISTORY_KEY, []);
        if (Array.isArray(savedHistory)) {
            history = savedHistory
                .filter(function (item) { return item && item.role && item.text; })
                .slice(-MAX_HISTORY);
        } else {
            history = [];
        }

        var savedState = safeReadJson(STATE_KEY, null);
        if (savedState && typeof savedState === "object") {
            waitingForPlacement = !!savedState.waitingForPlacement;
            pendingGeneratedText = typeof savedState.pendingGeneratedText === "string" ? savedState.pendingGeneratedText : "";
            pendingIntent = savedState.pendingIntent && typeof savedState.pendingIntent === "object" ? savedState.pendingIntent : null;
        }

        messagesEl.innerHTML = "";
        if (history.length) {
            history.forEach(function (entry) {
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
        } catch (e) {
            // ignore storage errors
        }
        renderMessage("assistant", t("clearDone"));
        addMessage("assistant", t("greeting"));
    }

    toggleButton.addEventListener("click", function () {
        togglePanel();
    });

    if (closeButton) {
        closeButton.addEventListener("click", function () {
            togglePanel(false);
        });
    }

    if (clearButton) {
        clearButton.addEventListener("click", function () {
            clearChat();
        });
    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        var userText = input.value.trim();
        if (!userText) return;

        addMessage("user", userText);
        input.value = "";

        if (tryHandlePlacementChoice(userText)) return;

        var intent = inferIntentFromText(userText);
        sendToAssistant(userText, intent);
    });

    input.addEventListener("keydown", function (event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    restoreFromStorage();
})();
