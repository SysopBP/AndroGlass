# AndroGlass

**AndroGlass** is an experimental Android 17 status-bar customization project powered by Xposed, focused on glass UI, dynamic status elements, expanded tiles, and deep SystemUI integration.

> **Preview status:** v0.1.0-preview.1 — early development and device testing.

## Goals

- Modern glass / frosted status-bar styling
- Dynamic status elements
- Expanded status-bar tiles
- Configurable camera-ring effects and accents
- Android 17 / modern Xposed integration
- Reversible SystemUI experiments with safe fallbacks
- Independent app and module architecture

## Current preview

The initial preview focuses on establishing a reliable SystemUI/Xposed foundation before expanding into full status-bar replacement and customization.

The prototype includes an experimental frosted clock capsule rendered inside SystemUI. Device compatibility is still being validated, particularly on Samsung Android 17 / One UI.

## Requirements

- Android 17 target environment
- Root/Xposed environment compatible with the modern Xposed API
- SystemUI module scope
- JDK 17 for source builds
- Android SDK 35

No KernelSU/Shizuku permission is required by AndroGlass itself unless a future feature explicitly requires it.

## Building

```sh
./gradlew assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions is intended to provide reproducible preview APK builds from the repository.

## Safety

SystemUI hooks can cause UI instability on unsupported firmware. Keep a working recovery/root configuration available while testing preview builds.

Experimental features should fail back to stock behavior when their target SystemUI component cannot be resolved.

## Roadmap

AndroGlass is intended to grow into a configurable Android status-bar platform with:

- Custom status groups
- More dynamic tiles
- Camera/cutout-aware visual effects
- Frosted and transparent surfaces
- Additional accent and ring styles
- Animation controls
- Expanded Android 17 SystemUI integration

## License

Copyright © 2026 AndroGlass contributors.

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

---

AndroGlass is an independent project and is not affiliated with Google, Samsung, or the Android Open Source Project.
