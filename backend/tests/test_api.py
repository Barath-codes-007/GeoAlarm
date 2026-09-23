from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


def make_client(**overrides) -> TestClient:
    base = dict(
        env="test",
        cors_origins=("https://example.github.io",),
        apk_url="",
        apk_sha256="",
        release_date_override="",
        min_android_override="",
        log_level="WARNING",
    )
    base.update(overrides)
    return TestClient(create_app(Settings(**base)))


def test_health():
    r = make_client().get("/api/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["version"].count(".") == 2


def test_version_matches_repo_version_file():
    from pathlib import Path

    expected = (Path(__file__).resolve().parents[2] / "VERSION").read_text().strip()
    body = make_client().get("/api/version").json()
    assert body["latestVersion"] == expected
    assert body["downloadUrl"] is None  # no fake APK link when unconfigured
    assert isinstance(body["releaseNotes"], list)
    assert body["minimumAndroidVersion"] == "8.0"


def test_version_exposes_configured_https_url_only():
    ok = make_client(apk_url="https://example.com/GeoAlarm.apk").get("/api/version").json()
    assert ok["downloadUrl"] == "https://example.com/GeoAlarm.apk"


def test_insecure_apk_url_is_rejected_by_loader(monkeypatch):
    from app.config import load_settings

    monkeypatch.setenv("GEOALARM_APK_URL", "http://insecure.example/app.apk")
    assert load_settings().apk_url == ""


def test_config_is_public_and_privacy_safe():
    body = make_client().get("/api/config").json()
    assert body["privacy"]["locationReceivedByServer"] is False
    assert body["downloadAvailable"] is False


def test_cors_allows_only_configured_origin():
    c = make_client()
    good = c.get("/api/health", headers={"Origin": "https://example.github.io"})
    assert good.headers.get("access-control-allow-origin") == "https://example.github.io"
    bad = c.get("/api/health", headers={"Origin": "https://evil.example"})
    assert "access-control-allow-origin" not in bad.headers


def test_unknown_route_returns_json_error():
    r = make_client().get("/api/nope")
    assert r.status_code == 404
    assert r.json()["error"]["code"] == "not_found"


def test_wildcard_cors_removed_in_production(monkeypatch):
    from app.config import load_settings

    monkeypatch.setenv("GEOALARM_ENV", "production")
    monkeypatch.setenv("GEOALARM_CORS_ORIGINS", "*,https://a.example")
    assert load_settings().cors_origins == ("https://a.example",)


def test_production_hides_docs():
    c = make_client(env="production")
    assert c.get("/api/docs").status_code == 404
