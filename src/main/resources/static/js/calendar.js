document.addEventListener('DOMContentLoaded', function () {
    var root = document.getElementById('calendarRoot');
    var titleEl = document.getElementById('calTitle');
    var popover = document.getElementById('eventPopover');
    var dateInput = document.getElementById('popoverDateInput');
    var csrfToken = document.querySelector('meta[name="_csrf"]').content;
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    var lang = document.documentElement.lang || 'en';
    var activeEventId = null;

    var monthNames = {
        en: ['January','February','March','April','May','June','July','August','September','October','November','December'],
        uk: ['Січень','Лютий','Березень','Квітень','Травень','Червень','Липень','Серпень','Вересень','Жовтень','Листопад','Грудень']
    };

    var dayNamesShort = {
        en: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'],
        uk: ['Пн','Вт','Ср','Чт','Пт','Сб','Нд']
    };

    var locale = monthNames[lang] ? lang : 'en';

    var calendar = new FullCalendar.Calendar(root, {
        initialView: 'dayGridMonth',
        headerToolbar: false,
        height: 'auto',
        firstDay: 1,
        editable: true,
        fixedMirrorParent: document.body,
        dragScroll: false,
        dragRevertDuration: 0,
        dayMaxEvents: 3,
        eventDisplay: 'block',
        forceEventDuration: true,
        defaultTimedEventDuration: '00:30:00',
        nextDayThreshold: '00:00:00',
        slotEventOverlap: false,
        allDaySlot: true,
        eventStartEditable: function (event) {
            var status = event.extendedProps.status;
            return status !== 'PUBLISHED' && status !== 'PUBLISHING';
        },
        events: '/api/calendar/events',
        eventTimeFormat: {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        },
        slotLabelFormat: {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        },
        dayHeaderContent: function (arg) {
            var dayIndex = (arg.dow + 6) % 7;
            return dayNamesShort[locale][dayIndex];
        },

        eventClick: function (info) {
            info.jsEvent.preventDefault();
            showPopover(info.event, info.el);
        },

        eventDrop: function (info) {
            var canDrag = info.event.extendedProps.status !== 'PUBLISHED' && info.event.extendedProps.status !== 'PUBLISHING';
            if (!canDrag) {
                info.revert();
                return;
            }
            rescheduleEvent(info.event.id, info.event.start, info.revert, {
                preserveScrollY: getScrollY(),
                refetchEvents: false
            });
        },

        datesSet: function () {
            updateTitle();
        }
    });

    calendar.render();
    updateTitle();

    window.addEventListener('resize', function () {
        calendar.updateSize();
    });

    document.getElementById('calPrev').addEventListener('click', function () {
        calendar.prev();
    });

    document.getElementById('calNext').addEventListener('click', function () {
        calendar.next();
    });

    document.getElementById('calToday').addEventListener('click', function () {
        calendar.today();
    });

    document.querySelectorAll('.view-btn[data-view]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.view-btn[data-view]').forEach(function (b) {
                b.classList.remove('active');
            });
            btn.classList.add('active');
            calendar.changeView(btn.dataset.view);
        });
    });

    document.getElementById('popoverClose').addEventListener('click', function () {
        hidePopover();
    });

    document.getElementById('popoverReschedule').addEventListener('click', function () {
        if (!activeEventId || !dateInput.value) return;
        var newDate = new Date(dateInput.value);
        rescheduleEvent(activeEventId, newDate, null, {
            preserveScrollY: getScrollY(),
            refetchEvents: true
        });
        hidePopover();
    });

    document.getElementById('popoverDelete').addEventListener('click', function () {
        if (!activeEventId) return;
        deleteEvent(activeEventId);
        hidePopover();
    });

    document.addEventListener('click', function (e) {
        if (!popover.contains(e.target) && !e.target.closest('.fc-event')) {
            hidePopover();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            hidePopover();
        }
    });

    function updateTitle() {
        var date = calendar.getDate();
        titleEl.textContent = monthNames[locale][date.getMonth()] + ' ' + date.getFullYear();
    }

    function toIsoLocal(d) {
        return d.getFullYear() + '-' +
            String(d.getMonth() + 1).padStart(2, '0') + '-' +
            String(d.getDate()).padStart(2, '0') + 'T' +
            String(d.getHours()).padStart(2, '0') + ':' +
            String(d.getMinutes()).padStart(2, '0');
    }

    function showPopover(event, targetEl) {
        var props = event.extendedProps;
        activeEventId = event.id;

        document.getElementById('popoverTitle').textContent = event.title;
        document.getElementById('popoverExcerpt').textContent = props.excerpt || '';

        var statusBadge = document.getElementById('popoverStatus');
        statusBadge.textContent = props.status;
        statusBadge.className = 'status-badge ' + props.status.toLowerCase();

        document.getElementById('popoverPlatforms').textContent = props.platforms || 'N/A';

        var start = event.start;
        if (start) {
            var h = String(start.getHours()).padStart(2, '0');
            var m = String(start.getMinutes()).padStart(2, '0');
            var day = String(start.getDate()).padStart(2, '0');
            var month = String(start.getMonth() + 1).padStart(2, '0');
            document.getElementById('popoverTime').textContent = day + '.' + month + '.' + start.getFullYear() + ' ' + h + ':' + m;
            dateInput.value = toIsoLocal(start);
        }

        document.getElementById('popoverEdit').href = '/posts/' + props.postId + '/edit';

        var canReschedule = props.status !== 'PUBLISHED' && props.status !== 'PUBLISHING';
        document.getElementById('popoverReschedule').style.display = canReschedule ? '' : 'none';
        document.querySelector('.popover-reschedule').style.display = canReschedule ? '' : 'none';

        var canDelete = props.status !== 'PUBLISHED' && props.status !== 'PUBLISHING';
        document.getElementById('popoverDelete').style.display = canDelete ? '' : 'none';

        var rect = targetEl.getBoundingClientRect();
        var popW = 300;
        var popH = 340;
        var left = rect.left + rect.width / 2 - popW / 2;
        var top = rect.bottom + 8;

        if (left < 8) left = 8;
        if (left + popW > window.innerWidth - 8) left = window.innerWidth - popW - 8;
        if (top + popH > window.innerHeight - 8) top = rect.top - popH - 8;

        popover.style.left = left + 'px';
        popover.style.top = top + 'px';
        popover.classList.remove('is-hidden');
    }

    function hidePopover() {
        popover.classList.add('is-hidden');
        activeEventId = null;
    }

    function getScrollY() {
        return window.scrollY || window.pageYOffset || document.documentElement.scrollTop || 0;
    }

    function restoreScrollY(targetY) {
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                window.scrollTo(window.scrollX || 0, targetY);
            });
        });
    }

    function rescheduleEvent(eventId, newDate, revert, options) {
        var opts = options || {};
        var iso = newDate.getFullYear() + '-' +
            String(newDate.getMonth() + 1).padStart(2, '0') + '-' +
            String(newDate.getDate()).padStart(2, '0') + 'T' +
            String(newDate.getHours()).padStart(2, '0') + ':' +
            String(newDate.getMinutes()).padStart(2, '0') + ':' +
            String(newDate.getSeconds()).padStart(2, '0');

        var headers = {
            'Content-Type': 'application/json'
        };
        headers[csrfHeader] = csrfToken;

        fetch('/api/calendar/events/' + eventId + '/reschedule', {
            method: 'PATCH',
            headers: headers,
            body: JSON.stringify({ scheduledAt: iso })
        })
        .then(function (res) {
            if (res.ok) {
                if (opts.refetchEvents) {
                    calendar.refetchEvents();
                }
                if (typeof opts.preserveScrollY === 'number') {
                    restoreScrollY(opts.preserveScrollY);
                }
            } else {
                if (revert) revert();
                if (typeof opts.preserveScrollY === 'number') {
                    restoreScrollY(opts.preserveScrollY);
                }
            }
        })
        .catch(function () {
            if (revert) revert();
            if (typeof opts.preserveScrollY === 'number') {
                restoreScrollY(opts.preserveScrollY);
            }
        });
    }

    function deleteEvent(eventId) {
        var headers = {};
        headers[csrfHeader] = csrfToken;

        fetch('/api/calendar/events/' + eventId, {
            method: 'DELETE',
            headers: headers
        })
        .then(function (res) {
            if (res.ok) {
                calendar.refetchEvents();
            }
        });
    }
});
