import { removeAccountRow, togglePasswordVisibility } from './accounts/actions.js';
import { 
    getTelegramTemplate, 
    getDiscordTemplate, 
    getSlackTemplate, 
    getNotionTemplate 
} from './accounts/templates.js';

function addNewAccountRow(platform) {
    const container = document.getElementById('account-rows-container');
    if (!container) return;

    const rows = container.getElementsByClassName('account-row-box');
    const nextIndex = rows.length;
    let template = '';

    if (platform === 'telegram') {
        template = getTelegramTemplate(nextIndex);
    } else if (platform === 'discord') {
        template = getDiscordTemplate(nextIndex);
    } else if (platform === 'slack') {
        template = getSlackTemplate(nextIndex);
    } else if (platform === 'notion') {
        template = getNotionTemplate(nextIndex);
    }

    if (template) {
        container.insertAdjacentHTML('beforeend', template);
    }
}

window.removeAccountRow = removeAccountRow;
window.togglePasswordVisibility = togglePasswordVisibility;
window.addNewAccountRow = addNewAccountRow;
