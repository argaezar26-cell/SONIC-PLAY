# SONICFLOW — Premium Hi-Res Lossless Audio Player

Native Android audio player engineered in Kotlin, Jetpack Compose (Material 3), and Jetpack Media3 ExoPlayer with a 64-bit floating-point Biquad Parametric DSP engine.

## Highlights

- **Lossless & Hi-Res Audio Engine**: Media3 ExoPlayer (`FLAC`, `WAV PCM`, `MP3`, `AAC/M4A`, `OGG Vorbis`, `OPUS`, `ALAC`) with direct binary FLAC `STREAMINFO` / Vorbis `REPLAYGAIN` & WAV `fmt ` header parsing, gapless playback, and true **Bit-Transparent** vs **64-bit DSP** switching.
- **10–15 Band Parametric Equalizer**: Real-time 64-bit Direct Form II Transposed Biquad filters (`Peaking`, `Low Shelf`, `High Shelf`, `High-Pass`, `Low-Pass`, `Notch`), interactive frequency-response graph, Preamp + Auto-Headroom, Clipping Detector, Before/After A/B bypass, 9 built-in presets, custom presets, and JSON import/export.
- **Advanced DSP Rack**: Bauer stereo crossfeed, ReplayGain (Track/Album), Digital gain trim, Soft-knee Peak Limiter, and Stereo balance / Mono compatibility check.
- **Output & Bit-Perfect Mixer Inspector**: `AudioManager` / `AudioDeviceInfo` + Android API 34+ `AudioMixerAttributes` verification for built-in JBL stereo speakers, USB-C adapters, external USB DACs, and Bluetooth outputs, with safe pause on disconnect.
- **Audio Test Lab & Quality Diagnostics**: Real `AudioTrack` PCM tone/sweep generator (L/R/Stereo/Mono, 20 Hz–20 kHz sweep, sine wave, clipping test with safety volume check) and real hardware/codec/DSP/service diagnostic probes.
- **100% Local & Private**: Storage Access Framework (`OpenDocumentTree`), Room Database, DataStore Preferences, zero network calls, zero account requirements.

## Build & Install APK (Infinix Note 40 4G / Android 14+)

1. Export the project ZIP or push to GitHub using the AI Studio menu.
2. Build the debug APK locally with Android Studio or Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
   The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
3. Install onto your Infinix Note 40 4G via USB Debugging (`adb install -r app/build/outputs/apk/debug/app-debug.apk`) or copy the APK to your device and install.
4. For automated **GitHub Releases**, push a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) — `.github/workflows/release.yml` runs unit/Robolectric tests, builds the APK, and attaches it to a new GitHub Release automatically.
