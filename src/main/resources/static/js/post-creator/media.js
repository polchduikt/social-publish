export function syncFilesToInput(collectedFiles, mediaInput) {
    if (!mediaInput) return;
    const dt = new DataTransfer();
    collectedFiles.forEach(file => {
        dt.items.add(file);
    });
    mediaInput.files = dt.files;
}

export function clearDragOverStates(container) {
    if (!container) return;
    container.querySelectorAll(".drag-over").forEach(el => {
        el.classList.remove("drag-over");
    });
}

export function updateExistingMediaBadges(existingGrid) {
    if (!existingGrid) return;
    existingGrid.querySelectorAll(".media-thumb-card").forEach((card, idx) => {
        const badge = card.querySelector(".media-order-badge");
        if (badge) badge.textContent = String(idx + 1);
    });
}

export function buildThumbCard(file, index) {
    const item = document.createElement("article");
    item.className = "media-thumb-card";
    item.draggable = true;
    item.dataset.sortIndex = String(index);

    const mediaEl = document.createElement("img");
    mediaEl.src = URL.createObjectURL(file);
    mediaEl.alt = "Selected image";

    const badge = document.createElement("span");
    badge.className = "media-order-badge";
    badge.textContent = String(index + 1);

    const removeButton = document.createElement("button");
    removeButton.type = "button";
    removeButton.className = "media-remove-btn selected-media-remove";
    removeButton.dataset.index = String(index);
    removeButton.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';

    const handle = document.createElement("div");
    handle.className = "media-drag-handle";
    handle.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><circle cx="9" cy="5" r="1.5"/><circle cx="15" cy="5" r="1.5"/><circle cx="9" cy="12" r="1.5"/><circle cx="15" cy="12" r="1.5"/><circle cx="9" cy="19" r="1.5"/><circle cx="15" cy="19" r="1.5"/></svg>';

    item.appendChild(mediaEl);
    item.appendChild(badge);
    item.appendChild(removeButton);
    item.appendChild(handle);

    return item;
}
