/*
 * GeoAlarm website configuration.
 * Nothing here is secret: this file is public. Never put API keys or tokens in it.
 * The GitHub Pages workflow (.github/workflows/website-deploy.yml) regenerates this
 * file from repository *variables*, so you normally do not edit it by hand.
 */
window.GEOALARM_CONFIG = {
  // Base URL of the optional backend, e.g. "https://your-service.onrender.com" (no trailing slash).
  apiBaseUrl: "",
  // HTTPS URL of the release APK (GitHub Releases asset). Empty = no download published yet.
  apkUrl: "",
  // Repository URL, e.g. "https://github.com/USER/GeoAlarm".
  repoUrl: "",
  // Optional public contact address. Empty = the contact page only shows the issue tracker.
  contactEmail: ""
};
