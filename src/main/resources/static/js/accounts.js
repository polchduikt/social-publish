function removeAccountRow(button) {
    const row = button.closest('.account-row-box');
    const container = row.parentElement;
    if (container.getElementsByClassName('account-row-box').length > 1) {
        row.remove();
        Array.from(container.getElementsByClassName('account-row-box')).forEach((r, idx) => {
            r.querySelectorAll('input').forEach(input => {
                const name = input.getAttribute('name');
                if (name && (name.startsWith('accounts[') || name.startsWith('_accounts['))) {
                    input.setAttribute('name', name.replace(/_?accounts\[\d+\]/, (match) => {
                        return match.startsWith('_') ? `_accounts[${idx}]` : `accounts[${idx}]`;
                    }));
                }
                const id = input.getAttribute('id');
                if (id && id.match(/_\d+$/)) {
                    input.setAttribute('id', id.replace(/_\d+$/, `_${idx}`));
                }
            });
            r.querySelectorAll('label').forEach(label => {
                const htmlFor = label.getAttribute('for');
                if (htmlFor && htmlFor.match(/_\d+$/)) {
                    label.setAttribute('for', htmlFor.replace(/_\d+$/, `_${idx}`));
                }
            });
        });
    } else {
        alert('You must have at least one account row.');
    }
}

function addNewAccountRow(platform) {
    const container = document.getElementById('account-rows-container');
    const rows = container.getElementsByClassName('account-row-box');
    const nextIndex = rows.length;
    let template = '';

    if (platform === 'telegram') {
        template = `
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
                        <input type="password" id="botToken_${nextIndex}" name="accounts[${nextIndex}].botToken" required autocomplete="off" placeholder="123456:ABC-DEF..."/>
                    </div>
                    <div class="field-with-label">
                        <label for="chatId_${nextIndex}">Chat ID</label>
                        <input type="text" id="chatId_${nextIndex}" name="accounts[${nextIndex}].chatId" required placeholder="@channel or -100..."/>
                    </div>
                </div>
                <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
                </button>
            </div>
        `;
    } else if (platform === 'discord') {
        template = `
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
                        <input type="password" id="webhookUrl_${nextIndex}" name="accounts[${nextIndex}].webhookUrl" required autocomplete="off" placeholder="https://discord.com/api/webhooks/..."/>
                    </div>
                </div>
                <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
                </button>
            </div>
        `;
    } else if (platform === 'slack') {
        template = `
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
                        <input type="password" id="webhookUrl_${nextIndex}" name="accounts[${nextIndex}].webhookUrl" required autocomplete="off" placeholder="https://hooks.slack.com/..."/>
                    </div>
                </div>
                <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
                </button>
            </div>
        `;
    } else if (platform === 'notion') {
        template = `
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
                        <input type="password" id="apiToken_${nextIndex}" name="accounts[${nextIndex}].apiToken" required autocomplete="off" placeholder="secret_..."/>
                    </div>
                    <div class="field-with-label">
                        <label for="databaseId_${nextIndex}">Database URL/ID</label>
                        <input type="text" id="databaseId_${nextIndex}" name="accounts[${nextIndex}].databaseId" required placeholder="https://notion.so/..."/>
                    </div>
                </div>
                <button type="button" class="btn-remove-account" onclick="removeAccountRow(this)">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke-linecap="round" stroke-linejoin="round"></path></svg>
                </button>
            </div>
        `;
    }

    if (template) {
        container.insertAdjacentHTML('beforeend', template);
    }
}
