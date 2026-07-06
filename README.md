# Severus

<img src="images/screenshot-0.png"
      alt="closeup"
      width="500"/>

## About

A lightweight, minimal Android keyboard focused on simplicity and performance.

### Features

- Small size (<1MB)
- Adjustable keyboard height for more screen space
- Number row
- Swipe space to move pointer
- Delete swipe
- Custom theme colors
- Minimal permissions (only Vibrate)
- Ads-free

### What it doesn't have

- Emojis
- GIFs
- Spell checker
- Swipe typing

## Building

### Prerequisites

- JDK 17+
- Android SDK (set `ANDROID_HOME` environment variable)

### Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

Output APKs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## License

Licensed under Apache License Version 2.0

Based on AOSP LatinIME keyboard. Original source: https://android.googlesource.com/platform/packages/inputmethods/LatinIME/
