# Profiles and saved data

ECHO NOW supports up to four local profiles. Each profile has a stable generated ID, a nickname, and an avatar selected from the piccdn image library. Avatar thumbnails are loaded on demand from the repository's documented GitHub Raw CDN URL pattern; the picker browses the square `anime`, `girls`, `boys`, `couples`, and `server-icons` categories. piccdn is MIT-licensed; see https://github.com/itsfizys/piccdn. Legacy emoji avatar IDs remain supported for existing profiles.

On first profile creation, the app copies the existing single-user `preferences`, `favorites`, and `watch_progress` values to that profile. The migration records a pending profile ID before copying, only writes destination keys that are absent, and marks completion before removing the legacy keys. This makes a retry reuse the same destination and preserves already-copied values.

Catalog responses and their in-memory cache remain shared because they are public media metadata, not personal data. Device pairing remains device-wide because paired devices identify and connect to the app installation, not to a viewing profile. Profile removal deletes that profile's settings, favorites, and watch progress; removing the final profile returns the app to required profile setup.
