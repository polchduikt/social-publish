export function removeAccountRow(button) {
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

export function togglePasswordVisibility(button) {
    const input = button.parentElement.querySelector('input');
    const icon = button.querySelector('svg');
    if (input.type === 'password') {
        input.type = 'text';
        icon.innerHTML = '<path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line>';
    } else {
        input.type = 'password';
        icon.innerHTML = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle>';
    }
}
