import { monthMap, platformMatchers } from './config.js';

export function normalizeText(value) {
    return (value || "")
        .normalize("NFKC")
        .toLowerCase()
        .replace(/\s+/g, " ")
        .trim();
}

export function toIsoLocal(dateValue) {
    const y = dateValue.getFullYear();
    const m = String(dateValue.getMonth() + 1).padStart(2, "0");
    const d = String(dateValue.getDate()).padStart(2, "0");
    const h = String(dateValue.getHours()).padStart(2, "0");
    const i = String(dateValue.getMinutes()).padStart(2, "0");
    return `${y}-${m}-${d}T${h}:${i}`;
}

export function parseScheduleDate(text) {
    const normalized = normalizeText(text);
    const now = new Date();
    let target = null;

    let hour = 10;
    let minute = 0;
    const timeMatch = normalized.match(/(?:о|на|at)\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?/);
    if (timeMatch) {
        hour = parseInt(timeMatch[1], 10);
        minute = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
        if (timeMatch[3] === "pm" && hour < 12) hour += 12;
        if (timeMatch[3] === "am" && hour === 12) hour = 0;
    } else {
        const shortTimeMatch = normalized.match(/\b(\d{1,2}):(\d{2})\b/);
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

    const dayMonthMatch = normalized.match(/\b(\d{1,2})\s+([a-zа-яіїєґ]+)(?:\s+(\d{4}))?\b/);
    if (dayMonthMatch && monthMap[dayMonthMatch[2]]) {
        const day = parseInt(dayMonthMatch[1], 10);
        const month = monthMap[dayMonthMatch[2]];
        const year = dayMonthMatch[3] ? parseInt(dayMonthMatch[3], 10) : now.getFullYear();
        target = new Date(year, month - 1, day, hour, minute, 0, 0);
        if (!dayMonthMatch[3] && target.getTime() < now.getTime()) {
            target.setFullYear(target.getFullYear() + 1);
        }
    }

    const numericDateMatch = normalized.match(/\b(\d{1,2})[.\/-](\d{1,2})(?:[.\/-](\d{4}))?\b/);
    if (!target && numericDateMatch) {
        const dayNum = parseInt(numericDateMatch[1], 10);
        const monthNum = parseInt(numericDateMatch[2], 10);
        const yearNum = numericDateMatch[3] ? parseInt(numericDateMatch[3], 10) : now.getFullYear();
        target = new Date(yearNum, monthNum - 1, dayNum, hour, minute, 0, 0);
        if (!numericDateMatch[3] && target.getTime() < now.getTime()) {
            target.setFullYear(target.getFullYear() + 1);
        }
    }

    if (!target) return "";
    target.setHours(hour, minute, 0, 0);
    return toIsoLocal(target);
}

export function inferIntentFromText(userText) {
    const text = normalizeText(userText);
    const platformTypes = [];

    platformMatchers.forEach(item => {
        if (item.pattern.test(text)) platformTypes.push(item.type);
    });

    let accountHint = "";
    const accountMatch = text.match(/\b(?:для|for)\s+([a-zа-яіїєґ0-9 _-]{2,30})/);
    if (accountMatch) {
        accountHint = accountMatch[1].split(/(?:\bна\b|\bо\b|\bat\b|,|\.|!|\?)/)[0].trim();
    }

    return {
        platformTypes,
        accountHint,
        scheduleAt: parseScheduleDate(text)
    };
}
