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
            window.showToast(notification);
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
                window.showToast(notification);
                updateStatusBadge(notification);
            });
        }, function () {
            setTimeout(function () { reconnect(); }, 5000);
        });
    }

    window.showToast = function(notification) {
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
    };

    document.addEventListener('DOMContentLoaded', function() {
        var pageMessages = document.getElementById('page-messages');
        if (pageMessages && window.showToast) {
            var rawMsg = pageMessages.getAttribute('data-raw-msg');
            var rawErr = pageMessages.getAttribute('data-raw-err');

            if (!rawMsg && !rawErr) {
                var searchParams = new URLSearchParams(window.location.search);
                rawMsg = searchParams.get('message') || searchParams.get('toast');
                rawErr = searchParams.get('error');
            }

            var msgMap = {
                'Post deleted': pageMessages.getAttribute('data-post-deleted'),
                'Post created': pageMessages.getAttribute('data-post-created'),
                'Post updated': pageMessages.getAttribute('data-post-updated'),
                'Post queued for publishing': pageMessages.getAttribute('data-post-queued'),
                'Post not found': pageMessages.getAttribute('data-post-notfound'),
                'deleted': pageMessages.getAttribute('data-tmpl-deleted')
            };

            if (rawMsg && rawMsg !== 'null') {
                window.showToast({title: 'Info', message: msgMap[rawMsg] || rawMsg, type: 'success'});
            }
            if (rawErr && rawErr !== 'null') {
                window.showToast({title: 'Error', message: msgMap[rawErr] || rawErr, type: 'error'});
            }

            const url = new URL(window.location);
            url.searchParams.delete('message');
            url.searchParams.delete('error');
            url.searchParams.delete('toast');
            window.history.replaceState({}, document.title, url);
        }
    });

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
