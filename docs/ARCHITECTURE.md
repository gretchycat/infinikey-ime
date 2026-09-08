# Infinikey IME Developer Architecture

This document provides a technical specification of the internal architecture, engine components, asset pipelines, and data dispatches in **Infinikey IME**.

---

## 1. High-Level Architecture Overview

```
                                +-------------------------------------+
                                |   ProgrammerInputMethodService      |
                                |    (Android IME Lifecycle Engine)   |
                                +----+---------------+----------------+
                                     |               |
                                     v               v
                    +----------------+--+         +--+-------------------+
                    |   KeyboardView    |         | ClipboardHistory /   |
                    | (Render Surface)  |         | VoiceInput Overlay   |
                    +--------+----------+         +----------+-----------+
                             |                               |
            +----------------+----------------+              v
            |                |                |       +------+-------+
            v                v                v       | SttEngine /  |
    +-------+------+  +------+-------+  +-----+--------+ SherpaOnnx   |
    | KeyPopup     |  | LayoutParser |  | SoundPool &  +--------------+
    | Overlay      |  | (JSON Engine)|  | Haptic Engine|
    +--------------+  +--------------+  +--------------+
```

---

## 2. Core Service Component

### `ProgrammerInputMethodService` (`com.infinikey_ime`)
- **Role**: Primary entry point implementing Android `InputMethodService`.
- **Key Responsibilities**:
  - **InputConnection Dispatch**: Executes text insertion (`commitText`), keycode dispatch (`sendDownUpKeyEvents`), and IME editor action dispatches.
  - **Terminal Target Detection**: Evaluates `isTerminalTarget()` to distinguish raw terminal shells (`InputType.TYPE_NULL` or terminal package targets like Termux) from standard Android text fields.
  - **Clipboard Listening**: Registers `ClipboardManager.OnPrimaryClipChangedListener` to capture clipboard updates and persist up to 30 history items in `SharedPreferences`.
  - **Modifier State Manager**: Tracks `SHIFT`, `CTRL`, `ALT`, `SUPER`, and `META` states (`OFF`, `LATCHED` one-shot, `LOCKED`), generating composite `metaState` flags for input events.
  - **Screen Docking Manager**: Controls form factor mode transitions (`FULL_WIDTH_DOCKED`, `SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, `FLOATING`).
  - **Action Dispatcher**: Handles `KeyAction` execution for all input, state, geometry, UI, macro, and application launching actions.
  - **Macro Manager Integration**: Coordinates recording and replay of keystroke sequences to slots `M1`–`M10`.

---

## 3. Rendering Engine

### `KeyboardView` (`com.infinikey_ime.view`)
- **Role**: Custom canvas `View` responsible for layout rendering and touch interaction.
- **Key Responsibilities**:
  - **Layout Geometry Calculation**: Translates `DimensionValue.Ratio` and `DimensionValue.Absolute` units into exact pixel coordinates based on container width, height, and active form factor.
  - **Keycap & Label Rendering**: Draws staggered and ortholinear keycaps, primary text labels, secondary badges, spacer text, accessory cards, and native vector SVG icon paths (`drawSvgCopyIcon`, `drawSvgCutIcon`, `drawSvgPasteIcon`, `drawSvgSelectAllIcon`, `drawSvgPaperclipIcon`, `drawSvgClipboardIcon`, `drawSvgMicIcon`, `drawSvgTtsIcon`, `drawSvgKeyboardIcon`).
  - **Accessory Area Computation**: Calculates remaining screen width in `SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, and `SIDE_DOCKED` modes, embedding secondary accessory layouts (`navigation`, `mobile_number`, `function`, `macro`, `media`, `launcher`, `mobile_symbol`).
  - **Multi-Touch & Gesture Processing**: Processes surface touch gestures (two-finger swipes, pinch-out), directional key swipes (`onSwipeUp`, `onSwipeDown`, etc.), spacebar trackpad mode, and long-press popups.
  - **Audio & Haptic Triggers**: Emits sound pool key-down/up triggers and vibration haptic pulses.

---

## 4. Layout & Theme Engine

### `LayoutParser` (`com.infinikey_ime.engine`)
- **Role**: Declarative JSON layout parser and theme resolution engine.
- **Key Responsibilities**:
  - **JSON Parsing**: Parses root layout fields, metadata parameters (`accessoryLayout`, `accessoryText`, `accessoryImage`), row structures, and key descriptors.
  - **Theme Resolution**: Merges default layout styles with theme presets (`themes.json`, `themes/*.json`) and custom HSL/RGB user color themes via `ThemeManager`.
  - **Asset & Storage Sync**: `syncAndUpgradeDefaultLayouts()` synchronizes layout JSON descriptors between asset bundles and user external storage directories (`Android/data/com.infinikey_ime/files/layouts/`), ensuring user modifications are protected from accidental overrides.
  - **Dynamic Layout Generation**: Generates the dynamic `meta` picker layout and runtime `"emoji_recents"` layout.

---

## 5. Overlay & Widget Subsystem

The UI overlay subsystem manages popups, overlays, and widgets:

1. **`KeyPopupOverlay`**: Renders 3D tactile button popups for long-press alternate characters and popup menus, featuring vector SVG icon caps and gesture hysteresis tracking.
2. **`ClipboardHistoryOverlay`**: Renders a floating, scrollable clipboard history view displaying item indices, character lengths, deletion handles (`🗑`), clear-all, and echo-paste handlers.
3. **`VoiceInputOverlay`**: Manages voice recognition dialogs connected to speech engines via `SttEngineFactory`.
4. **`EmojiPickerOverlay`**: Grid overlay for category emoji selection.
5. **`JoystickPopupWidget`**: Floating arrow-key joystick overlay for precision navigation.
6. **`KeyPreviewOverlay`**: Magnified key pop-up preview bubble.
7. **`TrackpadView`**: Precision cursor navigation overlay.

---

## 6. Build-Time Asset Pipelines

Infinikey IME utilizes two automated Python build pipeline scripts executed via Gradle build tasks:

### A. Mechanical Switch Click Splitting Pipeline
- **Script**: `scripts/split_key_clicks.py`
- **Gradle Task**: `splitKeyClicks` (runs during `preBuild`)
- **Behavior**:
  1. Analyzes recorded mechanical switch audio samples from `app/src/main/assets/audio/`.
  2. Detects key-press (down transient) and key-release (up transient) using energy envelope detection and zero-crossing alignment.
  3. Outputs split audio pairs to `app/src/main/assets/audio_split/`.
  4. Checks file timestamps to avoid unnecessary re-processing during incremental builds.

### B. Dynamic Emoji Layout Generator
- **Script**: `scripts/generate_emoji_layouts.py`
- **Gradle Task**: `generateEmojiLayouts` (runs during `preBuild`)
- **Behavior**:
  1. Downloads Unicode emoji database dataset from `amio/emoji.json` (with graceful offline fallback to local layout files).
  2. Consolidates skin tone variants (Fitzpatrick scale modifiers `0x1F3FB`–`0x1F3FF`) into the base emoji's `alternates` array for long-press popups.
  3. Formats category layouts (`emoji.json`, `emoji_body.json`, `emoji_animals.json`, etc.) with standard navigation headers and footers.

---

## 7. Speech-to-Text (STT) Subsystem

The STT subsystem (`com.infinikey_ime.stt`) provides decoupled online and offline speech-to-text engines instantiated via `SttEngineFactory`:
- **`AndroidSystemSttEngine`**: Leverages Android `SpeechRecognizer` OS framework.
- **`SherpaOnnxSttEngine`**: Offline on-device speech recognition via Sherpa-ONNX C++ / JNI runtime models (`SttArchiveUnpacker`).
- **`WhisperSttEngine`**: Offline local Whisper model engine.
- **`CloudApiSttEngine`**: Online REST/WebSocket cloud API engine.

---

## 8. Persistence & Storage Access Framework

- **`SharedPreferences`**: Stores user settings, active theme selection, macro sequences (`pref_macro_<id>`), launcher slot assignments (`LauncherPreferencesManager`), and emoji history (`pref_recent_emojis`).
- **`InfinikeyDocumentsProvider`**: Implements Android Storage Access Framework (SAF) Documents Provider, allowing external text editors or file managers to access and edit custom layout and theme JSON files directly.
