/* Runs in <head> before first paint to avoid a light/dark flash. */
(function () {
  try {
    var saved = localStorage.getItem("geoalarm-theme");
    if (saved === "light" || saved === "dark") document.documentElement.setAttribute("data-theme", saved);
  } catch (e) { /* storage unavailable: fall back to the system theme */ }
})();
