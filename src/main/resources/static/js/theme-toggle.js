(function () {
    document.addEventListener("DOMContentLoaded", function () {
        var btn = document.getElementById("theme-toggle");
        if (!btn) return;

        var sun = btn.querySelector(".sun-icon");
        var moon = btn.querySelector(".moon-icon");

        var isDark = document.documentElement.classList.contains("dark-mode");
        if (isDark) {
            sun.style.display = "block";
            moon.style.display = "none";
        }

        btn.addEventListener("click", function () {
            document.documentElement.classList.toggle("dark-mode");
            var dark = document.documentElement.classList.contains("dark-mode");
            localStorage.setItem("theme", dark ? "dark" : "light");
            sun.style.display = dark ? "block" : "none";
            moon.style.display = dark ? "none" : "block";
        });
    });
})();
