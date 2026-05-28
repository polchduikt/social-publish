export const HISTORY_KEY = "sp.aiAssistant.history.v1";
export const STATE_KEY = "sp.aiAssistant.state.v1";
export const MAX_HISTORY = 120;

export const monthMap = {
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

export const platformMatchers = [
    { type: "TELEGRAM", pattern: /(telegram|телеграм)/ },
    { type: "DISCORD", pattern: /(discord|дискорд)/ },
    { type: "SLACK", pattern: /(slack|слак)/ },
    { type: "LINKEDIN", pattern: /(linkedin|лінкедін|линкедин)/ },
    { type: "NOTION", pattern: /(notion|ноушн|нотіон)/ }
];

export function getI18n() {
    const root = document.getElementById("ai-assistant-root");
    if (!root) return {};
    
    const data = name => root.dataset[name] || "";
    return {
        greeting: data("i18nGreeting"),
        typing: data("i18nTyping"),
        askPlacement: data("i18nAskPlacement"),
        inserted: data("i18nInserted"),
        noMessageField: data("i18nNoMessageField"),
        requestFailed: data("i18nRequestFailed"),
        networkError: data("i18nNetworkError"),
        clearDone: data("i18nClearDone")
    };
}
