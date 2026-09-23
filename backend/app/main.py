"""GeoAlarm backend (FastAPI).

The Android app never needs this service for alarm monitoring. It exists to
publish release metadata and public configuration for the website, and to be the
home of future opt-in features (accounts, sync). It never receives location data.
"""
from __future__ import annotations

import logging
import time

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from .config import Settings, get_settings
from .release import build_version_payload, read_version

log = logging.getLogger("geoalarm.api")

APP_NAME = "GeoAlarm"
TAGLINE = "Never Miss Your Destination."


def _error(status: int, code: str, message: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={"error": {"code": code, "message": message}})


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    logging.basicConfig(
        level=getattr(logging, settings.log_level, logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )

    app = FastAPI(
        title="GeoAlarm API",
        version=read_version(),
        docs_url=None if settings.is_production else "/api/docs",
        redoc_url=None,
        openapi_url=None if settings.is_production else "/api/openapi.json",
    )

    if settings.cors_origins:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=list(settings.cors_origins),
            allow_methods=["GET", "OPTIONS"],
            allow_headers=["Content-Type"],
            allow_credentials=False,
            max_age=600,
        )

    @app.middleware("http")
    async def request_log_and_security_headers(request: Request, call_next):
        started = time.perf_counter()
        response = await call_next(request)
        elapsed_ms = (time.perf_counter() - started) * 1000
        # Deliberately no query string, headers, client IP or body in logs.
        log.info("%s %s -> %s (%.1f ms)", request.method, request.url.path, response.status_code, elapsed_ms)
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("Referrer-Policy", "no-referrer")
        response.headers.setdefault("Cache-Control", "no-store")
        return response

    @app.exception_handler(StarletteHTTPException)
    async def http_error(_: Request, exc: StarletteHTTPException):
        code = "not_found" if exc.status_code == 404 else "http_error"
        return _error(exc.status_code, code, str(exc.detail))

    @app.exception_handler(RequestValidationError)
    async def validation_error(_: Request, __: RequestValidationError):
        return _error(422, "invalid_request", "The request was not valid.")

    @app.exception_handler(Exception)
    async def unhandled_error(_: Request, exc: Exception):
        log.error("Unhandled error: %s", type(exc).__name__)  # type only: no user data in logs
        return _error(500, "internal_error", "Something went wrong on our side.")

    @app.get("/")
    def root():
        return {"name": APP_NAME, "tagline": TAGLINE, "health": "/api/health"}

    @app.get("/api/health")
    def health():
        return {"status": "ok", "service": "geoalarm-api", "version": read_version(), "environment": settings.env}

    @app.get("/api/version")
    def version():
        return JSONResponse(
            content=build_version_payload(settings),
            headers={"Cache-Control": "public, max-age=300"},
        )

    @app.get("/api/config")
    def config():
        payload = build_version_payload(settings)
        return JSONResponse(
            content={
                "appName": APP_NAME,
                "tagline": TAGLINE,
                "latestVersion": payload["latestVersion"],
                "minimumAndroidVersion": payload["minimumAndroidVersion"],
                "downloadAvailable": bool(payload["downloadUrl"]),
                "features": {"accounts": False, "cloudSync": False},
                "privacy": {"locationReceivedByServer": False},
            },
            headers={"Cache-Control": "public, max-age=300"},
        )

    return app


app = create_app()
