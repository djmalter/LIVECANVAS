# Google Play Console Policy & Data Safety Checklist

This checklist confirms compliance with Google Play Developer Program policies for LiveCanvas Studio:

### 1. Foreground Service Declarations
- [x] Declared specific types in `AndroidManifest.xml`: `camera`, `microphone`, `mediaProjection`.
- [x] Persistent notification displayed whenever recording or streaming is active (`NotificationHelper`).
- [x] Notification includes an explicit "Stop Session" user action.

### 2. Permissions & Media Access
- [x] Uses the zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- [x] Camera (`CAMERA`) and Microphone (`RECORD_AUDIO`) requested with explicit in-app rationale before use.
- [x] No broad storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`) requested.
- [x] Notifications permission (`POST_NOTIFICATIONS`) requested appropriately for Android 13+.

### 3. MediaProjection & Screen Capture
- [x] Android system consent dialogue triggered via `MediaProjectionManager.createScreenCaptureIntent()`.
- [x] Foreground service started before projection capture as required on modern Android versions.
- [x] Immediate termination when the user halts projection or stops from the persistent notification.

### 4. System Integrity & Device Security
- [x] No root or bypass modules (Magisk, Frida, Xposed, LSPosed, Shizuku).
- [x] No system camera HAL interception or cross-app virtual camera injection.
- [x] No misleading affordances or deceptive recording tools.
