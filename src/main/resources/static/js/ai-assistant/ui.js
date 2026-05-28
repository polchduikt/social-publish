import { normalizeText } from './parser.js';

export function shouldSuppressBoilerplateReply(reply, generatedText) {
    if (!generatedText) return false;
    const text = normalizeText(reply);
    if (!text) return false;
    return /^ось текст( поста)?/.test(text)
        || /^ось готовий текст/.test(text)
        || /^ось варіант/.test(text)
        || /^here('?s| is) (the )?text/.test(text)
        || /^here('?s| is) (a )?post/.test(text)
        || /^here('?s| is) your/.test(text);
}

export function looksLikeMessagePlacementChoice(value) {
    const text = normalizeText(value);
    return [
        "message", "в message", "у message", "поле message", "в поле message",
        "insert into message", "paste in message", "в повідомлення", "у повідомлення"
    ].some(token => text.indexOf(token) !== -1);
}

export function looksLikeChatPlacementChoice(value) {
    const text = normalizeText(value);
    return ["chat", "в чат", "у чат", "тут", "here"].some(token => text.indexOf(token) !== -1);
}

export function looksLikeAffirmative(value) {
    const text = normalizeText(value);
    return ["так", "yes", "ok", "okay", "ок", "добре", "звісно"].some(token => text === token || text.indexOf(token + " ") === 0);
}

export function applyToPostCreatorMessage(text) {
    const contentField = document.getElementById("content");
    if (!contentField) return false;
    contentField.value = text;
    contentField.dispatchEvent(new Event("input", { bubbles: true }));
    contentField.focus();
    return true;
}
