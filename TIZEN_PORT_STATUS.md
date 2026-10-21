# Tizen TV port status

## Current implementation

`tizenApp` is a separate Samsung TV Tizen web application; it is not an Android APK and does not reuse the Android Compose UI or Media3 player. The app targets Tizen 6.5 and includes:

- Home, Movies, Series, Search, and My List screens backed by a TMDB credential entered and stored locally on the TV.
- Genre filtering, server-side sorting, pagination, cast/trailer details, and season/episode browsing.
- TV remote navigation, Back handling, media-key controls, profiles, profile-specific favorites and viewing progress, resume cards, and preferences.
- An Android-matching full-screen player mirroring `PlayerScreen.kt`: top bar with back navigation and centered media/episode title; seek bar with elapsed and remaining time; centered circular transport controls (Replay 10s 48px, Play/Pause 64px, Forward 10s 48px) using official Android Material SVG icons; right-aligned utility bar with Subtitle controls (44px), Quality selector (44px button with quality label and floating popup dropdown menu for instant quality switching), Auto-play next episode toggle (44px), Aspect-ratio cycling (44px; cycling Fit/Contain, Crop/Cover, and Stretch/Fill), and a fullscreen toggle using the browser Fullscreen API with a Tizen-compatible layout fallback.
- Outlined 'Skip Intro' and 'Skip Credits' action buttons matching Android's pill button with `Icons.Rounded.SkipNext` SVG icon.
- Up Next overlay card matching Android `PlayerScreen.kt` with next episode preview thumbnail (82px), episode title, series name, linear progress countdown bar, remaining seconds indicator, and 'Play Now' / 'Cancel' action buttons.
- Automatic preferred-language subtitle selection on HTML video playback, with SubDL search and auto-download when an embedded track in the configured language is unavailable. On AVPlay, downloaded subtitle cues are rendered as a styled overlay; the subtitle control can toggle the overlay.
- Dual-engine playback architecture: Native Samsung hardware `webapis.avplay` (`<object id="av-player" type="application/avplayer">`) for direct hardware decoding of HLS, AES-128, and multi-channel audio without MSE memory limits, paired with bundled `Hls.js` MSE player fallback with PTS gap unfreezer, stall detection, audio codec swapping, and multi-source failover.
- Fix for "The TV could not decode this stream" and player stuck at 0:00: Included `<object id="av-player">` in the DOM with transparent overlay styling, resolved `stopCurrentPlayback()` state machine to cleanly close previous AVPlay instances, suppressed spurious DOM error events on `<video>` during engine switches, and added active timestamp nudging past initial non-zero presentation timestamp gaps (PTS).
- Exact Android UI design system parity: theme palette (`#09090D` background, `#101014` nav rail, `#14141B` surfaces, `#607D8B` primary accent, `#31A7A0` tertiary focus highlight and glow), rounded TV navigation rail with Android's official `Icons.Rounded.Tv`, `Home`, `Movie`, `Search`, and `Settings` SVG icons, 14px rounded poster cards with 2:3 aspect ratio and film/series tags, and responsive TV spatial navigation.

The stream-provider registry matches the Android application parity, featuring all 17 stream providers (VidSrc, VixSrc, FMovies4U, VidZee, AnyEmbed, CineSu, FShare, Icefy, Peachify, Popr, StreamMafia, Tulnex, VidApi, VidEasy, VidNest, VidRock, and 02MovieDownloader), complete with cryptographic handlers (AES-CBC, AES-GCM, PBKDF2, MD5, OpenSSL EVP_BytesToKey, custom Base64), proxy unwrapping via ProxyUnwrapper, quality normalization, language preference sorting, and provider subtitle passthrough. Stream resolution is fully aligned with Android's `DefaultCatalogRepository.kt`: movies and series automatically resolve valid streams, with robust series season/episode resolution, progress tracking, and up-next handling.

## Parity and verification

The navigation rail, icons, artwork, color scheme, poster dimensions, player layout, controls positioning, and stream providers now match Android closely.
Playback of HLS (`.m3u8`) and MP4 video streams is driven through Samsung hardware AVPlay with Hls.js fallback and multi-source error recovery, preventing decoding failures and 0:00 freeze on Tizen TV.

Run `powershell -ExecutionPolicy Bypass -File .\tizenApp\build.ps1 -Public` to generate a credential-free package for local testing, or without `-Public` for a configured package.

The configured build uses the `ECHONOW` signing profile by default. ECHO NOW was installed and launched on a Tizen 10.0 Samsung TV emulator, and a selected title rendered advancing video frames. The app targets Tizen 6.5; physical-TV playback is not yet verified.

Current build artifacts:
- Credential-free: `ECHO-NOW-0.2.0-tizen-6.5-public-dev.wgt` (SHA-256: `21429D9B3F202EF826E06E393BC65B0E9B3EFAE212A5B6F9EE99D162368935EF`)
- Configured: `ECHO-NOW-0.2.0-tizen-6.5-configured-dev.wgt` (SHA-256: `1E31BCEE4B6644FAD318BEE24543F78C72C37A321845333F27D2FA3359519434`)
- Playback & Stream Resolution: Dual-engine AVPlay hardware acceleration with Hls.js MSE fallback, sanitized network request headers removing browser-forbidden unsafe headers (`Origin`, `User-Agent`, `Referer`) that caused Samsung TV Web Engine preflight and security rejection, W3C WARP `XMLHttpRequest` fallback under `<access origin="*" subdomains="true"/>`, public network privileges in `config.xml` (`network.public`, `network.internet`, and `allow-navigation`), prioritized reliable high-availability providers (VidApi and VixSrc) with 6s timeout, and multi-source quality selection (1080p, 720p, Auto).