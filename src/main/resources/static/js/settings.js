(function () {
    var lightBtn = document.getElementById('themeLightBtn');
    var darkBtn = document.getElementById('themeDarkBtn');

    if (lightBtn && darkBtn) {
        function syncTheme() {
            var isDark = document.documentElement.classList.contains('dark-mode');
            lightBtn.classList.toggle('active', !isDark);
            darkBtn.classList.toggle('active', isDark);
        }
        syncTheme();

        lightBtn.addEventListener('click', function () {
            document.documentElement.classList.remove('dark-mode');
            localStorage.setItem('theme', 'light');
            syncTheme();
            var sun = document.querySelector('#theme-toggle .sun-icon');
            var moon = document.querySelector('#theme-toggle .moon-icon');
            if (sun) sun.style.display = 'none';
            if (moon) moon.style.display = 'block';
        });

        darkBtn.addEventListener('click', function () {
            document.documentElement.classList.add('dark-mode');
            localStorage.setItem('theme', 'dark');
            syncTheme();
            var sun = document.querySelector('#theme-toggle .sun-icon');
            var moon = document.querySelector('#theme-toggle .moon-icon');
            if (sun) sun.style.display = 'block';
            if (moon) moon.style.display = 'none';
        });
    }

    var autosaveBtns = document.querySelectorAll('#autosave-control .appearance-btn');
    if (autosaveBtns.length > 0) {
        function syncAutosave() {
            var activeInterval = localStorage.getItem('autosaveInterval') || '2000';
            autosaveBtns.forEach(function (btn) {
                btn.classList.toggle('active', btn.dataset.interval === activeInterval);
            });
        }
        syncAutosave();

        autosaveBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                localStorage.setItem('autosaveInterval', btn.dataset.interval);
                syncAutosave();
            });
        });
    }
})();
