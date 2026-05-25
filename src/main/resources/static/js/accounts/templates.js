export function getTelegramTemplate(nextIndex) {
    return `
        <div class="account-row-box">
            <div class="checkbox-wrapper">
                <label for="enabled_${nextIndex}">Enabled</label>
                <input type="checkbox" id="enabled_${nextIndex}" name="accounts[${nextIndex}].enabled" value="true" checked />
                <input type="hidden" name="_accounts[${nextIndex}].enabled" value="on" />
            </div>
            <div class="account-row-fields" style="grid-template-columns: 1fr 1fr 1fr;">
                <div class="field-with-label">
                    <label for="label_${nextIndex}">Account Label</label>
                    <input type="text" id="label_${nextIndex}" name="accounts[${nextIndex}].label" required placeholder="e.g. News Channel" value="New Account"/>
                </div>
                <div class="field-with-label">
                    <label for="botToken_${nextIndex}">Bot Token</label>
                    <div class="field-with-button">
                        <input type="password" id="botToken_${nextIndex}" name="accounts[${nextIndex}].botToken" required autocomplete="off" placeholder="123456:ABC-DEF..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
                <div class="field-with-label">
                    <label for="chatId_${nextIndex}">Chat ID</label>
                    <div class="field-with-button">
                        <input type="password" id="chatId_${nextIndex}" name="accounts[${nextIndex}].chatId" required placeholder="@channel or -100..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
            </div>
            <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
            </button>
        </div>
    `;
}

export function getDiscordTemplate(nextIndex) {
    return `
        <div class="account-row-box">
            <div class="checkbox-wrapper">
                <label for="enabled_${nextIndex}">Enabled</label>
                <input type="checkbox" id="enabled_${nextIndex}" name="accounts[${nextIndex}].enabled" value="true" checked />
                <input type="hidden" name="_accounts[${nextIndex}].enabled" value="on" />
            </div>
            <div class="account-row-fields" style="grid-template-columns: 1fr 2fr;">
                <div class="field-with-label">
                    <label for="label_${nextIndex}">Account Label</label>
                    <input type="text" id="label_${nextIndex}" name="accounts[${nextIndex}].label" required placeholder="e.g. My Server" value="New Account"/>
                </div>
                <div class="field-with-label">
                    <label for="webhookUrl_${nextIndex}">Webhook URL</label>
                    <div class="field-with-button">
                        <input type="password" id="webhookUrl_${nextIndex}" name="accounts[${nextIndex}].webhookUrl" required autocomplete="off" placeholder="https://discord.com/api/webhooks/..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
            </div>
            <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
            </button>
        </div>
    `;
}

export function getSlackTemplate(nextIndex) {
    return `
        <div class="account-row-box">
            <div class="checkbox-wrapper">
                <label for="enabled_${nextIndex}">Enabled</label>
                <input type="checkbox" id="enabled_${nextIndex}" name="accounts[${nextIndex}].enabled" value="true" checked />
                <input type="hidden" name="_accounts[${nextIndex}].enabled" value="on" />
            </div>
            <div class="account-row-fields" style="grid-template-columns: 1fr 2fr;">
                <div class="field-with-label">
                    <label for="label_${nextIndex}">Account Label</label>
                    <input type="text" id="label_${nextIndex}" name="accounts[${nextIndex}].label" required placeholder="e.g. Workspace" value="New Account"/>
                </div>
                <div class="field-with-label">
                    <label for="webhookUrl_${nextIndex}">Webhook URL</label>
                    <div class="field-with-button">
                        <input type="password" id="webhookUrl_${nextIndex}" name="accounts[${nextIndex}].webhookUrl" required autocomplete="off" placeholder="https://hooks.slack.com/..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
            </div>
            <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
            </button>
        </div>
    `;
}

export function getNotionTemplate(nextIndex) {
    return `
        <div class="account-row-box">
            <div class="checkbox-wrapper">
                <label for="enabled_${nextIndex}">Enabled</label>
                <input type="checkbox" id="enabled_${nextIndex}" name="accounts[${nextIndex}].enabled" value="true" checked />
                <input type="hidden" name="_accounts[${nextIndex}].enabled" value="on" />
            </div>
            <div class="account-row-fields" style="grid-template-columns: 1fr 1fr 1fr;">
                <div class="field-with-label">
                    <label for="label_${nextIndex}">Account Label</label>
                    <input type="text" id="label_${nextIndex}" name="accounts[${nextIndex}].label" required placeholder="e.g. Wiki" value="New Account"/>
                </div>
                <div class="field-with-label">
                    <label for="apiToken_${nextIndex}">API Token</label>
                    <div class="field-with-button">
                        <input type="password" id="apiToken_${nextIndex}" name="accounts[${nextIndex}].apiToken" required autocomplete="off" placeholder="secret_..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
                <div class="field-with-label">
                    <label for="databaseId_${nextIndex}">Database URL/ID</label>
                    <div class="field-with-button">
                        <input type="password" id="databaseId_${nextIndex}" name="accounts[${nextIndex}].databaseId" required placeholder="https://notion.so/..."/>
                        <button type="button" class="btn-toggle-visibility" onclick="togglePasswordVisibility(this)" tabindex="-1">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
            </div>
            <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
            </button>
        </div>
    `;
}
