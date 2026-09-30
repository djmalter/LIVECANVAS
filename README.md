# LIVE CANVAS STUDIO

> **Tagline:** Create, preview, record, and stream your own media scenes.

LiveCanvas Studio is an original native Android live-media production tool that enables creators to compose, transform, record, and broadcast media scenes from their device.

---

## 1. What This App Can and Cannot Do

> **Platform Boundary Notice:**  
> LiveCanvas Studio creates and streams scenes from within its own recording and streaming workflow. It does **not** replace the Android system camera or inject media into third-party video-call applications.

- **What LiveCanvas Studio DOES:**
  - Composes scenes from device camera (front/back with torch & mirroring), local photos, local videos, user-entered network media streams, screen capture, and privacy slates.
  - Applies real-time visual transforms (pan, pinch zoom, rotation, horizontal/vertical flip, fit/fill/stretch, opacity, watermark text, safe-area alignment guides).
  - Encodes and saves scenes locally to MP4 via the Android MediaStore.
  - Publishes live RTMP transmissions to user-authorized endpoints with live metrics (bitrate, FPS, dropped frame statistics, elapsed duration).
  - Protects user privacy with zero hidden telemetry, no account requirements, and explicit user-initiated permission workflows.

- **What LiveCanvas Studio DOES NOT do:**
  - Does NOT hook into the Android Camera HAL or virtual camera driver frameworks.
  - Does NOT inject mock feeds or simulated camera frames into third-party communication apps (e.g. WhatsApp, Zoom, Teams, Meet).
  - Does NOT use root, Xposed, Magisk, Frida, Shizuku, or reflection-based bypasses.
  - Does NOT bypass Android runtime permissions, foreground service declarations, or MediaProjection system consent dialogues.

---

## 2. Technical Stack

- **Language:** Kotlin (100%)
- **Target SDK:** 36 (Android 15+) | **Min SDK:** 24
- **UI Framework:** Jetpack Compose with Material 3 Dark-Mode-First Design
- **Architecture:** Clean MVVM with repository pattern and unidirectional data flow
- **Local Persistence:** Room Database for scenes, recent media, and stream endpoints
- **Preferences:** Jetpack DataStore Preferences
- **Camera:** CameraX (Core, Camera2, Lifecycle, View)
- **Image Pipeline:** Coil Compose
- **Media Projection:** Android MediaProjection with typed foreground service
- **Streaming:** Pluggable `StreamingEngine` interface with `FakeStreamingEngine` simulation and production socket adapter boundary

---

## 3. How to Build & Export

In Google AI Studio:
1. **GitHub Export:** Click the Settings / Project menu in AI Studio to connect and push the project directly to your GitHub repository.
2. **Download ZIP:** Choose "Export Project (ZIP)" from the project options menu.
3. **Generate APK / AAB:** Use the AI Studio build menu to produce downloadable debug APKs or release Android App Bundles (AAB).
4. **Local Command Line Build:**
   ```bash
   ./gradlew assembleDebug    # Generates app/build/outputs/apk/debug/app-debug.apk
   ./gradlew bundleRelease    # Generates app/build/outputs/bundle/release/app-release.aab
   ```
