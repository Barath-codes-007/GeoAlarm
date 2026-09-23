# Render deployment

`render.yaml` at the repository root defines the backend as a Render Blueprint.

## First deploy

1. Push the repository to GitHub.
2. In Render: **New > Blueprint**, point it at the repository. Render reads
   `render.yaml` and proposes one web service, `geoalarm-api`.
3. Render will ask for the values marked `sync: false` in `render.yaml`:
   - `GEOALARM_CORS_ORIGINS` — your GitHub Pages origin, e.g.
     `https://YOUR-USER.github.io` (no path, no trailing slash). Leave blank until the
     site is live if you prefer.
   - `GEOALARM_APK_URL` — the HTTPS link to your published APK (optional; leave blank
     until you have a release).
   - `GEOALARM_APK_SHA256` — optional checksum.
4. Deploy. Render runs `pip install -r backend/requirements.txt` then
   `uvicorn app.main:app --app-dir backend --host 0.0.0.0 --port $PORT`, and polls
   `/api/health` to confirm the service is up.

## Updating configuration later

Render dashboard → the `geoalarm-api` service → **Environment** → edit the variable →
the service redeploys automatically. No code change or redeploy-from-git is needed for
config-only changes.

## Free-tier note

The `render.yaml` here uses `plan: free`. Render's free web services spin down after a
period of inactivity and take a few seconds to wake on the next request — acceptable for
a metadata endpoint the website polls occasionally, but mention it in your own release
notes if it matters to you. Upgrading `plan:` in `render.yaml` removes the spin-down.

## Custom domain / HTTPS

Render issues a free TLS certificate automatically for both the default
`onrender.com` subdomain and any custom domain you attach in the dashboard.
