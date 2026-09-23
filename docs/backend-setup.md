# Backend setup

The backend is a small FastAPI service. **The Android app does not need it** for alarm
creation or location monitoring — it only serves release metadata for the website (and
is a natural home for future opt-in features like account sync).

## Run locally

From the repository root:

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r backend/requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --app-dir backend --reload --port 8000
```

- Health check: `http://127.0.0.1:8000/api/health`
- Interactive docs (development only): `http://127.0.0.1:8000/api/docs`

## Configuration

All configuration is environment variables — see `.env.example` at the repository root
for the full list and defaults. Nothing in `backend/app/` reads a secret from a file
committed to the repository.

| Variable | Purpose |
|---|---|
| `GEOALARM_ENV` | `development` or `production`. Production disables `/api/docs`. |
| `GEOALARM_CORS_ORIGINS` | Comma-separated list of exact origins allowed to call the API from a browser (e.g. your GitHub Pages URL). |
| `GEOALARM_APK_URL` | HTTPS link to the published APK. Non-HTTPS values are ignored. |
| `GEOALARM_APK_SHA256` | Optional checksum shown on the website's download page. |
| `GEOALARM_LOG_LEVEL` | Standard Python logging level. |

## Tests

```bash
cd backend
python -m pytest -v
```

## Adding an endpoint

Keep the "Android never depends on this" principle: any new endpoint should be
something the *website* or a future optional account system needs, not something the
alarm-monitoring path requires. Add the route in `app/main.py`, add a matching test in
`tests/test_api.py`, and update `docs/architecture.md` if it changes the shape of the
system.
