export const featureSupport = {
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

export const platformLimits = {
    TELEGRAM: 4096,
    DISCORD: 2000,
    SLACK: 4000,
    LINKEDIN: 3000,
    NOTION: 2000,
    DEFAULT: 5000
};

export function getMessages() {
    const creatorShell = document.querySelector(".creator-shell");
    return {
        noPlatform: creatorShell?.dataset.msgNoPlatform || "Select platform",
        mixedMode: creatorShell?.dataset.msgMixedMode || "Mixed Mode",
        telegramMode: creatorShell?.dataset.msgTelegramMode || "Telegram Mode",
        discordMode: creatorShell?.dataset.msgDiscordMode || "Discord Mode",
        whatsappMode: creatorShell?.dataset.msgWhatsappMode || "WhatsApp Mode",
        formattingSuffix: creatorShell?.dataset.msgFormattingSuffix || " formatting",
        sharedSuffix: creatorShell?.dataset.msgSharedSuffix || " shared",
        defaultSuffix: creatorShell?.dataset.msgDefaultSuffix || " default",
        previewPlaceholder: creatorShell?.dataset.msgPreviewPlaceholder || "Preview",
        autosaved: creatorShell?.dataset.msgAutosaved || "Autosaved",
        draftRestored: creatorShell?.dataset.msgDraftRestored || "Draft restored"
    };
}
