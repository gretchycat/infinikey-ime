# Infinikey IME Architecture Specification

## Overview
Infinikey IME is a high-performance, layout-driven, customizable Android Input Method Editor (IME). It provides desktop-grade programming layouts, dedicated function keys, clipboard history management, vector SVG icon rendering, interactive form factor switching, mechanical switch audio feedback, custom theme engines, and an integrated real-time WYSIWYG layout editor.

---

## System Architecture

```
                               +-------------------------------------+
                               |   ProgrammerInputMethodService      |
                               |    (Android IME Lifecycle Engine)   |
                               +----+---------------+----------------+
                                    |               |
                                    v               v
                   +----------------+--+         +--+-------------------+
                   |   KeyboardView    |         | ClipboardHistory     |
                   | (Render Surface)  |         |      Overlay         |
                   +--------+----------+         +----------------------+
                            |
           +----------------+----------------+
           |                |                |
           v                v                v
   +-------+------+  +------+-------+  +-----+--------+
   | KeyPopup     |  | LayoutParser |  | SoundPool &  |
   | Overlay      |  | (JSON Engine)|  | Haptic Engine|
   +--------------+  +--------------+  +--------------+
```

### 1. `ProgrammerInputMethodService` (`com.infinikey_ime`)
* **Role**: Primary entry point implementing Android `InputMethodService`.
* **Responsibilities**:
  - Manages `InputConnection` dispatches (`commitText`, `sendDownUpKeyEvents`, `performEditorAction`).
  - Target application detection via `isTerminalTarget()` to differentiate between raw terminal shells (`TYPE_NULL`) and text fields.
  - Monitors system clipboard changes with `ClipboardManager.OnPrimaryClipChangedListener` and persists up to 30 clipboard entries in `SharedPreferences`.
  - Manages active modifier states (`SHIFT`, `CTRL`, `ALT`, `SUPER`, `META`).
  - Handles screen mode transitions (`FULL_WIDTH_DOCKED`, `SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, `FLOATING`).
  - Processes macro actions (`SEND_TEXT`, `SEND_CODE`, `SWITCH_LAYOUT`, `LAUNCH_APP`, `TOGGLE_ROW`, `TOGGLE_MODIFIER`).

### 2. `KeyboardView` (`com.infinikey_ime.view`)
* **Role**: Custom high-FPS canvas surface for dynamic key layout rendering and touch interaction.
* **Responsibilities**:
  - Renders staggered and ortholinear key rows, keycaps, primary/secondary labels, and native SVG vector icon paths (`drawSvgCopyIcon`, `drawSvgCutIcon`, `drawSvgPasteIcon`, `drawSvgSelectAllIcon`, `drawSvgPaperclipIcon`, `drawSvgClipboardIcon`, `drawSvgMicIcon`, `drawSvgTtsIcon`).
  - Multi-touch gesture processing (swipe-up for secondary symbols, directional key swipes, long-press popups, trackpad gestures).
  - Integrates `SoundPool` for asset-based mechanical switch audio feedback and `Vibrator` for haptics.

### 3. `KeyPopupOverlay` & `ClipboardHistoryOverlay` (`com.infinikey_ime.view`)
* **Role**: Floating overlay windows for action menus and history management.
* **Responsibilities**:
  - **`KeyPopupOverlay`**: Renders 3D tactile button caps with SVG vector icons and relaxed gesture tracking (28dp movement threshold, 40% hysteresis, direct tap-to-select support, and active dismissal callbacks).
  - **`ClipboardHistoryOverlay`**: Renders a floating scrollable history view displaying index numbers, character lengths, individual item deletion (`🗑`), long-press removal, clear-all, and direct echo paste.

### 4. `LayoutParser` (`com.infinikey_ime.engine`)
* **Role**: Declarative JSON layout parser and theme engine.
* **Responsibilities**:
  - Parses JSON layout descriptors (`main.json`, `function.json`, `mobile.json`, `mobile_number.json`, `mobile_symbol.json`, `phone.json`, `emoji*.json`).
  - Merges styles, row offsets, split keys, and preset color themes (`themes.json`, `themes/*.json`).
  - Implements System Dynamic Day/Night theme resolution and custom HSL/RGB user palette generator.

### 5. `InteractiveLayoutEditorView` & `SettingsActivity` (`com.infinikey_ime.settings`)
* **Role**: WYSIWYG layout editor and configuration manager.
* **Responsibilities**:
  - Provides a 6-tab configuration UI (Geometry, Behavior, Haptics, Audio, Themes, Layout Editor).
  - Features real-time drag-and-drop key reordering and undo/redo history stack (`ArrayDeque<LayoutDefinition>`).
  - Allows editing key labels, weights, styles, action types, and parameters.

---

## Layout & Macro Engine Architecture

```
+-------------------+        +---------------------+        +--------------------+
|  JSON Descriptor  |  --->  |    LayoutParser     |  --->  |   KeyDefinition    |
| (main/mobile.json)|        | (Resolves Ratios &  |        | (Strongly Typed    |
+-------------------+        |  Theme Overrides)   |        |  Action Models)    |
                             +---------------------+        +---------+----------+
                                                                      |
                                                                      v
                                                            +---------+----------+
                                                            |  KeyAction Dispatch|
                                                            | (SEND_TEXT, CODE,  |
                                                            |  SWITCH_LAYOUT,    |
                                                            |  LAUNCH_APP, etc.) |
                                                            +--------------------+
```

### Layout Engine Capabilities
1. **Dynamic Ratio Calculation**: Translates `Float` weight ratios and `Int` absolute DP values into responsive pixel geometry based on screen dimensions and form factor mode.
2. **Row Visibility Engine**: Controls dynamic visibility per row ID (`TOGGLE_ROW`). AllowsFn row layers or optional symbol rows to be toggled on demand without reloading the whole layout.
3. **Layer Switching**: Enables instant layer transitions (`SWITCH_LAYOUT`) between QWERTY, Function layer, Numeric, Symbolic, and Emoji layouts.

---

## Emoji Layout Generation Pipeline

```
+---------------------------------------+
|  amio/emoji.json (GitHub Database)    |
+-------------------+-------------------+
                    | (urllib fetch)
                    v
+-------------------+-------------------+
|     generate_emoji_layouts.py         |
| (Groups by category, merges Flags,    |
|  consolidates skin tones to alternates|
+-------------------+-------------------+
                    | (JSON generation)
                    v
+-------------------+-------------------+
|  app/src/main/assets/layouts/emoji*   |
+---------------------------------------+
```

### 1. Build-Time Generator (`generate_emoji_layouts.py`)
Registered as a Gradle pre-build task (`generateEmojiLayouts`), this script handles layout creation:
* **Online Fetch**: Downloads the official Unicode emoji dataset from `amio/emoji.json`.
* **Offline Resilience**: If the internet connection is unavailable, it gracefully checks for existing local layout files to prevent compilation failures.
* **Skin Tone Consolidation**: Detects Fitzpatrick scale modifiers (`0x1F3FB`–`0x1F3FF`) and groups variants under their base emoji. Variants are defined in the key's `"alternates"` array, displaying on long-press.
* **Layout Generation**: Splits emojis by categories (mapping `Flags` to `Symbols`), chunks them into rows of 8, and writes separate asset layouts (`emoji.json`, `emoji_body.json`, etc.) with category headers and footers.

### 2. Runtime Recents Tracker (`emoji_recents`)
* Whenever an emoji is selected, `ProgrammerInputMethodService` logs it to the user's `SharedPreferences` history (capped at 24).
* Tapping the main emoji key `😀` targets the dynamic `"emoji_recents"` layout. `LayoutParser` generates the keyboard on the fly from the history list, displaying your most frequently used emojis.
* Long-pressing `😀` bypasses recents and opens the main complete Smileys layout directly.
