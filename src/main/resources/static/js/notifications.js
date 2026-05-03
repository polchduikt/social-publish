(function () {
    'use strict';

    var userId = document.body.getAttribute('data-user-id');
    if (!userId) return;

    var container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    var socket = new SockJS('/ws');
    var client = Stomp.over(socket);
    client.debug = null;

    client.connect({}, function () {
        client.subscribe('/topic/user.' + userId, function (message) {
            var notification = JSON.parse(message.body);
            showToast(notification);
            updateStatusBadge(notification);
        });
    }, function () {
        setTimeout(function () { reconnect(); }, 5000);
    });

    function reconnect() {
        socket = new SockJS('/ws');
        client = Stomp.over(socket);
        client.debug = null;
        client.connect({}, function () {
            client.subscribe('/topic/user.' + userId, function (message) {
                var notification = JSON.parse(message.body);
                showToast(notification);
                updateStatusBadge(notification);
            });
        }, function () {
            setTimeout(function () { reconnect(); }, 5000);
        });
    }

    function showToast(notification) {
        var toast = document.createElement('div');
        toast.className = 'toast toast-' + notification.type;

        toast.innerHTML =
            '<div class="toast-body">' +
            '<strong class="toast-title">' + escapeHtml(notification.title) + '</strong>' +
            '<span class="toast-msg">' + escapeHtml(notification.message) + '</span>' +
            '</div>' +
            '<button class="toast-close" onclick="this.parentElement.remove()">×</button>';

        container.appendChild(toast);

        requestAnimationFrame(function () {
            toast.classList.add('toast-visible');
        });

        setTimeout(function () {
            toast.classList.add('toast-hiding');
            setTimeout(function () { toast.remove(); }, 400);
        }, notification.type === 'error' ? 8000 : 5000);
    }

    function updateStatusBadge(notification) {
        var badges = document.querySelectorAll('[data-post-id="' + notification.postId + '"] .status-badge');
        badges.forEach(function (badge) {
            badge.className = 'status-badge ' + notification.status.toLowerCase();
            badge.textContent = notification.status;
        });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text || '';
        return div.innerHTML;
    }
})();
