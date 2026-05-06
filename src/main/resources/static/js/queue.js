(function () {
    var selectAll = document.getElementById("selectAllPosts");
    var rowChecks = Array.from(document.querySelectorAll(".row-post-checkbox"));
    var bulkForm = document.getElementById("bulkActionForm");
    var actionSelect = document.getElementById("bulkAction");
    var scheduleInput = document.getElementById("bulkScheduleAt");

    if (!bulkForm || !actionSelect || !scheduleInput) {
        return;
    }

    function updateSelectAllState() {
        if (!selectAll) {
            return;
        }
        var checkedCount = rowChecks.filter(function (checkbox) {
            return checkbox.checked;
        }).length;
        selectAll.checked = checkedCount > 0 && checkedCount === rowChecks.length;
        selectAll.indeterminate = checkedCount > 0 && checkedCount < rowChecks.length;
    }

    function updateScheduleInputState() {
        var needsSchedule = actionSelect.value === "RESCHEDULE";
        scheduleInput.disabled = !needsSchedule;
        scheduleInput.required = needsSchedule;
        if (!needsSchedule) {
            scheduleInput.value = "";
        }
    }

    if (selectAll) {
        selectAll.addEventListener("change", function () {
            rowChecks.forEach(function (checkbox) {
                checkbox.checked = selectAll.checked;
            });
            updateSelectAllState();
        });
    }

    rowChecks.forEach(function (checkbox) {
        checkbox.addEventListener("change", updateSelectAllState);
    });

    actionSelect.addEventListener("change", updateScheduleInputState);

    bulkForm.addEventListener("submit", function (event) {
        var hasSelected = rowChecks.some(function (checkbox) {
            return checkbox.checked;
        });
        if (!hasSelected) {
            event.preventDefault();
            alert("Select at least one post");
            return;
        }
        if (actionSelect.value === "RESCHEDULE" && !scheduleInput.value) {
            event.preventDefault();
            alert("Select date and time for reschedule");
        }
    });

    updateSelectAllState();
    updateScheduleInputState();
})();
