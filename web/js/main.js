(function () {
  "use strict";
  var cfg = window.GEOALARM_CONFIG || {};
  var doc = document;

  /* ---------- theme ---------- */
  var root = doc.documentElement;
  function effectiveTheme() {
    var t = root.getAttribute("data-theme");
    if (t) return t;
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  var toggle = doc.querySelector(".theme-toggle");
  if (toggle) {
    toggle.addEventListener("click", function () {
      var next = effectiveTheme() === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", next);
      try { localStorage.setItem("geoalarm-theme", next); } catch (e) { /* ignore */ }
    });
  }

  /* ---------- mobile navigation ---------- */
  var menuBtn = doc.querySelector(".menu-btn");
  var nav = doc.getElementById("site-nav");
  if (menuBtn && nav) {
    menuBtn.addEventListener("click", function () {
      var open = nav.classList.toggle("open");
      menuBtn.setAttribute("aria-expanded", String(open));
    });
    nav.addEventListener("click", function (e) {
      if (e.target.tagName === "A") { nav.classList.remove("open"); menuBtn.setAttribute("aria-expanded", "false"); }
    });
    doc.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && nav.classList.contains("open")) {
        nav.classList.remove("open"); menuBtn.setAttribute("aria-expanded", "false"); menuBtn.focus();
      }
    });
  }

  /* ---------- config-driven links ---------- */
  function isHttps(u) { return typeof u === "string" && /^https:\/\//i.test(u); }
  function releasesUrl() { return isHttps(cfg.repoUrl) ? cfg.repoUrl.replace(/\/$/, "") + "/releases" : ""; }
  var links = {
    repoUrl: isHttps(cfg.repoUrl) ? cfg.repoUrl : "",
    issuesUrl: isHttps(cfg.repoUrl) ? cfg.repoUrl.replace(/\/$/, "") + "/issues" : "",
    releasesUrl: releasesUrl(),
    contactEmail: cfg.contactEmail ? "mailto:" + cfg.contactEmail : ""
  };
  doc.querySelectorAll("[data-cfg-href]").forEach(function (el) {
    var v = links[el.getAttribute("data-cfg-href")];
    if (v) { el.setAttribute("href", v); el.hidden = false; } else { el.hidden = true; }
  });
  doc.querySelectorAll("[data-cfg-fallback]").forEach(function (el) {
    var v = links[el.getAttribute("data-cfg-fallback")];
    el.hidden = !!v;
  });
  doc.querySelectorAll("[data-cfg-text='contactEmail']").forEach(function (el) {
    if (cfg.contactEmail) { el.textContent = cfg.contactEmail; }
  });

  /* ---------- version + download ---------- */
  var info = {};
  function applyVersion() {
    var map = { version: info.version, releaseDate: info.releaseDate, minAndroid: info.minimumAndroidVersion, buildType: info.buildType };
    Object.keys(map).forEach(function (k) {
      if (map[k]) doc.querySelectorAll("[data-" + k.replace(/[A-Z]/g, function (c) { return "-" + c.toLowerCase(); }) + "]").forEach(function (el) { el.textContent = map[k]; });
    });
    var url = isHttps(cfg.apkUrl) ? cfg.apkUrl : (isHttps(info.downloadUrl) ? info.downloadUrl : "");
    doc.querySelectorAll("[data-apk-link]").forEach(function (a) {
      if (url) { a.setAttribute("href", url); a.removeAttribute("aria-disabled"); a.removeAttribute("tabindex"); }
      else { a.removeAttribute("href"); a.setAttribute("aria-disabled", "true"); a.setAttribute("tabindex", "-1"); }
    });
    doc.querySelectorAll("[data-apk-missing]").forEach(function (el) { el.hidden = !!url; });
    doc.querySelectorAll("[data-apk-sha]").forEach(function (el) {
      if (info.sha256) { el.textContent = info.sha256; el.closest("li").hidden = false; }
    });
    var notes = doc.getElementById("release-notes");
    if (notes && Array.isArray(info.releaseNotes) && info.releaseNotes.length) {
      notes.textContent = "";
      info.releaseNotes.forEach(function (n) { var li = doc.createElement("li"); li.textContent = n; notes.appendChild(li); });
    }
  }
  function getJson(url, ms) {
    var ctl = "AbortController" in window ? new AbortController() : null;
    var timer = ctl ? setTimeout(function () { ctl.abort(); }, ms) : null;
    return fetch(url, { headers: { Accept: "application/json" }, signal: ctl ? ctl.signal : undefined })
      .then(function (r) { if (!r.ok) throw new Error("HTTP " + r.status); return r.json(); })
      .finally(function () { if (timer) clearTimeout(timer); });
  }
  // 1) static version.json generated from the VERSION file at deploy time
  getJson("version.json", 5000).then(function (j) { info = { version: j.version, releaseDate: j.releaseDate, minimumAndroidVersion: j.minimumAndroidVersion, buildType: j.buildType, releaseNotes: j.releaseNotes }; })
    .catch(function () { /* keep the built-in fallback text */ })
    .then(applyVersion)
    // 2) optional live metadata from the backend, if configured
    .then(function () {
      if (!isHttps(cfg.apiBaseUrl)) return;
      return getJson(cfg.apiBaseUrl.replace(/\/$/, "") + "/api/version", 6000).then(function (j) {
        info = { version: j.latestVersion, releaseDate: j.releaseDate, minimumAndroidVersion: j.minimumAndroidVersion, buildType: j.buildType,
                 releaseNotes: j.releaseNotes, downloadUrl: j.downloadUrl, sha256: j.sha256 };
        applyVersion();
      }).catch(function () { /* backend asleep or unreachable: the static data above stays */ });
    });

  doc.querySelectorAll("[data-year]").forEach(function (el) { el.textContent = new Date().getFullYear(); });

  /* ---------- hero demo: one orchestrated moment ---------- */
  var route = doc.getElementById("demo-route");
  if (route) {
    var dot = doc.getElementById("demo-dot"), halo = doc.getElementById("demo-halo");
    var distEl = doc.getElementById("demo-dist"), stateEl = doc.getElementById("demo-state");
    var dest = { x: 390, y: 190 }, M_PER_PX = 500 / 150, R_WARN = 150, R_ARRIVE = 30;
    var len = route.getTotalLength();
    var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var fmt = function (m) { return m >= 1000 ? (m / 1000).toFixed(1) + " km" : (Math.max(0, Math.round(m / 10) * 10)) + " m"; };
    var draw = function (p) {
      var pt = route.getPointAtLength(p * len);
      dot.setAttribute("cx", pt.x); dot.setAttribute("cy", pt.y);
      halo.setAttribute("cx", pt.x); halo.setAttribute("cy", pt.y);
      var px = Math.hypot(pt.x - dest.x, pt.y - dest.y);
      distEl.textContent = fmt(px * M_PER_PX);
      var s = px <= R_ARRIVE ? "arrived" : px <= R_WARN ? "warn" : "mon";
      if (stateEl.getAttribute("data-s") !== s) {
        stateEl.setAttribute("data-s", s);
        stateEl.textContent = s === "arrived" ? "Alarm ringing" : s === "warn" ? "Warning sent" : "Monitoring";
      }
    };
    if (reduce) { draw(0.8); }
    else {
      var TRAVEL = 13000, HOLD = 3000, t0 = null;
      var frame = function (ts) {
        if (t0 === null) t0 = ts;
        var t = (ts - t0) % (TRAVEL + HOLD);
        draw(Math.min(1, t / TRAVEL));
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    }
  }
})();
