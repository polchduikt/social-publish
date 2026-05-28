import { normalizeText } from './parser.js';

export function applyPlatformsFromIntent(intent) {
    const checkboxes = Array.from(document.querySelectorAll('input[name="platforms"]'));
    if (!checkboxes.length) return false;
    if ((!intent.platformTypes || !intent.platformTypes.length) && !intent.accountHint) return false;

    const hint = normalizeText(intent.accountHint || "");
    let selectedCount = 0;
    checkboxes.forEach(checkbox => { checkbox.checked = false; });

    checkboxes.forEach(checkbox => {
        if (checkbox.disabled) return;
        const valueType = (checkbox.value || "").split(":")[0].toUpperCase();
        const labelEl = checkbox.closest("label") ? checkbox.closest("label").querySelector(".platform-chip-inner span:last-child") : null;
        const label = normalizeText(labelEl ? labelEl.textContent : "");
        const matchesType = !intent.platformTypes.length || intent.platformTypes.indexOf(valueType) !== -1;
        const matchesHint = !hint || label.indexOf(hint) !== -1;
        if (matchesType && matchesHint) {
            checkbox.checked = true;
            selectedCount++;
        }
    });

    if (selectedCount > 0) {
        checkboxes.forEach(checkbox => {
            checkbox.dispatchEvent(new Event("change", { bubbles: true }));
        });
        return true;
    }
    return false;
}

export function applyScheduleFromIntent(scheduleAt) {
    if (!scheduleAt) return false;
    const scheduledInput = document.getElementById("scheduledAt");
    if (!scheduledInput) return false;

    const onceRadio = document.querySelector('input[name="recurring"][value="false"]');
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

export function applyIntent(intent) {
    if (!intent) return;
    applyPlatformsFromIntent(intent);
    applyScheduleFromIntent(intent.scheduleAt);
}
