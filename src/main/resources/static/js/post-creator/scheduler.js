export function initScheduler(saveDraftCallback) {
    try {
        flatpickr("#scheduledAt", {
            enableTime: true,
            dateFormat: "Y-m-d\\TH:i",
            altInput: true,
            altFormat: "Y-m-d H:i",
            time_24hr: true,
            minDate: "today",
            minuteIncrement: 1,
            defaultHour: new Date().getHours(),
            defaultMinute: new Date().getMinutes()
        });

        flatpickr("#recurringEndDate", {
            dateFormat: "Y-m-d\\TH:i",
            altInput: true,
            altFormat: "Y-m-d",
            minDate: "today"
        });
    } catch (e) {
        console.warn("Flatpickr init failed:", e);
    }

    const recurringOptions = document.getElementById("recurringOptions");
    const singleScheduleField = document.getElementById("singleScheduleField");

    window.updateScheduleMode = function (isRecurring) {
        if (isRecurring) {
            recurringOptions?.classList.remove("is-hidden");
            singleScheduleField?.classList.add("is-hidden");
        } else {
            recurringOptions?.classList.add("is-hidden");
            singleScheduleField?.classList.remove("is-hidden");
        }
        saveDraftCallback();
    };
}
