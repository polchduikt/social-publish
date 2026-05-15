(function () {
    flatpickr("#scheduledAt", {
        enableTime: true,
        dateFormat: "Y-m-d\\TH:i",
        altInput: true,
        altFormat: "Y-m-d H:i",
        time_24hr: true,
        minDate: "today",
        minuteIncrement: 1,
        defaultHour: new Date().getHours(),
        defaultMinute: new Date().getMinutes()
    });

    flatpickr("#recurringEndDate", {
        dateFormat: "Y-m-d\\TH:i",
        altInput: true,
        altFormat: "Y-m-d",
        minDate: "today"
    });

    var recurringOptions = document.getElementById("recurringOptions");
    var singleScheduleField = document.getElementById("singleScheduleField");

    window.updateScheduleMode = function(isRecurring) {
        if (isRecurring) {
            recurringOptions.classList.remove("is-hidden");
            if (singleScheduleField) singleScheduleField.classList.add("is-hidden");
        } else {
            recurringOptions.classList.add("is-hidden");
            if (singleScheduleField) singleScheduleField.classList.remove("is-hidden");
        }
        saveDraftDebounced();
    };

    var content = document.getElementById("content");
    var charCount = document.getElementById("charCount");
    var maxCharCount = document.getElementById("maxCharCount");
    var previewTelegramContent = document.getElementById("previewTelegramContent");
    var previewDiscordContent = document.getElementById("previewDiscordContent");
    var previewSlackContent = document.getElementById("previewSlackContent");
    var previewLinkedinContent = document.getElementById("previewLinkedinContent");
    var previewNotionContent = document.getElementById("previewNotionContent");

    var previewTelegramMedia = document.getElementById("previewTelegramMedia");
    var previewDiscordMedia = document.getElementById("previewDiscordMedia");
    var previewSlackMedia = document.getElementById("previewSlackMedia");
    var previewLinkedinMedia = document.getElementById("previewLinkedinMedia");
    var previewNotionMedia = document.getElementById("previewNotionMedia");

    var previewTelegramCard = document.getElementById("previewTelegram");
    var previewDiscordCard = document.getElementById("previewDiscord");
    var previewSlackCard = document.getElementById("previewSlack");
    var previewLinkedinCard = document.getElementById("previewLinkedin");
    var previewNotionCard = document.getElementById("previewNotion");
    var toolbar = document.getElementById("formatToolbar");
    var featureHint = document.getElementById("featureHint");
    var mediaInput = document.getElementById("mediaFiles");
    var mediaPreview = document.getElementById("mediaPreview");
    var mediaDropzone = document.getElementById("mediaDropzone");
    var existingMediaGrid = document.getElementById("existingMediaGrid");
    var removedMediaContainer = document.getElementById("removedMediaContainer");

    var creatorShell = document.querySelector(".creator-shell");
    if (!content || !toolbar || !creatorShell) {
        return;
    }

    var messages = {
        noPlatform: creatorShell.dataset.msgNoPlatform,
        mixedMode: creatorShell.dataset.msgMixedMode,
        telegramMode: creatorShell.dataset.msgTelegramMode,
        discordMode: creatorShell.dataset.msgDiscordMode,
        whatsappMode: creatorShell.dataset.msgWhatsappMode,
        formattingSuffix: creatorShell.dataset.msgFormattingSuffix,
        sharedSuffix: creatorShell.dataset.msgSharedSuffix,
        defaultSuffix: creatorShell.dataset.msgDefaultSuffix,
        previewPlaceholder: creatorShell.dataset.msgPreviewPlaceholder,
        autosaved: creatorShell.dataset.msgAutosaved,
        draftRestored: creatorShell.dataset.msgDraftRestored
    };

    var autosaveNotice = document.getElementById("autosave-notice");

    function showNotice(text) {
        if (!autosaveNotice) return;
        autosaveNotice.textContent = text;
        autosaveNotice.classList.add("show");
        setTimeout(function () {
            autosaveNotice.classList.remove("show");
        }, 3000);
    }

    var saveDraftDebounced = (function() {
        var interval = parseInt(localStorage.getItem("autosaveInterval") || "5000");
        if (interval === 0) return function() {};
        
        return debounce(function () {
            if (!DraftStore || window.location.pathname.includes("/edit")) return;
            
            var currentData = {
                content: content.value,
                platforms: getSelectedPlatforms(),
                files: collectedFiles
            };

            DraftStore.save("currentDraft", currentData).then(function () {
                showNotice(messages.autosaved);
            });
        }, interval);
    })();

    var collectedFiles = [];
    var dragSrcIndex = null;

    var featureSupport = {
        bold: { telegram: true, discord: true, whatsapp: true },
        italic: { telegram: true, discord: true, whatsapp: true },
        underline: { telegram: true, discord: true, whatsapp: false },
        strikethrough: { telegram: true, discord: true, whatsapp: true },
        spoiler: { telegram: true, discord: true, whatsapp: false },
        code: { telegram: true, discord: true, whatsapp: true },
        quote: { telegram: true, discord: true, whatsapp: false },
        link: { telegram: true, discord: true, whatsapp: true },
        hashtag: { telegram: true, discord: true, whatsapp: false },
        mention: { telegram: true, discord: true, whatsapp: false }
    };

    var platformLimits = {
        TELEGRAM: 4096,
        DISCORD: 2000,
        SLACK: 4000,
        LINKEDIN: 3000,
        NOTION: 2000,
        DEFAULT: 5000
    };

    function getSelectedPlatforms() {
        var result = [];
        document.querySelectorAll('input[name="platforms"]:checked').forEach(function (input) {
            result.push(input.value);
        });
        return result;
    }

    function applyWrap(open, close, fallbackText) {
        var start = content.selectionStart;
        var end = content.selectionEnd;
        var selectedText = content.value.substring(start, end);
        var inner = selectedText || fallbackText;
        var output = open + inner + close;

        content.setRangeText(output, start, end, "end");
        var caretStart = start + open.length;
        var caretEnd = caretStart + inner.length;
        content.focus();
        content.setSelectionRange(caretStart, caretEnd);
        updatePreview();
    }

    function applyLinePrefix(prefix) {
        var start = content.selectionStart;
        var end = content.selectionEnd;
        var selectedText = content.value.substring(start, end);
        var textToFormat = selectedText || "quoted text";
        var lines = textToFormat.split("\n").map(function (line) {
            return prefix + line;
        }).join("\n");
        content.setRangeText(lines, start, end, "end");
        content.focus();
        updatePreview();
    }

    function applyLink() {
        var url = prompt("Enter URL", "https://");
        if (!url) {
            return;
        }
        var start = content.selectionStart;
        var end = content.selectionEnd;
        var selectedText = content.value.substring(start, end) || "link text";
        var output = "[" + selectedText + "](" + url + ")";
        content.setRangeText(output, start, end, "end");
        content.focus();
        updatePreview();
    }

    function applyHashtag() {
        var tag = prompt("Hashtag (without #)", "news");
        if (!tag) {
            return;
        }
        var cleaned = tag.replace(/\s+/g, "");
        var start = content.selectionStart;
        content.setRangeText("#" + cleaned + " ", start, start, "end");
        content.focus();
        updatePreview();
    }

    function applyMention() {
        var username = prompt("Username (without @)", "username");
        if (!username) {
            return;
        }
        var cleaned = username.replace(/\s+/g, "");
        var start = content.selectionStart;
        content.setRangeText("@" + cleaned + " ", start, start, "end");
        content.focus();
        updatePreview();
    }

    function applyFeature(feature) {
        switch (feature) {
            case "bold":
                return applyWrap("**", "**", "bold text");
            case "italic":
                return applyWrap("*", "*", "italic text");
            case "underline":
                return applyWrap("__", "__", "underlined text");
            case "strikethrough":
                return applyWrap("~~", "~~", "strikethrough text");
            case "spoiler":
                return applyWrap("||", "||", "hidden text");
            case "code":
                return applyWrap("`", "`", "code");
            case "quote":
                return applyLinePrefix("> ");
            case "link":
                return applyLink();
            case "hashtag":
                return applyHashtag();
            case "mention":
                return applyMention();
            default:
                return;
        }
    }

    function updateToolbarAvailability() {
        var selected = getSelectedPlatforms();
        var hasTelegram = selected.some(v => v.startsWith("TELEGRAM"));
        var hasDiscord = selected.some(v => v.startsWith("DISCORD"));
        var hasWhatsapp = selected.some(v => v.startsWith("WHATSAPP"));

        var count = (hasTelegram ? 1 : 0) + (hasDiscord ? 1 : 0) + (hasWhatsapp ? 1 : 0);

        var modeText = messages.noPlatform;
        if (count > 1) {
            modeText = messages.mixedMode;
        } else if (hasTelegram) {
            modeText = messages.telegramMode;
        } else if (hasDiscord) {
            modeText = messages.discordMode;
        } else if (hasWhatsapp) {
            modeText = messages.whatsappMode;
        }

        Array.prototype.forEach.call(toolbar.querySelectorAll(".format-btn"), function (button) {
            var feature = button.dataset.feature;
            var support = featureSupport[feature];
            var enabled = true;

            if (count > 1) {
                enabled = (!hasTelegram || support.telegram) && (!hasDiscord || support.discord) && (!hasWhatsapp || support.whatsapp);
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

        if (featureHint) {
            if (count === 1) {
                featureHint.textContent = modeText + messages.formattingSuffix;
            } else if (count > 1) {
                featureHint.textContent = modeText + messages.sharedSuffix;
            } else {
                featureHint.textContent = modeText + messages.defaultSuffix;
            }
        }
    }

    function updateCharacterLimit() {
        var selected = getSelectedPlatforms();
        var minLimit = platformLimits.DEFAULT;

        if (selected.length > 0) {
            selected.forEach(function (val) {
                var platform = val.split(":")[0];
                var limit = platformLimits[platform] || platformLimits.DEFAULT;
                if (limit < minLimit) {
                    minLimit = limit;
                }
            });
        }

        if (maxCharCount) {
            maxCharCount.textContent = minLimit;
        }
        if (content) {
            content.maxLength = minLimit;
        }
        
        updateCharCountColor();
    }

    function updateCharCountColor() {
        if (!charCount || !content || !maxCharCount) return;
        var current = content.value.length;
        var max = parseInt(maxCharCount.textContent, 10);
        
        if (current > max) {
            charCount.classList.add("text-danger");
        } else if (current > max * 0.9) {
            charCount.classList.add("text-warning");
            charCount.classList.remove("text-danger");
        } else {
            charCount.classList.remove("text-warning", "text-danger");
        }
    }

    function escapeHtml(value) {
        return value
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function renderMarkdownForPreview(rawText) {
        if (!rawText || !rawText.trim()) {
            return messages.previewPlaceholder;
        }

        var safe = escapeHtml(rawText);
        safe = safe.replace(/\r\n/g, "\n");

        safe = safe.replace(/^&gt; (.+)$/gm, "<blockquote>$1</blockquote>");
        safe = safe.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
        safe = safe.replace(/__(.+?)__/g, "<u>$1</u>");
        safe = safe.replace(/\*(.+?)\*/g, "<em>$1</em>");
        safe = safe.replace(/~~(.+?)~~/g, "<del>$1</del>");
        safe = safe.replace(/\|\|(.+?)\|\|/g, "<span class=\"preview-spoiler\">$1</span>");
        safe = safe.replace(/`([^`]+?)`/g, "<code>$1</code>");
        safe = safe.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, "<a href=\"$2\" target=\"_blank\" rel=\"noopener noreferrer\">$1</a>");
        safe = safe.replace(/(^|[\s])#([A-Za-z0-9_]+)/g, "$1<span class=\"preview-tag\">#$2</span>");
        safe = safe.replace(/(^|[\s])@([A-Za-z0-9_]+)/g, "$1<span class=\"preview-mention\">@$2</span>");
        safe = safe.replace(/\n/g, "<br>");

        return safe;
    }

    function buildMediaCard(src, index) {
        return "<figure class=\"preview-media-card\"><img src=\"" + src + "\" alt=\"Selected media " + (index + 1) + "\"></figure>";
    }

    function renderPreviewMedia() {
        if (!previewTelegramMedia || !previewDiscordMedia) {
            return;
        }

        var combinedHtml = "";
        collectedFiles.forEach(function (file, index) {
            combinedHtml += buildMediaCard(URL.createObjectURL(file), index);
        });

        if (existingMediaGrid) {
            Array.prototype.forEach.call(existingMediaGrid.querySelectorAll(".existing-media-item"), function (item, index) {
                var img = item.querySelector("img");
                var src = img ? img.src : "";
                combinedHtml += buildMediaCard(src, index + collectedFiles.length);
            });
        }

        previewTelegramMedia.innerHTML = combinedHtml;
        previewDiscordMedia.innerHTML = combinedHtml;
        if (previewSlackMedia) previewSlackMedia.innerHTML = combinedHtml;
        if (previewLinkedinMedia) previewLinkedinMedia.innerHTML = combinedHtml;
        if (previewNotionMedia) previewNotionMedia.innerHTML = combinedHtml;
    }

    function updatePreview() {
        var html = renderMarkdownForPreview(content.value);
        if (previewTelegramContent) previewTelegramContent.innerHTML = html;
        if (previewDiscordContent) previewDiscordContent.innerHTML = html;
        if (previewSlackContent) previewSlackContent.innerHTML = html;
        if (previewLinkedinContent) previewLinkedinContent.innerHTML = html;
        if (previewNotionContent) previewNotionContent.innerHTML = html;

        renderPreviewMedia();
        if (charCount) charCount.textContent = content.value.length;
        updateCharCountColor();
    }

    function resolvePreviewMode() {
        var selected = getSelectedPlatforms();
        if (selected.some(v => v.startsWith("DISCORD"))) return "discord";
        if (selected.some(v => v.startsWith("SLACK"))) return "slack";
        if (selected.some(v => v.startsWith("LINKEDIN"))) return "linkedin";
        if (selected.some(v => v.startsWith("NOTION"))) return "notion";
        return "telegram";
    }

    function setPreviewMode(mode) {
        if (previewTelegramCard) previewTelegramCard.classList.toggle("is-hidden", mode !== "telegram");
        if (previewDiscordCard) previewDiscordCard.classList.toggle("is-hidden", mode !== "discord");
        if (previewSlackCard) previewSlackCard.classList.toggle("is-hidden", mode !== "slack");
        if (previewLinkedinCard) previewLinkedinCard.classList.toggle("is-hidden", mode !== "linkedin");
        if (previewNotionCard) previewNotionCard.classList.toggle("is-hidden", mode !== "notion");

        document.querySelectorAll(".preview-platform-tab").forEach(function (tab) {
            tab.classList.toggle("is-active", tab.dataset.previewPlatform === mode);
        });
    }

    function syncFilesToInput() {
        if (!mediaInput) return;
        var dt = new DataTransfer();
        collectedFiles.forEach(function (file) {
            dt.items.add(file);
        });
        mediaInput.files = dt.files;
    }

    function addFiles(fileList) {
        var maxFiles = 10;
        var existingCount = 0;
        if (existingMediaGrid) {
            existingCount = existingMediaGrid.querySelectorAll(".existing-media-item").length;
        }

        Array.from(fileList).forEach(function (file) {
            if (collectedFiles.length + existingCount >= maxFiles) return;
            if (!file.type.startsWith("image/")) return;
            if (file.size > 10 * 1024 * 1024) return;
            collectedFiles.push(file);
        });
        syncFilesToInput();
        renderSelectedMedia();
        updatePreview();
        saveDraftDebounced();
    }

    function renderSelectedMedia() {
        if (!mediaPreview) return;

        mediaPreview.innerHTML = "";
        collectedFiles.forEach(function (file, index) {
            var item = document.createElement("article");
            item.className = "media-thumb-card";
            item.draggable = true;
            item.dataset.sortIndex = String(index);

            var mediaEl = document.createElement("img");
            mediaEl.src = URL.createObjectURL(file);
            mediaEl.alt = "Selected image";

            var badge = document.createElement("span");
            badge.className = "media-order-badge";
            badge.textContent = String(index + 1);

            var removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "media-remove-btn selected-media-remove";
            removeButton.dataset.index = String(index);
            removeButton.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';

            var handle = document.createElement("div");
            handle.className = "media-drag-handle";
            handle.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><circle cx="9" cy="5" r="1.5"/><circle cx="15" cy="5" r="1.5"/><circle cx="9" cy="12" r="1.5"/><circle cx="15" cy="12" r="1.5"/><circle cx="9" cy="19" r="1.5"/><circle cx="15" cy="19" r="1.5"/></svg>';

            item.appendChild(mediaEl);
            item.appendChild(badge);
            item.appendChild(removeButton);
            item.appendChild(handle);
            mediaPreview.appendChild(item);

            item.addEventListener("dragstart", function (e) {
                dragSrcIndex = index;
                item.classList.add("is-dragging");
                e.dataTransfer.effectAllowed = "move";
                e.dataTransfer.setData("text/plain", String(index));
            });

            item.addEventListener("dragend", function () {
                item.classList.remove("is-dragging");
                clearDragOverStates(mediaPreview);
            });

            item.addEventListener("dragover", function (e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                item.classList.add("drag-over");
            });

            item.addEventListener("dragleave", function () {
                item.classList.remove("drag-over");
            });

            item.addEventListener("drop", function (e) {
                e.preventDefault();
                item.classList.remove("drag-over");
                var fromIndex = dragSrcIndex;
                var toIndex = index;
                if (fromIndex === null || fromIndex === toIndex) return;
                var moved = collectedFiles.splice(fromIndex, 1)[0];
                collectedFiles.splice(toIndex, 0, moved);
                syncFilesToInput();
                renderSelectedMedia();
                updatePreview();
            });
        });
    }

    function clearDragOverStates(container) {
        container.querySelectorAll(".drag-over").forEach(function (el) {
            el.classList.remove("drag-over");
        });
    }

    function removeSelectedFile(indexToRemove) {
        collectedFiles.splice(indexToRemove, 1);
        syncFilesToInput();
        renderSelectedMedia();
        updatePreview();
        saveDraftDebounced();
    }

    function markExistingMediaRemoved(publicId, targetCard) {
        if (!removedMediaContainer || !publicId) {
            return;
        }
        var hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = "removeMediaPublicIds";
        hidden.value = publicId;
        removedMediaContainer.appendChild(hidden);

        if (targetCard) {
            targetCard.remove();
        }
        updateExistingMediaBadges();
        updatePreview();
    }

    function updateExistingMediaBadges() {
        if (!existingMediaGrid) return;
        existingMediaGrid.querySelectorAll(".media-thumb-card").forEach(function (card, idx) {
            var badge = card.querySelector(".media-order-badge");
            if (badge) badge.textContent = String(idx + 1);
        });
    }

    function initExistingMediaSorting() {
        if (!existingMediaGrid) return;
        var cards = existingMediaGrid.querySelectorAll(".media-thumb-card");
        cards.forEach(function (card, index) {
            var badge = card.querySelector(".media-order-badge");
            if (badge) badge.textContent = String(index + 1);

            card.addEventListener("dragstart", function (e) {
                dragSrcIndex = index;
                card.classList.add("is-dragging");
                e.dataTransfer.effectAllowed = "move";
                e.dataTransfer.setData("text/plain", String(index));
            });

            card.addEventListener("dragend", function () {
                card.classList.remove("is-dragging");
                clearDragOverStates(existingMediaGrid);
            });

            card.addEventListener("dragover", function (e) {
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                card.classList.add("drag-over");
            });

            card.addEventListener("dragleave", function () {
                card.classList.remove("drag-over");
            });

            card.addEventListener("drop", function (e) {
                e.preventDefault();
                card.classList.remove("drag-over");
                var fromIndex = dragSrcIndex;
                var allCards = Array.from(existingMediaGrid.querySelectorAll(".media-thumb-card"));
                var toIndex = allCards.indexOf(card);
                if (fromIndex === null || fromIndex === toIndex) return;

                var movedCard = allCards[fromIndex];
                var refCard = allCards[toIndex];
                if (fromIndex < toIndex) {
                    existingMediaGrid.insertBefore(movedCard, refCard.nextSibling);
                } else {
                    existingMediaGrid.insertBefore(movedCard, refCard);
                }
                updateExistingMediaBadges();
                updatePreview();
            });
        });
    }

    toolbar.addEventListener("click", function (event) {
        var button = event.target.closest(".format-btn");
        if (!button || button.disabled) {
            return;
        }
        applyFeature(button.dataset.feature);
    });

    if (mediaDropzone) {
        var dropzoneEnterCount = 0;

        mediaDropzone.addEventListener("dragenter", function (e) {
            e.preventDefault();
            dropzoneEnterCount++;
            mediaDropzone.classList.add("is-dragover");
        });

        mediaDropzone.addEventListener("dragleave", function (e) {
            e.preventDefault();
            dropzoneEnterCount--;
            if (dropzoneEnterCount <= 0) {
                dropzoneEnterCount = 0;
                mediaDropzone.classList.remove("is-dragover");
            }
        });

        mediaDropzone.addEventListener("dragover", function (e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = "copy";
        });

        mediaDropzone.addEventListener("drop", function (e) {
            e.preventDefault();
            dropzoneEnterCount = 0;
            mediaDropzone.classList.remove("is-dragover");
            if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                addFiles(e.dataTransfer.files);
            }
        });
    }

    if (mediaInput) {
        mediaInput.addEventListener("change", function () {
            addFiles(mediaInput.files);
        });
    }

    if (mediaPreview) {
        mediaPreview.addEventListener("click", function (event) {
            var removeBtn = event.target.closest(".selected-media-remove");
            if (!removeBtn) return;
            var index = Number.parseInt(removeBtn.dataset.index, 10);
            if (!Number.isNaN(index)) {
                removeSelectedFile(index);
            }
        });
    }

    if (existingMediaGrid) {
        existingMediaGrid.addEventListener("click", function (event) {
            var removeBtn = event.target.closest(".existing-media-remove");
            if (!removeBtn) return;
            var publicId = removeBtn.dataset.publicId;
            markExistingMediaRemoved(publicId, removeBtn.closest(".existing-media-item"));
        });
        initExistingMediaSorting();
    }

    document.querySelectorAll('input[name="platforms"]').forEach(function (input) {
        input.addEventListener("change", function () {
            updateToolbarAvailability();
            updateCharacterLimit();
            setPreviewMode(resolvePreviewMode());
            saveDraftDebounced();
        });
    });

    document.querySelectorAll(".preview-platform-tab").forEach(function (tab) {
        tab.addEventListener("click", function () {
            setPreviewMode(tab.dataset.previewPlatform || "telegram");
        });
    });

    function updateTelegramTime() {
        var timeEl = document.querySelector(".tg-bubble-time");
        if (timeEl) {
            var now = new Date();
            var h = String(now.getHours()).padStart(2, "0");
            var m = String(now.getMinutes()).padStart(2, "0");
            timeEl.textContent = h + ":" + m;
        }
    }

    content.addEventListener("input", function() {
        updatePreview();
        saveDraftDebounced();
    });

    updateToolbarAvailability();
    updateCharacterLimit();
    setPreviewMode(resolvePreviewMode());
    updateTelegramTime();
    updatePreview();

    const btnSaveTemplate = document.getElementById("btnSaveTemplate");
    const saveTemplateModal = document.getElementById("saveTemplateModal");
    const btnCancelSaveTemplate = document.getElementById("btnCancelSaveTemplate");
    const templateNameInput = document.getElementById("templateNameInput");

    function openSaveModal() {
        if (!saveTemplateModal) return;
        if (!content.value.trim()) {
            return;
        }
        templateNameInput.value = "";
        saveTemplateModal.classList.add("active");
        templateNameInput.focus();
    }

    function closeSaveModal() {
        if (!saveTemplateModal) return;
        saveTemplateModal.classList.remove("active");
    }

    function applyTemplate(contentVal, platformsStr) {
        content.value = contentVal || "";

        document.querySelectorAll('input[name="platforms"]').forEach(input => {
            input.checked = false;
        });
        
        if (platformsStr) {
            const platforms = platformsStr.split(',');
            platforms.forEach(p => {
                const input = document.querySelector(`input[name="platforms"][value="${p}"]`);
                if (input && !input.disabled) {
                    input.checked = true;
                }
            });
        }
        
        updateToolbarAvailability();
        updateCharacterLimit();
        updatePreview();
        saveDraftDebounced();
    }

    document.addEventListener('click', (e) => {
        const applyBtn = e.target.closest('.btn-apply-template');
        if (applyBtn) {
            const card = applyBtn.closest('.template-inline-card');
            if (card) applyTemplate(card.dataset.content, card.dataset.platforms);
        }
    });

    if (btnSaveTemplate) btnSaveTemplate.addEventListener("click", openSaveModal);
    if (btnCancelSaveTemplate) btnCancelSaveTemplate.addEventListener("click", closeSaveModal);
    if (saveTemplateModal) {
        saveTemplateModal.addEventListener("click", (e) => {
            if (e.target === saveTemplateModal) closeSaveModal();
        });
        const closeModalBtn = saveTemplateModal.querySelector('.close-modal-btn');
        if (closeModalBtn) closeModalBtn.addEventListener('click', closeSaveModal);
    }
    document.querySelectorAll('input[name="platforms"]').forEach(function (input) {
        input.addEventListener("change", saveDraftDebounced);
    });

    document.addEventListener("htmx:beforeRequest", function (evt) {
        if (evt.detail.elt && (evt.detail.elt.closest("form.creator-layout") || evt.detail.elt.id === "btnConfirmSaveTemplate")) {
            if (saveDraftDebounced && saveDraftDebounced.cancel) {
                saveDraftDebounced.cancel();
            }
            DraftStore.remove("currentDraft");
        }
    });

    document.addEventListener("htmx:afterRequest", function (evt) {
        if (evt.detail.successful) {
            if (saveDraftDebounced && saveDraftDebounced.cancel) {
                saveDraftDebounced.cancel();
            }
            DraftStore.remove("currentDraft");
        }
    });

    if (!window.location.pathname.includes("/edit")) {
        var interval = parseInt(localStorage.getItem("autosaveInterval") || "5000");
        if (interval !== 0) {
            DraftStore.init().then(function () {
                DraftStore.load("currentDraft").then(function (data) {
                    if (data) {
                        if (data.content) {
                            content.value = data.content;
                        }
                        if (data.platforms && data.platforms.length > 0) {
                            document.querySelectorAll('input[name="platforms"]').forEach(function (input) {
                                input.checked = data.platforms.indexOf(input.value) !== -1;
                            });
                        }
                        if (data.files && data.files.length > 0) {
                            collectedFiles = data.files;
                            syncFilesToInput();
                            renderSelectedMedia();
                        }
                        updateCharacterLimit();
                        updatePreview();
                        showNotice(messages.draftRestored);
                    }
                });
            });
        }
    }
})();
