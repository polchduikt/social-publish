import { getMessages } from './config.js';
import { syncFilesToInput } from './media.js';

export function debounce(func, wait) {
    let timeout;
    const debounced = function (...args) {
        const context = this;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
    debounced.cancel = function () {
        clearTimeout(timeout);
    };
    return debounced;
}

export function initAutosave(content, getSelectedPlatformsCallback, getCollectedFilesCallback, setCollectedFilesCallback, refreshLayoutCallback, renderSelectedMediaCallback) {
    const interval = parseInt(localStorage.getItem("autosaveInterval") || "5000");
    if (interval === 0 || window.location.pathname.includes("/edit")) {
        return {
            saveDraftDebounced: () => {},
            loadDraft: () => Promise.resolve()
        };
    }

    const autosaveNotice = document.getElementById("autosave-notice");

    function showNotice(text) {
        if (!autosaveNotice) return;
        autosaveNotice.textContent = text;
        autosaveNotice.classList.add("show");
        setTimeout(() => {
            autosaveNotice.classList.remove("show");
        }, 3000);
    }

    const saveDraftDebounced = debounce(() => {
        if (!window.DraftStore) return;

        const currentData = {
            content: content.value,
            platforms: getSelectedPlatformsCallback(),
            files: getCollectedFilesCallback()
        };

        DraftStore.save("currentDraft", currentData).then(() => {
            showNotice(getMessages().autosaved);
        });
    }, interval);

    function loadDraft() {
        return DraftStore.init().then(() => {
            return DraftStore.load("currentDraft").then(data => {
                if (data) {
                    if (data.content) {
                        content.value = data.content;
                    }
                    if (data.platforms && data.platforms.length > 0) {
                        document.querySelectorAll('input[name="platforms"]').forEach(input => {
                            input.checked = data.platforms.indexOf(input.value) !== -1;
                        });
                    }
                    if (data.files && data.files.length > 0) {
                        setCollectedFilesCallback(data.files);
                        syncFilesToInput(data.files, document.getElementById("mediaFiles"));
                        renderSelectedMediaCallback();
                    }
                    refreshLayoutCallback();
                    showNotice(getMessages().draftRestored);
                }
            });
        });
    }

    return {
        saveDraftDebounced,
        loadDraft
    };
}
