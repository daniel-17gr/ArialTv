# Arial TV (`arial tv.`)

A standalone **Android TV** streaming app crafted with a minimalist **Nothing OS × YouTube TV** aesthetic — dot-matrix typography, crisp monochrome + Signal Red (`#E11D25`) focus accents, borderless cinema posters, live catalog filtering, customizable subtitles, and resilient Media3 + FFmpeg playback.

Developed by **Auris Daniel** ([@daniel-17gr](https://github.com/daniel-17gr)).

---

## Highlights

- **10-Foot Android TV Experience**: Built with Jetpack Compose, Material 3 for TV (`androidx.tv:tv-material`), and collapsible YouTube TV–style left rail navigation.
- **YouTube TV Clean Design**:
  - Custom widescreen launch screen artwork (`splash_bg.webp`) with `arial tv.` branding, 3-dot pulse, and clean version/developer footer.
  - Pure YouTube TV focus (`#F1F1F1` white fill on focus) + warm light-orange (`#FFE4CC`) / black selected accent containers, with zero harsh outline borders.
  - Full-bleed Hero backdrop starting right from the top edge behind a transparent `"developed by Auris"` top bar.
  - Edge-to-edge poster rails with content-type badges (`MOVIE`, `SERIES`, `DOC`, `MUSIC`) and a custom framed portrait showcase inside the remote Menu Drawer.
- **Smart Home Discovery, Live Filters & Voice Search**:
  - Curated parallel-loaded Home rails (Trending Movies, Top Rated Cinema, Trending Series, New Releases, Action Blockbusters) plus personalized **"Because You Watched..."** and **"More Like This"** rails on Detail pages.
  - Filter Home, Films, and Series live by Genre, Year, and Sort (`Trending`, `Latest`, `2026`, `2025`).
  - TV remote Voice Search mic button, instant live search suggestions while typing, persisted Search History with 1-click clear, `All / Films / Series` filter pills, and live API Trending Now discovery.
- **Cinema Player (Media3 ExoPlayer + Jellyfin FFmpeg)**:
  - **Full Audio Language Switcher**: Switch between dubbed audio languages (Original, English, Français, Español, Arabic, Hindi, Português) seamlessly at the exact timestamp, switch embedded multi-track audio (`5.1` / `Stereo`) with zero reload, and auto-select your preferred audio language in Settings.
  - Bundled `org.jellyfin.media3:media3-ffmpeg-decoder` for broad audio codec support (AC3, E-AC3, DTS, TrueHD, FLAC, Vorbis, Opus, etc.).
  - Automatic software video decoder failover + automatic stream/H.264 failover on playback errors, plus a fully remote-focusable recovery dialog.
  - In-player **Volume Control** drawer (`-10%`, `+10%`, `Mute`, quick presets) + vertical swipe brightness/volume gestures & double-tap ±10s seek.
  - Live **Subtitle Styling** (Size, Color, Background, Position) + **Sync Offset** (`-0.5s` / `Reset` / `+0.5s`).
  - In-player **Episodes & Seasons** drawer, floating **Next Episode** pill in the final 45s, and 10-second Up Next autoplay.
  - **Sleep Timer** (`Off`, `15m`, `30m`, `45m`, `1h`, `1h 30m`, `2h`, `3h`, `4h`), Speed (`0.5x–2.0x`), Quality & Decoder Mode, and Aspect Ratio (`Fit / Zoom / Stretch`).
- **TV Remote Quick Menu & GitHub Update Check**: Press `MENU` anywhere for instant navigation, catalog refresh, sleep timer, language toggle (`English` / `Français`), or check for newer releases directly from **Settings → About**.
- **Optimized for Low-RAM Android TV**: R8 code & resource shrinking, zero-relayout overlay navigation rail, isolated timer/hero state flows, lean 10% RAM Coil `RGB_565` image cache with `onTrimMemory` eviction, parallel API loading, and 10-minute TTL caching.

---

## Installation (Android TV / Google TV)

Requires **Android 8.0+ (API 26)** — verified for **Android TV 14**.

1. Download **`ArialTv-release.apk`** from the [GitHub Releases](https://github.com/daniel-17gr/ArialTv/releases) page.
2. Transfer or sideload to your Android TV via **BetaDrop**, **Send Files to TV**, **Downloader**, or `adb install ArialTv-release.apk`.
3. Enable **"Install unknown apps"** for your installer if prompted, then launch **Arial TV** from your TV Apps row.

---

## Building from Source

### Prerequisites
- Android Studio (with bundled JetBrains Runtime JDK 21) or JDK 21
- Android SDK Platform 37 (compileSdk 37, targetSdk 36, minSdk 26)

### Build Release APK & Run Tests

```bash
./gradlew :app:assembleRelease :app:testDebugUnitTest
```

Output APK:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Tech Stack

- **Kotlin** + **Jetpack Compose** + **androidx.tv:tv-material**
- **AndroidX Media3 ExoPlayer** (`1.9.0`) + **Jellyfin Media3 FFmpeg Decoder** (`1.9.0+1`)
- **OkHttp 4** + **Coil 2** (`RGB_565` TV-optimized `ImageLoader`)
- **Coroutines & StateFlow**

---

## Credits

- **Developer**: Auris Daniel — [github.com/daniel-17gr](https://github.com/daniel-17gr)
- **Repository**: [github.com/daniel-17gr/ArialTv](https://github.com/daniel-17gr/ArialTv)
