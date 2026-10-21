# ECHO NOW

ECHO NOW is a Kotlin Multiplatform streaming client with a shared Compose UI, domain layer, persistent local preferences, Android/Android TV playback through Media3, and Windows/Linux playback through VLCJ. Settings → Device includes platform-reported device, operating-system, network, IP, and MAC information where available; Android intentionally does not expose a hardware MAC address because of platform privacy restrictions.

## Requirements

- JDK 17 or later
- Android Studio with Android SDK Platform 37 and the Android SDK Build Tools installed for Android builds
- VLC 3.x installed on the desktop for VLCJ playback
- A populated project-root `config.cfg` for TMDB credentials and app metadata

ECHO NOW loads popular movies, popular TV series, search results, title metadata, genres, and episode listings directly from TMDB. The project-root `config.cfg` is the canonical source for the app name, version, website, TMDB credentials, and SubDL credential; Gradle reads it for Android and desktop builds. Keep this file private: it is ignored by Git, and credentials bundled into a client app can be recovered from its binaries. Android uses registered stream extractors; integrations must only be used where authorized. The Tizen app does not port those extractors and keeps its provider registry empty until an authorized, browser-compatible source is supplied.

The home screen stores playback position locally and presents a Continue Watching row. Player controls hide during playback and can be restored with a tap or remote input. Subtitle search uses the configured SubDL key unless a personal key is entered under Settings; an OpenSubtitles API key can also be configured there. Subtitle files can be searched, downloaded, or selected from the device, with SRT/VTT cue rendering, language preference, auto-apply, timing offset, and style controls. Android hides system bars for an immersive full-screen app experience; landscape layouts use a navigation rail and portrait phone layouts use bottom navigation.

Title details resume movies and series from saved progress. Series episode rows play on selection, show watched progress, and offer Resume or Watch again when partially watched. Settings categories are collapsed by default and subtitle language can be selected from a country list.

## Build and run

The Gradle wrapper is the canonical build entry point.

### Android phone and tablet

Windows PowerShell:

```powershell
.\gradlew.bat :androidApp:installDebug
```

Linux:

```bash
./gradlew :androidApp:installDebug
```

Open the `composeApp` Android run configuration in Android Studio to select a connected device or emulator.

### Android TV

Build and install the same Android app. The manifest registers both the standard launcher and Leanback launcher categories and disables the touchscreen requirement.

```powershell
.\gradlew.bat :androidApp:installDebug
```

Use a TV emulator/device in Android Studio. D-pad navigation, Select, and Back are handled in the shared Compose screens and player.

### Samsung Tizen TV

The separate native Tizen web app is in `tizenApp`. Its JavaScript is organized into shared core/preferences, reusable media-card components, screen modules, player modules, and app event wiring; ordered classic scripts preserve Samsung Web Engine compatibility. Its manifest currently declares a Tizen 4.0 minimum to allow testing on older Samsung TVs; compatibility on those TVs remains unverified. The app includes remote navigation, Home/Movies/Series/Search routes, a Home favorites carousel, genre filtering, seven catalog sort modes and paged horizontal carousels, live search with result counts and favorite controls, backdrop-led title details with cast/trailers and episode cards, profile creation/editing/removal with profile-specific favorites and watch progress, and settings categories for appearance, interface, subtitles, date/time, device, and application. Device settings report model, firmware, network, IP, and MAC details through available Samsung/Tizen APIs; unavailable APIs are identified rather than inferred. Remote navigation moves focus without changing a control; OK activates buttons, expands settings categories, opens dropdowns, or enters control-edit mode. Only one settings category stays expanded. Open dropdowns use arrows to highlight options, OK commits, and Back/Escape cancels. Focus stays within active dialogs, left/right navigation connects the rail with page content, and focus is shown with shape-matched accent backgrounds rather than outline rings. Its Android-inspired full-screen player includes a fullscreen toggle, D-pad transport, resume, seek, quality/source selection, playback speed, aspect-ratio modes, intro/credits skip, subtitle controls, and series up-next controls. Tizen's media engine and Android-only system services remain platform-dependent and are not one-to-one replacements.

The Tizen preview must be started with `node .\tizenApp\dev-server.js`; it reads the project-root `config.cfg` and serves build configuration only to the same-origin local preview. The server binds to `127.0.0.1`, validates required app and API settings, and never writes credentials into the source tree or logs them. Android and desktop builds also read the same root config through Gradle. To build a Tizen WGT, run `powershell -ExecutionPolicy Bypass -File .\tizenApp\build.ps1`. It reads `APP_NAME`, `APP_VERSION`, `APP_WEB`, `THEMOVIEDB_API_KEY`, `THEMOVIEDB_API_READ_ACCESS_TOKEN`, and `SUBDL_API_KEY` from `config.cfg`, and signs with the `ECHONOW` Tizen security profile by default (`-SecurityProfile` can select another installed profile). Tizen settings do not prompt for credentials; all builds and previews use the configured values. A WGT embeds recoverable credentials, so keep it private and never upload or share it.

The stream-provider registry includes the 17 configured providers and resolves available streams for supported titles. Playback uses Samsung AVPlay with Hls.js fallback and retries alternate sources after failures; subtitle search uses the configured SubDL key in the locally configured build. The app was installed and launched on the Tizen 10.0 Samsung TV emulator using the `ECHONOW` signing profile, and a selected title rendered advancing video frames. The Tizen 4.0 minimum is intended to allow testing on the 2018 Samsung TV generation, but physical-TV installation and playback remain unverified. Tizen reuses Android artwork, palette, gradient, control placement, focus treatments, and player interactions, but its media engine cannot reproduce Android Media3 codecs, DRM, arbitrary request headers, or Android-only integrations. Device pairing and some Android-specific services remain unavailable. This is a Samsung Tizen Web application and its install package is a `.wgt`, not an Android APK.

With Tizen Studio installed and an active development signing profile configured:

```powershell
tizen build-web -- .\tizenApp
tizen package -t wgt -- .\tizenApp
```

Install the signed `.wgt` from Tizen Studio or with `tizen install` after enabling TV Developer Mode and connecting over SDB. Public Samsung Store distribution requires a Samsung Seller Office signing profile and review. Tizen TV installation and playback have not been verified on a physical TV.

### Windows desktop

Install VLC 3.x and ensure `libvlc.dll` is discoverable by the application. Then run:

```powershell
.\gradlew.bat :composeApp:run
```

### Linux desktop

Install VLC 3.x and its development/runtime libraries (including `libvlc`), then run:

```bash
./gradlew :composeApp:run
```

Create desktop packages with `:composeApp:packageMsi` on Windows or `:composeApp:packageDeb` on Linux.

## Architecture

- `domain`: media entities, repository contracts, and use cases
- `data`: TMDB catalog/search/detail adapter, in-memory catalog cache, AndroidX DataStore preferences on Android, and Java Preferences on desktop
- `presentation`: StateFlow-backed ViewModels, adaptive Compose screens, typed navigation, and RokuFocus-based horizontal rails
- `androidMain`: Android entry point, Media3 player, and Android download intents
- `desktopMain`: Compose Desktop entry point and VLCJ player/download actions

Favorites, preferences, and watch progress are stored through repository interfaces; the Android DataStore and desktop Preferences adapters share a common `KeyValueStore` interface.
