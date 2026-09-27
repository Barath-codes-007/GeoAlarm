# GeoAlarm release ProGuard/R8 rules.
# Most modern libraries (Room via KSP, MapLibre, OkHttp, Compose) ship their own
# consumer rules, so this file only adds project-specific keeps.

# Keep Room-generated code and entities' field names (safety net; KSP already
# generates most of what's needed, but entity reflection during migrations benefits
# from this).
-keep class com.geoalarm.app.data.db.** { *; }

# Keep domain models: they're serialized nowhere today, but keeping them avoids
# any accidental obfuscation surprises if debugging a release build.
-keep class com.geoalarm.app.domain.model.** { *; }

-dontwarn org.maplibre.**
