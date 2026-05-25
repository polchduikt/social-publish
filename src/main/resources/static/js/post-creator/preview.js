import { platformLimits, getMessages } from './config.js';

export function escapeHtml(value) {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

export function renderMarkdownForPreview(rawText) {
    const messages = getMessages();
    if (!rawText || !rawText.trim()) {
        return messages.previewPlaceholder;
    }

    let safe = escapeHtml(rawText);
    safe = safe.replace(/\r\n/g, "\n");
    safe = safe.replace(/^&gt; (.+)$/gm, "<blockquote>$1</blockquote>");
    safe = safe.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
    safe = safe.replace(/__(.+?)__/g, "<u>$1</u>");
    safe = safe.replace(/\*(.+?)\*/g, "<em>$1</em>");
    safe = safe.replace(/~~(.+?)~~/g, "<del>$1</del>");
    safe = safe.replace(/\|\|(.+?)\|\|/g, '<span class="preview-spoiler">$1</span>');
    safe = safe.replace(/`([^`]+?)`/g, "<code>$1</code>");
    safe = safe.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    safe = safe.replace(/(^|[\s])#([A-Za-z0-9_]+)/g, '$1<span class="preview-tag">#$2</span>');
    safe = safe.replace(/(^|[\s])@([A-Za-z0-9_]+)/g, '$1<span class="preview-mention">@$2</span>');
    safe = safe.replace(/\n/g, "<br>");

    return safe;
}

export function buildMediaCard(src, index) {
    return `<figure class="preview-media-card"><img src="${src}" alt="Selected media ${index + 1}"></figure>`;
}

export function renderPreviewMedia(files, existingMediaGridSelector, targets) {
    let combinedHtml = "";
    files.forEach((file, index) => {
        combinedHtml += buildMediaCard(URL.createObjectURL(file), index);
    });

    const existingGrid = document.querySelector(existingMediaGridSelector);
    if (existingGrid) {
        existingGrid.querySelectorAll(".existing-media-item").forEach((item, index) => {
            const img = item.querySelector("img");
            const src = img ? img.src : "";
            combinedHtml += buildMediaCard(src, index + files.length);
        });
    }

    targets.forEach(target => {
        if (target) target.innerHTML = combinedHtml;
    });
}

export function updateCharacterLimit(selectedPlatforms, contentArea, maxCharCountEl, charCountEl) {
    let minLimit = platformLimits.DEFAULT;

    if (selectedPlatforms.length > 0) {
        selectedPlatforms.forEach(val => {
            const platform = val.split(":")[0];
            const limit = platformLimits[platform] || platformLimits.DEFAULT;
            if (limit < minLimit) {
                minLimit = limit;
            }
        });
    }

    if (maxCharCountEl) {
        maxCharCountEl.textContent = minLimit;
    }
    if (contentArea) {
        contentArea.maxLength = minLimit;
    }

    updateCharCountColor(contentArea, maxCharCountEl, charCountEl);
}

export function updateCharCountColor(contentArea, maxCharCountEl, charCountEl) {
    if (!charCountEl || !contentArea || !maxCharCountEl) return;
    const current = contentArea.value.length;
    const max = parseInt(maxCharCountEl.textContent, 10);

    if (current > max) {
        charCountEl.classList.add("text-danger");
        charCountEl.classList.remove("text-warning");
    } else if (current > max * 0.9) {
        charCountEl.classList.add("text-warning");
        charCountEl.classList.remove("text-danger");
    } else {
        charCountEl.classList.remove("text-warning", "text-danger");
    }
}

export function resolvePreviewMode(selectedPlatforms) {
    if (selectedPlatforms.some(v => v.startsWith("DISCORD"))) return "discord";
    if (selectedPlatforms.some(v => v.startsWith("SLACK"))) return "slack";
    if (selectedPlatforms.some(v => v.startsWith("LINKEDIN"))) return "linkedin";
    if (selectedPlatforms.some(v => v.startsWith("NOTION"))) return "notion";
    return "telegram";
}

export function setPreviewMode(mode, cards, tabs) {
    if (cards.telegram) cards.telegram.classList.toggle("is-hidden", mode !== "telegram");
    if (cards.discord) cards.discord.classList.toggle("is-hidden", mode !== "discord");
    if (cards.slack) cards.slack.classList.toggle("is-hidden", mode !== "slack");
    if (cards.linkedin) cards.linkedin.classList.toggle("is-hidden", mode !== "linkedin");
    if (cards.notion) cards.notion.classList.toggle("is-hidden", mode !== "notion");

    tabs.forEach(tab => {
        tab.classList.toggle("is-active", tab.dataset.previewPlatform === mode);
    });
}
