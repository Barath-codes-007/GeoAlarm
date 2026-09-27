#!/usr/bin/env python3
"""Single source of truth for the GeoAlarm version.

The `VERSION` file at the repository root is the only place the version is
edited by hand. This script derives everything else from it:

  * web/version.json       (read by the website; generated, git-ignored)
  * Android versionName/versionCode (computed inside android/app/build.gradle.kts)
  * Backend /api/version   (computed inside backend/app/release.py)

Usage:
  python scripts/sync_version.py           # (re)generate web/version.json
  python scripts/sync_version.py --check   # verify consistency, exit 1 on mismatch
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEMVER = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")


def read_version() -> str:
    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    if not SEMVER.match(version):
        raise SystemExit(f"VERSION must be MAJOR.MINOR.PATCH, got {version!r}")
    return version


def version_code(version: str) -> int:
    major, minor, patch = (int(p) for p in SEMVER.match(version).groups())
    return major * 10_000 + minor * 100 + patch


def build_payload(version: str) -> dict:
    meta = json.loads((ROOT / "release-metadata.json").read_text(encoding="utf-8"))
    return {"version": version, "versionCode": version_code(version), **meta}


def check(version: str) -> list[str]:
    problems: list[str] = []
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(r"^## \[(\d+\.\d+\.\d+)\]", changelog, re.MULTILINE)
    if not match:
        problems.append("CHANGELOG.md has no '## [x.y.z]' entry")
    elif match.group(1) != version:
        problems.append(f"CHANGELOG top entry {match.group(1)} != VERSION {version}")
    gradle = (ROOT / "android" / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    if re.search(r'versionName\s*=\s*"\d', gradle):
        problems.append("android/app/build.gradle.kts hard-codes versionName; it must read ../../VERSION")
    return problems


def main() -> int:
    version = read_version()
    if "--check" in sys.argv:
        problems = check(version)
        for p in problems:
            print("FAIL:", p)
        print("OK: version", version) if not problems else None
        return 1 if problems else 0
    out = ROOT / "web" / "version.json"
    out.write_text(json.dumps(build_payload(version), indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out.relative_to(ROOT)} for version {version}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
