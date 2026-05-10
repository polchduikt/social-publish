(function () {
    'use strict';

    var userId = document.body.getAttribute('data-user-id');
    if (!userId) return;

    var MAX_ITEMS = 50;

    var container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    var notifItems = [];

    // Fetch persisted notifications from backend on page load
    fetchNotifications();

    var socket = new SockJS('/ws');
    var client = Stomp.over(socket);
    client.debug = null;

    client.connect({}, function () {
        client.subscribe('/topic/user.' + userId, function (message) {
            var notification = JSON.parse(message.body);
            window.showToast(notification);
            addNotifItemLocal(notification);
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
                addNotifItemLocal(notification);
                updateStatusBadge(notification);
            });
        }, function () {
            setTimeout(function () { reconnect(); }, 5000);
        });
    }

    function fetchNotifications() {
        fetch('/api/notifications')
            .then(function (res) {
                if (!res.ok) return [];
                return res.json();
            })
            .then(function (data) {
                if (Array.isArray(data)) {
                    notifItems = data.slice(0, MAX_ITEMS);
                    renderNotifBell();
                }
            })
            .catch(function () {});
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

    function addNotifItemLocal(notification) {
        notifItems.unshift({
            postId: notification.postId || '',
            title: notification.title,
            status: notification.status,
            message: notification.message,
            type: notification.type,
            read: false,
            timestamp: notification.timestamp || new Date().toISOString()
        });
        if (notifItems.length > MAX_ITEMS) {
            notifItems = notifItems.slice(0, MAX_ITEMS);
        }
        renderNotifBell();
    }

    function renderNotifBell() {
        var countEl = document.getElementById('notifCount');
        var listEl = document.getElementById('notifList');
        if (!countEl || !listEl) return;

        var unreadCount = 0;
        notifItems.forEach(function (n) {
            if (!n.read) unreadCount++;
        });

        if (unreadCount > 0) {
            countEl.textContent = unreadCount > 99 ? '99+' : unreadCount;
            countEl.style.display = '';
        } else {
            countEl.style.display = 'none';
        }

        listEl.innerHTML = '';

        if (notifItems.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'notif-empty';
            empty.textContent = 'No new notifications';
            listEl.appendChild(empty);
            return;
        }

        notifItems.forEach(function (n) {
            var item = document.createElement('div');
            item.className = 'notif-item notif-' + n.type + (n.read ? ' notif-read' : '');
            var time = n.timestamp ? formatTimeAgo(new Date(n.timestamp)) : '';
            item.innerHTML =
                '<div class="notif-item-icon">' + getStatusIcon(n.type) + '</div>' +
                '<div class="notif-item-body">' +
                    '<strong>' + escapeHtml(n.title) + '</strong>' +
                    '<span>' + escapeHtml(n.message) + '</span>' +
                '</div>' +
                '<span class="notif-item-time">' + time + '</span>';
            listEl.appendChild(item);
        });
    }

    function getStatusIcon(type) {
        switch (type) {
            case 'success': return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
            case 'error': return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
            case 'warning': return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
            default: return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4f83ff" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
        }
    }

    function formatTimeAgo(date) {
        var diff = Math.floor((Date.now() - date.getTime()) / 1000);
        if (diff < 60) return 'now';
        if (diff < 3600) return Math.floor(diff / 60) + 'm';
        if (diff < 86400) return Math.floor(diff / 3600) + 'h';
        return Math.floor(diff / 86400) + 'd';
    }

    document.addEventListener('DOMContentLoaded', function() {
        var toggleBtn = document.getElementById('notifToggle');
        var dropdown = document.getElementById('notifDropdown');
        var clearBtn = document.getElementById('notifClear');

        if (toggleBtn && dropdown) {
            toggleBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                var wasHidden = dropdown.classList.contains('is-hidden');
                dropdown.classList.toggle('is-hidden');
                if (wasHidden) {
                    markAllAsRead();
                }
            });

            document.addEventListener('click', function(e) {
                if (!dropdown.contains(e.target) && !toggleBtn.contains(e.target)) {
                    dropdown.classList.add('is-hidden');
                }
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', function() {
                clearAllNotifications();
            });
        }

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

            var url = new URL(window.location);
            url.searchParams.delete('message');
            url.searchParams.delete('error');
            url.searchParams.delete('toast');
            window.history.replaceState({}, document.title, url);
        }
    });

    function markAllAsRead() {
        var headers = { 'Content-Type': 'application/json' };
        if (window.csrfHeader && window.csrfToken) {
            headers[window.csrfHeader] = window.csrfToken;
        }

        fetch('/api/notifications/read', {
            method: 'POST',
            headers: headers
        }).then(function (res) {
            if (!res.ok) throw new Error('Network response was not ok');
            notifItems.forEach(function (n) { n.read = true; });
            renderNotifBell();
        }).catch(function (error) {
            console.error('Failed to mark notifications as read', error);
        });
    }

    function clearAllNotifications() {
        var headers = {};
        if (window.csrfHeader && window.csrfToken) {
            headers[window.csrfHeader] = window.csrfToken;
        }

        fetch('/api/notifications', {
            method: 'DELETE',
            headers: headers
        }).then(function (res) {
            if (!res.ok) throw new Error('Network response was not ok');
            notifItems = [];
            renderNotifBell();
        }).catch(function (error) {
            console.error('Failed to clear notifications', error);
        });
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
