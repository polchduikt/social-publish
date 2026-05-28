(function () {
  var toggle = document.querySelector('.hamburger-toggle');
  var backdrop = document.querySelector('.sidebar-backdrop');
  if (!toggle) return;

  function openSidebar() {
    document.body.classList.add('sidebar-open');
  }

  function closeSidebar() {
    document.body.classList.remove('sidebar-open');
  }

  toggle.addEventListener('click', function () {
    if (document.body.classList.contains('sidebar-open')) {
      closeSidebar();
    } else {
      openSidebar();
    }
  });

  if (backdrop) {
    backdrop.addEventListener('click', closeSidebar);
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeSidebar();
  });

  var sidebarLinks = document.querySelectorAll('.sidebar-nav a, .sidebar-bottom a');
  sidebarLinks.forEach(function (link) {
    link.addEventListener('click', function () {
      if (window.innerWidth <= 768) closeSidebar();
    });
  });
})();
