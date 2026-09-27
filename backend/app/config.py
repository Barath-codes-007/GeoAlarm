"""Runtime configuration, read exclusively from environment variables.

Nothing secret has a default value. See ../.env.example for the full list.
"""
from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

log = logging.getLogger("geoalarm.config")

# Load a local .env in development only. On Render, real environment variables are used.
_REPO_ROOT = Path(__file__).resolve().parents[2]
load_dotenv(_REPO_ROOT / ".env", override=False)


def _split_csv(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


@dataclass(frozen=True)
class Settings:
    env: str
    cors_origins: tuple[str, ...]
    apk_url: str
    apk_sha256: str
    release_date_override: str
    min_android_override: str
    log_level: str

    @property
    def is_production(self) -> bool:
        return self.env == "production"


def _clean_apk_url(raw: str) -> str:
    """Only HTTPS download links are ever advertised."""
    if not raw:
        return ""
    if not raw.lower().startswith("https://"):
        log.warning("GEOALARM_APK_URL ignored: it must start with https://")
        return ""
    return raw


def load_settings() -> Settings:
    env = os.environ.get("GEOALARM_ENV", "development").strip().lower()
    if env not in {"development", "production", "test"}:
        env = "development"
    origins = _split_csv(os.environ.get("GEOALARM_CORS_ORIGINS", ""))
    if "*" in origins and env == "production":
        log.warning("Wildcard CORS origin ignored in production")
        origins = [o for o in origins if o != "*"]
    return Settings(
        env=env,
        cors_origins=tuple(origins),
        apk_url=_clean_apk_url(os.environ.get("GEOALARM_APK_URL", "").strip()),
        apk_sha256=os.environ.get("GEOALARM_APK_SHA256", "").strip().lower(),
        release_date_override=os.environ.get("GEOALARM_RELEASE_DATE", "").strip(),
        min_android_override=os.environ.get("GEOALARM_MIN_ANDROID", "").strip(),
        log_level=os.environ.get("GEOALARM_LOG_LEVEL", "INFO").strip().upper(),
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return load_settings()
