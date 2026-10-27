<div align="center">

  <img src="branding/1000086209.png" alt="ECHO NOW Logo" width="560">



  **A modern, cross-platform media streaming client**

  [![GitHub Release](https://img.shields.io/github/v/release/ShadyDevelopment/ECHO.NOW?style=flat-square&color=008080)](https://github.com/ShadyDevelopment/ECHO.NOW/releases)
  [![Platform - Android](https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV-3DDC84?style=flat-square&logo=android&logoColor=white)](#android--android-tv)
  [![Platform - Desktop](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-0078D4?style=flat-square&logo=windows&logoColor=white)](#desktop-windows--linux)
  [![Platform - Tizen](https://img.shields.io/badge/Platform-Samsung%20Tizen-1B6AC6?style=flat-square&logo=samsung&logoColor=white)](#samsung-tizen-tv)
  [![License](https://img.shields.io/badge/Access-Closed%20Source-red?style=flat-square)](#)

</div>

---

> **Notice:** This repository is an official release-only distribution point for pre-compiled **ECHO NOW** binaries and install packages. Source code is closed-source and maintained privately.

---

## Overview

**ECHO NOW** delivers a seamless cinematic experience across mobile devices, desktop computers, and TV screens:

- **Unified Interface:** Fluid, adaptive UI with native remote navigation support.
- **Cross-Platform Playback:** Hardware-accelerated streaming.
- **Rich Media Catalog:** Real-time metadata, trending titles, genre filters, cast details, and backdrop-led screen layouts.
- **Smart Progress Tracking:** Persistent watch progress, "Continue Watching" shelves, and instant resume support.
- **Integrated Subtitles:** Automatic search, download, and synchronization with SRT/VTT formatting, timing offsets, and styling controls (Additional SubDL & OpenSubtitles configuration support).

---

## Platform Downloads & Installation

Binaries can be found under the **[Releases](../../releases)** section.

### Android & Android TV

*Supports phones, tablets, and Android TV / Google TV (Leanback launcher enabled).*

- **Package:** `ECHO-NOW-*.apk`
- **Installation:**
  1. Download the `.apk` from the latest release.
  2. On your device, enable installation from unknown sources.
  3. Install the APK directly or sideload it to your Android TV using ADB:
     ```bash
     adb install -r ECHO-NOW-<version>.apk
     ```

### Desktop (Windows & Linux)

*Windows and Linux builds require [VLC Media Player 3.x](https://www.videolan.org/vlc/) installed on your machine for the underlying audio/video codecs (`libvlc`).*

- **Windows:** Download and run `ECHO-NOW-*.msi` or the portable `.zip`.
- **Linux:** Download `ECHO-NOW-*.deb` or package archive:
  ```bash
  sudo apt install ./ECHO-NOW-<version>.deb
