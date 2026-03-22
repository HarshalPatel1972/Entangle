# ⚡ Entangle

> Quantum entanglement — two particles instantly connected regardless of distance.
> Your devices should work the same way.

Share anything from any app on your phone.
It appears on your laptop instantly.
One tap. That's it.

## The Problem

Someone sends you a file on WhatsApp. WhatsApp desktop is broken.
You forward it to Telegram. Open Telegram on your laptop.
Download from there. That's 4 steps just to get a file.

Entangle makes it 1 step.

## How It Works

1. Install Entangle on your laptop — it runs in the system tray
2. Install Entangle on your Android phone
3. Share anything from any app — Entangle appears in the share sheet
4. Done. File is on your laptop.

## Requirements

- Both devices on the same WiFi network
- Windows 10+ or macOS 10.15+
- Android 8.0+

## Download

- [Windows (.exe)]()
- [macOS (.dmg)]()
- [Android (.apk)]()

## Build From Source

### Desktop
```bash
cd desktop
go build -ldflags="-H windowsgui" -o entangle.exe  # Windows
go build -o entangle-mac                             # macOS
```

### Android
```bash
cd android
./gradlew assembleRelease
```

## License

AGPL-3.0 — See LICENSE

---

*Built because the WhatsApp → Telegram → Desktop pipeline is embarrassing in 2026.*