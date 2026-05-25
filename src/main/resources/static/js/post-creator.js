import { getMessages } from './post-creator/config.js';
import { applyFeature, updateToolbarAvailability } from './post-creator/toolbar.js';
import { 
    updateCharacterLimit, 
    renderMarkdownForPreview, 
    renderPreviewMedia, 
    resolvePreviewMode, 
    setPreviewMode 
} from './post-creator/preview.js';
import { 
    syncFilesToInput, 
    clearDragOverStates, 
    updateExistingMediaBadges, 
    buildThumbCard 
} from './post-creator/media.js';
import { 
    toggleGroup, 
    updateAdvancedSettingsVisibility, 
    syncQuiz 
} from './post-creator/poll.js';
import { initScheduler } from './post-creator/scheduler.js';
import { initAutosave } from './post-creator/drafts.js';

(function () {
    const content = document.getElementById("content");
    const toolbar = document.getElementById("formatToolbar");
    const creatorShell = document.querySelector(".creator-shell");
    
    if (!content || !toolbar || !creatorShell) return;

    const charCount = document.getElementById("charCount");
    const maxCharCount = document.getElementById("maxCharCount");
    const featureHint = document.getElementById("featureHint");
    const mediaInput = document.getElementById("mediaFiles");
    const mediaPreview = document.getElementById("mediaPreview");
    const mediaDropzone = document.getElementById("mediaDropzone");
    const existingMediaGrid = document.getElementById("existingMediaGrid");
    const removedMediaContainer = document.getElementById("removedMediaContainer");

    const previewTargets = {
        telegram: document.getElementById("previewTelegramContent"),
        discord: document.getElementById("previewDiscordContent"),
        slack: document.getElementById("previewSlackContent"),
        linkedin: document.getElementById("previewLinkedinContent"),
        notion: document.getElementById("previewNotionContent")
    };

    const mediaTargets = [
        document.getElementById("previewTelegramMedia"),
        document.getElementById("previewDiscordMedia"),
        document.getElementById("previewSlackMedia"),
        document.getElementById("previewLinkedinMedia"),
        document.getElementById("previewNotionMedia")
    ];

    const cards = {
        telegram: document.getElementById("previewTelegram"),
        discord: document.getElementById("previewDiscord"),
        slack: document.getElementById("previewSlack"),
        linkedin: document.getElementById("previewLinkedin"),
        notion: document.getElementById("previewNotion")
    };

    const tabs = document.querySelectorAll(".preview-platform-tab");

    let collectedFiles = [];
    let dragSrcIndex = null;

    function getSelectedPlatforms() {
        return Array.from(document.querySelectorAll('input[name="platforms"]:checked')).map(i => i.value);
    }

    function triggerPreviewUpdate() {
        const text = renderMarkdownForPreview(content.value);
        Object.values(previewTargets).forEach(target => {
            if (target) target.innerHTML = text;
        });

        renderPreviewMedia(collectedFiles, "#existingMediaGrid", mediaTargets);
        if (charCount) charCount.textContent = content.value.length;
        updateCharacterLimit(getSelectedPlatforms(), content, maxCharCount, charCount);
    }

    const autosave = initAutosave(
        content,
        getSelectedPlatforms,
        () => collectedFiles,
        (files) => { collectedFiles = files; },
        refreshLayout,
        renderSelectedMedia
    );

    function refreshLayout() {
        const selected = getSelectedPlatforms();
        updateToolbarAvailability(selected, toolbar, featureHint);
        updateCharacterLimit(selected, content, maxCharCount, charCount);
        setPreviewMode(resolvePreviewMode(selected), cards, tabs);
        updateAdvancedSettingsVisibility(selected);
        triggerPreviewUpdate();
    }

    initScheduler(() => autosave.saveDraftDebounced());

    function addFiles(fileList) {
        const maxFiles = 10;
        const existingCount = existingMediaGrid ? existingMediaGrid.querySelectorAll(".existing-media-item").length : 0;

        Array.from(fileList).forEach(file => {
            if (collectedFiles.length + existingCount >= maxFiles) return;
            if (!file.type.startsWith("image/")) return;
            if (file.size > 10 * 1024 * 1024) return;
            collectedFiles.push(file);
        });

        syncFilesToInput(collectedFiles, mediaInput);
        renderSelectedMedia();
        triggerPreviewUpdate();
        autosave.saveDraftDebounced();
    }

    function renderSelectedMedia() {
        if (!mediaPreview) return;
        mediaPreview.innerHTML = "";

        collectedFiles.forEach((file, index) => {
            const card = buildThumbCard(file, index);
            mediaPreview.appendChild(card);

            card.addEventListener("dragstart", e => {
                dragSrcIndex = index;
                card.classList.add("is-dragging");
                e.dataTransfer.effectAllowed = "move";
                e.dataTransfer.setData("text/plain", String(index));
            });

            card.addEventListener("dragend", () => {
                card.classList.remove("is-dragging");
                clearDragOverStates(mediaPreview);
            });

            card.addEventListener("dragover", e => {
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                card.classList.add("drag-over");
            });

            card.addEventListener("dragleave", () => card.classList.remove("drag-over"));

            card.addEventListener("drop", e => {
                e.preventDefault();
                card.classList.remove("drag-over");
                const fromIndex = dragSrcIndex;
                if (fromIndex === null || fromIndex === index) return;

                const moved = collectedFiles.splice(fromIndex, 1)[0];
                collectedFiles.splice(index, 0, moved);

                syncFilesToInput(collectedFiles, mediaInput);
                renderSelectedMedia();
                triggerPreviewUpdate();
            });
        });
    }

    function removeSelectedFile(index) {
        collectedFiles.splice(index, 1);
        syncFilesToInput(collectedFiles, mediaInput);
        renderSelectedMedia();
        triggerPreviewUpdate();
        autosave.saveDraftDebounced();
    }

    function markExistingMediaRemoved(publicId, targetCard) {
        if (!removedMediaContainer || !publicId) return;

        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = "removeMediaPublicIds";
        hidden.value = publicId;
        removedMediaContainer.appendChild(hidden);

        if (targetCard) targetCard.remove();

        updateExistingMediaBadges(existingMediaGrid);
        triggerPreviewUpdate();
    }

    function initExistingMediaSorting() {
        if (!existingMediaGrid) return;
        existingMediaGrid.querySelectorAll(".media-thumb-card").forEach((card, index) => {
            card.addEventListener("dragstart", e => {
                dragSrcIndex = index;
                card.classList.add("is-dragging");
                e.dataTransfer.effectAllowed = "move";
                e.dataTransfer.setData("text/plain", String(index));
            });

            card.addEventListener("dragend", () => {
                card.classList.remove("is-dragging");
                clearDragOverStates(existingMediaGrid);
            });

            card.addEventListener("dragover", e => {
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                card.classList.add("drag-over");
            });

            card.addEventListener("dragleave", () => card.classList.remove("drag-over"));

            card.addEventListener("drop", e => {
                e.preventDefault();
                card.classList.remove("drag-over");
                const fromIndex = dragSrcIndex;
                const allCards = Array.from(existingMediaGrid.querySelectorAll(".media-thumb-card"));
                const toIndex = allCards.indexOf(card);
                if (fromIndex === null || fromIndex === toIndex) return;

                const movedCard = allCards[fromIndex];
                if (fromIndex < toIndex) {
                    existingMediaGrid.insertBefore(movedCard, card.nextSibling);
                } else {
                    existingMediaGrid.insertBefore(movedCard, card);
                }

                updateExistingMediaBadges(existingMediaGrid);
                triggerPreviewUpdate();
            });
        });
    }

    toolbar.addEventListener("click", event => {
        const button = event.target.closest(".format-btn");
        if (!button || button.disabled) return;
        applyFeature(content, button.dataset.feature, triggerPreviewUpdate);
    });

    content.addEventListener("input", () => {
        triggerPreviewUpdate();
        autosave.saveDraftDebounced();
    });

    if (mediaDropzone) {
        let enterCount = 0;
        mediaDropzone.addEventListener("dragenter", e => {
            e.preventDefault();
            enterCount++;
            mediaDropzone.classList.add("is-dragover");
        });
        mediaDropzone.addEventListener("dragleave", e => {
            e.preventDefault();
            enterCount--;
            if (enterCount <= 0) {
                enterCount = 0;
                mediaDropzone.classList.remove("is-dragover");
            }
        });
        mediaDropzone.addEventListener("dragover", e => {
            e.preventDefault();
            e.dataTransfer.dropEffect = "copy";
        });
        mediaDropzone.addEventListener("drop", e => {
            e.preventDefault();
            enterCount = 0;
            mediaDropzone.classList.remove("is-dragover");
            if (e.dataTransfer.files?.length > 0) addFiles(e.dataTransfer.files);
        });
    }

    if (mediaInput) mediaInput.addEventListener("change", () => addFiles(mediaInput.files));

    if (mediaPreview) {
        mediaPreview.addEventListener("click", event => {
            const removeBtn = event.target.closest(".selected-media-remove");
            if (!removeBtn) return;
            const index = parseInt(removeBtn.dataset.index, 10);
            if (!isNaN(index)) removeSelectedFile(index);
        });
    }

    if (existingMediaGrid) {
        existingMediaGrid.addEventListener("click", event => {
            const removeBtn = event.target.closest(".existing-media-remove");
            if (!removeBtn) return;
            markExistingMediaRemoved(removeBtn.dataset.publicId, removeBtn.closest(".existing-media-item"));
        });
        updateExistingMediaBadges(existingMediaGrid);
        initExistingMediaSorting();
    }

    function setupAdvancedSettings() {
        const btnSilent = document.getElementById("btnToggleSilent");
        const btnButtons = document.getElementById("btnToggleButtons");
        const btnPoll = document.getElementById("btnTogglePoll");
        const groupSilent = document.getElementById("groupSilentMode");
        const groupButtons = document.getElementById("groupInlineButtons");
        const groupPoll = document.getElementById("groupPoll");

        if (btnSilent) btnSilent.addEventListener("click", function () { toggleGroup(this, groupSilent); });
        if (btnButtons) btnButtons.addEventListener("click", function () { toggleGroup(this, groupButtons); });
        if (btnPoll) btnPoll.addEventListener("click", function () { toggleGroup(this, groupPoll); });

        if (document.getElementById("silentMode")?.checked) {
            if (groupSilent) groupSilent.style.display = 'block';
            btnSilent?.classList.add("is-active");
        }
        if (document.getElementById("inlineButtons")?.value.trim().length > 0) {
            if (groupButtons) groupButtons.style.display = 'block';
            btnButtons?.classList.add("is-active");
        }

        const pollQuestionInput = document.getElementById("pollQuestion");
        if (pollQuestionInput) {
            if (pollQuestionInput.value.trim().length > 0) {
                if (groupPoll) groupPoll.style.display = 'block';
                btnPoll?.classList.add("is-active");
                const container = document.getElementById("pollOptionsContainer");
                if (container) container.style.display = 'block';
            }

            pollQuestionInput.addEventListener("input", function () {
                const container = document.getElementById("pollOptionsContainer");
                if (container) container.style.display = this.value.trim().length > 0 ? 'block' : 'none';
            });
        }
    }

    setupAdvancedSettings();

    document.querySelectorAll('input[name="platforms"]').forEach(input => {
        input.addEventListener("change", () => {
            refreshLayout();
            autosave.saveDraftDebounced();
        });
    });

    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            setPreviewMode(tab.dataset.previewPlatform || "telegram", cards, tabs);
        });
    });

    function updateTelegramTime() {
        const timeEl = document.querySelector(".tg-bubble-time");
        if (timeEl) {
            const now = new Date();
            const h = String(now.getHours()).padStart(2, "0");
            const m = String(now.getMinutes()).padStart(2, "0");
            timeEl.textContent = `${h}:${m}`;
        }
    }

    const btnSaveTemplate = document.getElementById("btnSaveTemplate");
    const saveTemplateModal = document.getElementById("saveTemplateModal");
    const btnCancelSaveTemplate = document.getElementById("btnCancelSaveTemplate");
    const templateNameInput = document.getElementById("templateNameInput");

    function openSaveModal() {
        if (!saveTemplateModal || !content.value.trim()) return;
        templateNameInput.value = "";
        saveTemplateModal.classList.add("active");
        templateNameInput.focus();
    }

    function closeSaveModal() {
        saveTemplateModal?.classList.remove("active");
    }

    function applyTemplate(contentVal, platformsStr) {
        content.value = contentVal || "";
        document.querySelectorAll('input[name="platforms"]').forEach(input => { input.checked = false; });

        if (platformsStr) {
            platformsStr.split(',').forEach(p => {
                const input = document.querySelector(`input[name="platforms"][value="${p}"]`);
                if (input && !input.disabled) input.checked = true;
            });
        }

        refreshLayout();
        autosave.saveDraftDebounced();
    }

    document.addEventListener('click', e => {
        const applyBtn = e.target.closest('.btn-apply-template');
        if (applyBtn) {
            const card = applyBtn.closest('.template-inline-card');
            if (card) applyTemplate(card.dataset.content, card.dataset.platforms);
        }
    });

    if (btnSaveTemplate) btnSaveTemplate.addEventListener("click", openSaveModal);
    if (btnCancelSaveTemplate) btnCancelSaveTemplate.addEventListener("click", closeSaveModal);
    if (saveTemplateModal) {
        saveTemplateModal.addEventListener("click", e => {
            if (e.target === saveTemplateModal) closeSaveModal();
        });
        saveTemplateModal.querySelector('.close-modal-btn')?.addEventListener('click', closeSaveModal);
    }

    document.addEventListener("htmx:beforeRequest", evt => {
        if (evt.detail.elt && (evt.detail.elt.closest("form.creator-layout") || evt.detail.elt.id === "btnConfirmSaveTemplate")) {
            autosave.saveDraftDebounced?.cancel?.();
            window.DraftStore?.remove("currentDraft");
        }
    });

    document.addEventListener("htmx:afterRequest", evt => {
        if (evt.detail.successful) {
            autosave.saveDraftDebounced?.cancel?.();
            window.DraftStore?.remove("currentDraft");
        }
    });

    syncQuiz(() => autosave.saveDraftDebounced());
    setInterval(() => syncQuiz(() => autosave.saveDraftDebounced()), 1000);

    autosave.loadDraft?.();
    refreshLayout();
    updateTelegramTime();
})();
