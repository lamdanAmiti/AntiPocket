<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Anti Pocket" width="120" height="120">
</p>

<h1 align="center">Anti Pocket</h1>

<p align="center">
  <strong>Prevent accidental pocket dials and unauthorized calls</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#installation">Installation</a> •
  <a href="#permissions">Permissions</a> •
  <a href="#license">License</a>
</p>

---

## Features

### Secure Calls
Require a slider confirmation before any outgoing call is placed. This prevents accidental pocket dials and ensures you actually intend to make each call.

### Only When In Pocket
Limit the call confirmation requirement to only when your phone detects it's in a pocket (using proximity and light sensors). Calls made while actively using your phone go through normally.

### Anti Pocket Mode
Automatically show an unlock slider when your phone enters a pocket. This helps prevent accidental screen interactions while your phone is in your pocket.

### Lock Device
Automatically lock your device when it enters a pocket. Uses Android's Accessibility Service to lock the screen while preserving fingerprint unlock capability.

### Hide Launcher Icon
Hide the app from your launcher for privacy. The app can be reopened by dialing the secret code `*#*#2684#*#*` (spells "ANTI" on a phone keypad).

---

## Screenshots

<p align="center">
  <img src="screenshots/Screenshot_20260110-182552.png" width="250" alt="Main Screen">
  <img src="screenshots/Screenshot_20260110-182606.png" width="250" alt="Settings">
  <img src="screenshots/Screenshot_20260110-182617.png" width="250" alt="Slider">
</p>

---

## Installation

### From Source

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AntiPocket.git
   ```

2. Open the project in Android Studio

3. Build and install:
   ```bash
   ./gradlew installDebug
   ```

### Requirements

- Android 10 (API 29) or higher
- Permissions must be granted for full functionality

---

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `READ_PHONE_STATE` | Detect outgoing calls |
| `CALL_PHONE` | Place calls after confirmation |
| `ANSWER_PHONE_CALLS` | Call management |
| `POST_NOTIFICATIONS` | Show foreground service notification |
| `SYSTEM_ALERT_WINDOW` | Display slider overlay |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep pocket detection running |
| `RECEIVE_BOOT_COMPLETED` | Restart service after device reboot |

### Special Permissions

- **Call Redirection Role**: Required for intercepting outgoing calls
- **Accessibility Service** (optional): Required for the "Lock Device" feature

---

## How It Works

1. **Call Interception**: When you initiate an outgoing call, the app intercepts it using Android's Call Redirection API
2. **Confirmation**: A full-screen slider appears requiring you to slide down to confirm the call
3. **Pocket Detection**: Uses proximity sensor (object nearby) and light sensor (darkness) to determine if the phone is in a pocket
4. **Call Placement**: After confirmation, the original call is placed

---

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## License

```
MIT License

Copyright (c) 2025 Anti Pocket

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">
  Made with care to prevent pocket dials
</p>
