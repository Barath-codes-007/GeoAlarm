# GitHub Pages deployment

The website is fully static (`web/`) and is deployed by
`.github/workflows/website-deploy.yml` using GitHub's official Pages Actions — no
`gh-pages` branch or third-party action required.

## One-time setup

1. Repository **Settings → Pages → Source**: choose **GitHub Actions** (not "Deploy from
   a branch").
2. (Optional) Repository **Settings → Secrets and variables → Actions → Variables**, add
   any of:
   - `GEOALARM_API_BASE_URL` — your Render backend URL, e.g.
     `https://geoalarm-api.onrender.com`. Leave unset to run the site without a backend;
     it will still show version info from the static `version.json` generated at deploy
     time from the repository's `VERSION` file.
   - `GEOALARM_APK_URL` — HTTPS link to your published APK.
   - `GEOALARM_REPO_URL` — defaults to `https://github.com/<owner>/<repo>` automatically.
   - `GEOALARM_CONTACT_EMAIL` — shown on the contact page if set.
3. Push to `main`. The workflow builds `web/version.json` from the root `VERSION` file,
   writes `web/js/config.js` from the variables above, and publishes `web/` as-is.

## Project site vs. user/org site

If this repository is `github.com/YOUR-USER/GeoAlarm` (not `YOUR-USER.github.io`), the
site is served at `https://YOUR-USER.github.io/GeoAlarm/`. Every link and asset path in
`web/` is **relative** (`css/style.css`, `assets/logo.svg`, not `/css/style.css`), so the
site works correctly under that subpath without any extra configuration. If you rename
the repository, no path changes are needed.

## Verifying a deploy

Actions tab → the latest "Website deploy" run → the `deploy` job's summary links
directly to the published URL.
