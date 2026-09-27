# GeoAlarm backend

Small FastAPI service. **The Android app does not need it** to monitor destinations.

| Endpoint | Purpose |
|---|---|
| `GET /api/health` | Liveness check (Render health check) |
| `GET /api/version` | `latestVersion`, `releaseDate`, `minimumAndroidVersion`, `downloadUrl`, `releaseNotes` |
| `GET /api/config` | Public, non-secret configuration for the website |

Run locally (from the repository root):

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r backend/requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --app-dir backend --reload --port 8000
# http://127.0.0.1:8000/api/health   (docs in development: /api/docs)
```

Tests: `cd backend && python -m pytest`

See `docs/backend-setup.md` and `docs/render-deployment.md`.
