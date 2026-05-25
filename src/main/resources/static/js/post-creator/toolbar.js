import { featureSupport, getMessages } from './config.js';

export function applyWrap(contentArea, open, close, fallbackText, updatePreviewCallback) {
    const start = contentArea.selectionStart;
    const end = contentArea.selectionEnd;
    const selectedText = contentArea.value.substring(start, end);
    const inner = selectedText || fallbackText;
    const output = open + inner + close;

    contentArea.setRangeText(output, start, end, "end");
    const caretStart = start + open.length;
    const caretEnd = caretStart + inner.length;
    contentArea.focus();
    contentArea.setSelectionRange(caretStart, caretEnd);
    updatePreviewCallback();
}

export function applyLinePrefix(contentArea, prefix, updatePreviewCallback) {
    const start = contentArea.selectionStart;
    const end = contentArea.selectionEnd;
    const selectedText = contentArea.value.substring(start, end);
    const textToFormat = selectedText || "quoted text";
    const lines = textToFormat.split("\n").map(line => prefix + line).join("\n");
    contentArea.setRangeText(lines, start, end, "end");
    contentArea.focus();
    updatePreviewCallback();
}

export function applyLink(contentArea, updatePreviewCallback) {
    const url = prompt("Enter URL", "https://");
    if (!url) return;
    const start = contentArea.selectionStart;
    const end = contentArea.selectionEnd;
    const selectedText = contentArea.value.substring(start, end) || "link text";
    const output = `[${selectedText}](${url})`;
    contentArea.setRangeText(output, start, end, "end");
    contentArea.focus();
    updatePreviewCallback();
}

export function applyHashtag(contentArea, updatePreviewCallback) {
    const tag = prompt("Hashtag (without #)", "news");
    if (!tag) return;
    const cleaned = tag.replace(/\s+/g, "");
    const start = contentArea.selectionStart;
    contentArea.setRangeText(`#${cleaned} `, start, start, "end");
    contentArea.focus();
    updatePreviewCallback();
}

export function applyMention(contentArea, updatePreviewCallback) {
    const username = prompt("Username (without @)", "username");
    if (!username) return;
    const cleaned = username.replace(/\s+/g, "");
    const start = contentArea.selectionStart;
    contentArea.setRangeText(`@${cleaned} `, start, start, "end");
    contentArea.focus();
    updatePreviewCallback();
}

export function applyFeature(contentArea, feature, updatePreviewCallback) {
    switch (feature) {
        case "bold":
            applyWrap(contentArea, "**", "**", "bold text", updatePreviewCallback);
            break;
        case "italic":
            applyWrap(contentArea, "*", "*", "italic text", updatePreviewCallback);
            break;
        case "underline":
            applyWrap(contentArea, "__", "__", "underlined text", updatePreviewCallback);
            break;
        case "strikethrough":
            applyWrap(contentArea, "~~", "~~", "strikethrough text", updatePreviewCallback);
            break;
        case "spoiler":
            applyWrap(contentArea, "||", "||", "hidden text", updatePreviewCallback);
            break;
        case "code":
            applyWrap(contentArea, "`", "`", "code", updatePreviewCallback);
            break;
        case "quote":
            applyLinePrefix(contentArea, "> ", updatePreviewCallback);
            break;
        case "link":
            applyLink(contentArea, updatePreviewCallback);
            break;
        case "hashtag":
            applyHashtag(contentArea, updatePreviewCallback);
            break;
        case "mention":
            applyMention(contentArea, updatePreviewCallback);
            break;
    }
}

export function updateToolbarAvailability(selectedPlatforms, toolbarElement, hintElement) {
    if (!toolbarElement) return;

    const hasTelegram = selectedPlatforms.some(v => v.startsWith("TELEGRAM"));
    const hasDiscord = selectedPlatforms.some(v => v.startsWith("DISCORD"));
    const hasWhatsapp = selectedPlatforms.some(v => v.startsWith("WHATSAPP"));

    const count = (hasTelegram ? 1 : 0) + (hasDiscord ? 1 : 0) + (hasWhatsapp ? 1 : 0);
    const messages = getMessages();

    let modeText = messages.noPlatform;
    if (count > 1) {
        modeText = messages.mixedMode;
    } else if (hasTelegram) {
        modeText = messages.telegramMode;
    } else if (hasDiscord) {
        modeText = messages.discordMode;
    } else if (hasWhatsapp) {
        modeText = messages.whatsappMode;
    }

    toolbarElement.querySelectorAll(".format-btn").forEach(button => {
        const feature = button.dataset.feature;
        const support = featureSupport[feature];
        let enabled = true;

        if (count > 1) {
            enabled = (!hasTelegram || support.telegram) && 
                      (!hasDiscord || support.discord) && 
                      (!hasWhatsapp || support.whatsapp);
        } else if (hasTelegram) {
            enabled = support.telegram;
        } else if (hasDiscord) {
            enabled = support.discord;
        } else if (hasWhatsapp) {
            enabled = support.whatsapp;
        }

        button.disabled = !enabled;
        button.classList.toggle("is-disabled", !enabled);
    });

    if (hintElement) {
        if (count === 1) {
            hintElement.textContent = modeText + messages.formattingSuffix;
        } else if (count > 1) {
            hintElement.textContent = modeText + messages.sharedSuffix;
        } else {
            hintElement.textContent = modeText + messages.defaultSuffix;
        }
    }
}
