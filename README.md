# Infinikey IME

An open-source, layout-driven, highly customizable soft keyboard for Android designed for power users, software developers, terminal environments (Termux, X11, VNC, RDP, SSH), and modern mobile typing.

![Infinikey IME Banner](app/src/main/assets/images/infinikey-ime.png)

---

## Key Highlights

- **Declarative Layout Engine**: JSON-driven keyboard layouts with row visibility toggling, Fn layer switching, staggered vs ortholinear key arrangements, and dynamic ratio-based geometry.
- **Powerful Macro System**: Bind complex macros, multi-touch gestures, swipe actions (`onSwipeUp`, `onSwipeDown`, `onSwipeLeft`, `onSwipeRight`), long-press popups, keycode auto-repeats, and app launchers directly to keys.
- **Decoupled Theme & Color Palette Engine**: 7 built-in presets (Slate Dark, Cyberpunk Neon, OLED True Black, Matrix Terminal, Retro Vintage, Muted Slate, System Auto Light/Dark) plus custom HSL/RGB palette generation and theme JSON override loading.
- **Multiple Form Factors & Split Mode**: Docked, Left-Docked, Right-Docked, Split Thumb-Cluster, and Floating Window modes with drag handles and persistent offset memory.
- **Clipboard History Overlay**: Persistent overlay saving up to 30 copied items with index badges, character lengths, individual item deletion (`🗑`), clear-all, and direct echo-paste connection to both standard text fields and raw terminal shells.
- **Vector SVG Icon Engine**: Crisp native canvas rendering for vector icons (`mic`, `tts`, `paperclip`, `clipboard`, `copy`, `cut`, `paste`, `select_all`) scaling cleanly across all screen densities.
- **Trackpad Cursor Navigation**: Independent spacebar trackpad and arrow key trackpad modes for fluid desktop-class mouse and cursor control.
- **Authentic Mechanical Switch Audio Engine**: Integrated Mechvibes switch packs (Cherry MX Blue, Brown, Red, Black, NovelKeys Cream, EG Oreo, EG Crystal Purple, Topre, IBM Model M Buckling Spring) and synthesized click audio with volume control.
- **Visual WYSIWYG Layout Editor**: Built-in 6-tab Settings Activity with real-time drag-and-drop key reordering, row properties, undo/redo state stack, and layout customization.

---

## Architecture & Feature Breakdown

### 1. Declarative Layout & Macro System

Infinikey IME uses a flexible JSON layout descriptor specification (`LAYOUT_DESCRIPTOR_SPEC.md`). Layouts are stored as standalone JSON files in `assets/layouts/` (e.g., `main.json`, `function.json`, `mobile.json`, `mobile_number.json`, `mobile_symbol.json`, `phone.json`).

#### Geometry & Dimensioning Rules
All key widths, heights, spacing gaps, font sizes, and corner radiuses use a strict dual-unit system:
- **Ratios (`Float`)**: Values like `1.0`, `1.5`, `0.02` represent ratios relative to the parent container (e.g. key width weight within a row, height ratio, or font size ratio).
- **Absolute DP (`Int`)**: Values like `4`, `8`, `16` represent fixed units in density-independent pixels.

#### Key Actions & Macro Triggers
Each key descriptor can define multiple touch event actions:
- **`onPress`**: Action executed on single tap.
- **`onLongPress`**: Action executed on touch and hold (with configurable timeout).
- **`onSwipeUp` / `onSwipeDown` / `onSwipeLeft` / `onSwipeRight`**: Directional swipe macros on individual keycaps.

#### Supported Macro Action Types
| Action `type` | Description |
| :--- | :--- |
| `"SEND_TEXT"` | Sends raw text strings or single characters directly to the input connection. |
| `"SEND_CODE"` | Sends specific Android `KeyEvent` keycodes (e.g. `67` for Backspace, `66` for Enter, `131` for F1). |
| `"SWITCH_LAYOUT"` | Swaps active layout layer dynamically (e.g. `"function"`, `"mobile"`, `"main"`). |
| `"SET_SCREEN_MODE"` | Changes screen docking form factor (`"FULL_WIDTH_DOCKED"`, `"SPLIT"`, `"LEFT_DOCKED"`, `"RIGHT_DOCKED"`, `"FLOATING"`). |
| `"ADJUST_HEIGHT"` | Dynamically resizes keyboard display height percentage (15% to 60%). |
| `"SHOW_POPUP"` | Displays modern 3D tactile character/action selection popup menus. |
| `"SHOW_WIDGET"` | Spawns interactive sub-widget overlays (`"JOYSTICK"`, `"EMOJI_PICKER"`, `"CLIPBOARD_HISTORY"`, `"VOICE_INPUT"`). |
| `"AUTO_REPEAT"` | Continuously auto-repeats keycode execution while key is held down. |
| `"TOGGLE_ROW"` | Dynamically shows/hides individual row IDs (e.g. Fn row) or toggles layer visibility (`"all_hidden"`). |
| `"TOGGLE_MODIFIER"` | Toggles modifier state (`SHIFT`, `CTRL`, `ALT`, `SUPER`, `META`). |
| `"SELECT_ALL"`, `"COPY"`, `"CUT"`, `"PASTE"` | Direct text editing and clipboard controls with fallback context support. |
| `"SWITCH_IME"` | Opens system Input Method Manager picker dialog. |
| `"LAUNCH_APP"` | Launches target Android application package directly from a key tap. |

---

### 2. Theme & Color Palette Engine

The color system is completely decoupled from layout descriptors (`THEMING_SPEC.md`). Themes can be loaded from preset asset files (`themes.json`, `themes/cyberpunk.json`, etc.) or generated on the fly via the built-in custom palette picker.

#### Preset Color Themes
1. **System Dynamic (`system_auto`)**: Follows Android OS Light Mode (`system_light`) and Dark Mode (`slate`).
2. **Slate Dark (`slate`)**: Default dark slate blue `#0F172A` theme with cyan and amber accents.
3. **Cyberpunk Neon (`cyberpunk`)**: High-contrast neon purple, yellow, magenta, and cyan palette.
4. **OLED True Black (`oled`)**: `#000000` pitch black background optimized for OLED display power saving.
5. **Matrix Terminal (`matrix`)**: Hacker green monochrome text on deep black.
6. **Retro Vintage (`retro`)**: Classic beige and taupe mechanical keyboard aesthetic.
7. **Muted Slate (`muted_slate`)**: Monochromatic low-saturation slate for distraction-free typing.
8. **Custom Palette (`custom`)**: User-configured theme defined via HSL/RGB palette generator.

#### Category Style Mapping & Style Inheritance
Keys inherit visual attributes from style classes (`styles`), which can be overridden per key:
- **`alphaKey`**: Standard letter and punctuation keys.
- **`numberKey`**: Number row and numeric keypad keys.
- **`modifierKey`**: `Shift`, `Ctrl`, `Alt`, `Super`, `Fn` modifier keys.
- **`functionKey`**: `F1` through `F12` function keys.
- **`actionKey`**: Primary action keys (`Enter`, `Backspace`, `Space`, `Tab`, `Escape`).
- **`navigationKey`**: Navigation cluster keys (`PageUp`, `PageDown`, `Home`, `End`, `Arrows`).
- **`editingKey`**: Clipboard and text editing keys (`SelectAll`, `Copy`, `Cut`, `Paste`).

---

### 3. Form Factors & Floating Window Geometry

Infinikey IME supports 5 screen docking modes:
- **Full Width Docked**: Standard full-width anchored keyboard.
- **Left Docked**: Comfortably aligned to the left side for single-handed use.
- **Right Docked**: Comfortably aligned to the right side for single-handed use.
- **Split Mode**: Divides keys into left and right thumb clusters for large screens and tablets.
- **Floating Window Mode**: Renders a floating window with top handle bar for drag repositioning and persistent offset memory.

---

### 4. Trackpad & Cursor Navigation

Infinikey IME includes dual trackpad cursor emulation modes:
- **Spacebar Trackpad**: Long-pressing or sliding along the spacebar (`␣`) transforms the key into a precision cursor trackpad.
- **Arrow Key Trackpad / Joystick**: Sliding over arrow keys activates analog trackpad navigation with visual cursor feedback.

---

### 5. Mechanical Switch Audio & Haptic Feedback

- **Sound Engine**: Powered by `SoundPool` with 8 authentic recorded switch sound packs (Cherry MX Blue, Brown, Red, Black, NovelKeys Cream, EG Oreo, EG Crystal Purple, Topre, IBM Model M Buckling Spring) and synthesized switch audio.
- **Build-Time Key Click Splitting Pipeline**: Automated Python pipeline (`scripts/split_key_clicks.py`) runs as part of the normal build process (`splitKeyClicks` Gradle task). It analyzes key press recordings, detects key-down (press) vs. key-up (release) transients using energy envelope and zero-crossing alignment, and outputs split sound sets to `app/src/main/assets/audio_split/`. Gradle automatically checks for missing or out-of-date split assets during `preBuild`.
- **Haptic Engine**: Supports System Haptics (`HapticFeedbackConstants`) and Android `Vibrator` with custom vibration styles (`SHARP_CLICK`, `CRISP_TICK`, `HEAVY_CLICK`, `DOUBLE_CLICK`, `CUSTOM_PULSE`), duration, and amplitude controls.

---

### 6. Dynamic Emoji Layout Generation

- **Build-Time Generation**: Python pipeline (`generate_emoji_layouts.py`) fetches Unicode emoji datasets, groups skin tones under base emojis in `alternates` arrays, and generates category asset layouts.
- **Runtime Recents Tracker**: Logs recently used emojis to `SharedPreferences` (up to 24) and dynamically generates the `"emoji_recents"` layout when tapping `😀`.


---

## Project Structure

- **`app/src/main/java/com/programmerkeyboard/`**:
  - `ProgrammerInputMethodService.kt`: Core `InputMethodService` managing keyboard state, target terminal detection, clipboard listening, layout switching, and action dispatching.
- **`app/src/main/java/com/programmerkeyboard/view/`**:
  - `KeyboardView.kt`: High-performance custom canvas View rendering key rows, SVG vector icon paths, touch gestures, trackpad modes, and haptics.
  - `KeyPopupOverlay.kt`: 3D tactile action popups with SVG icon caps and dismissal tracking.
  - `ClipboardHistoryOverlay.kt`: Floating scrollable clipboard history view with single-item deletion and quick paste.
  - `EmojiPickerOverlay.kt`: Grid emoji picker overlay window.
  - `VoiceInputOverlay.kt`: Floating voice recognition dialog.
  - `JoystickPopupWidget.kt`: Floating arrow trackpad widget.
- **`app/src/main/java/com/programmerkeyboard/model/`**:
  - `KeyDefinition.kt`: Strongly-typed `KeyAction`, `KeyDefinition`, `KeyStyle`, and `DimensionValue` data models.
  - `LayoutDefinition.kt`: Schema models for layout metadata, rows, keys, and themes.
- **`app/src/main/java/com/programmerkeyboard/engine/`**:
  - `LayoutParser.kt`: JSON layout engine parsing layout descriptors and applying theme overrides.
  - `KeyRepeatEngine.kt`: Handles long-press timeouts and key auto-repeats.
- **`app/src/main/java/com/programmerkeyboard/settings/`**:
  - `SettingsActivity.kt`: 6-tab preference and configuration activity.
  - `InteractiveLayoutEditorView.kt`: WYSIWYG canvas for real-time drag-and-drop key layout editing.

---

## Building the Project

Assemble debug or release APKs using Gradle:

```bash
sh gradlew assembleDebug
sh gradlew assembleRelease
```

Generated APK output locations:
- **Debug**: `app/build/outputs/apk/debug/infinikey-ime-v0.1.30-b114-debug.apk`
- **Release**: `app/build/outputs/apk/release/infinikey-ime-v0.1.30-b114-release.apk`

---

## License

This project is open-source under the [MIT License](LICENSE).

## Attributions & Credits

Mechanical keyboard switch audio samples are sourced from **[Mechvibes](https://mechvibes.com/)**. See [ATTRIBUTION.md](ATTRIBUTION.md) for full credits.
