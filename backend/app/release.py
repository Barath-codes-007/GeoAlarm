"""Release metadata. The version comes from the repository-level VERSION file."""
from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any

from .config import Settings

_SEMVER = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
_REPO_ROOT = Path(__file__).resolve().parents[2]
_BACKEND_ROOT = Path(__file__).resolve().parents[1]


def read_version() -> str:
    """Resolve the version: env override, then ../VERSION, then backend/VERSION."""
    override = os.environ.get("GEOALARM_VERSION", "").strip()
    candidates = [override] if override else []
    for path in (_REPO_ROOT / "VERSION", _BACKEND_ROOT / "VERSION"):
        if path.is_file():
            candidates.append(path.read_text(encoding="utf-8").strip())
    for value in candidates:
        if _SEMVER.match(value):
            return value
    return "0.0.0"


def version_code(version: str) -> int:
    match = _SEMVER.match(version)
    if not match:
        return 0
    major, minor, patch = (int(p) for p in match.groups())
    return major * 10_000 + minor * 100 + patch


def _read_metadata() -> dict[str, Any]:
    path = _REPO_ROOT / "release-metadata.json"
    if not path.is_file():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def build_version_payload(settings: Settings) -> dict[str, Any]:
    meta = _read_metadata()
    version = read_version()
    return {
        "latestVersion": version,
        "versionCode": version_code(version),
        "releaseDate": settings.release_date_override or meta.get("releaseDate"),
        "minimumAndroidVersion": settings.min_android_override or meta.get("minimumAndroidVersion", "8.0"),
        "downloadUrl": settings.apk_url or None,
        "sha256": settings.apk_sha256 or None,
        "buildType": meta.get("buildType", "debug"),
        "releaseNotes": meta.get("releaseNotes", []),
        "knownLimitations": meta.get("knownLimitations", []),
    }
