# Infinikey IME Architecture Specification

## Overview
Infinikey IME is a high-performance, layout-driven, customizable Android Input Method Editor (IME). It provides desktop-grade programming layouts, dedicated function keys, clipboard history management, vector SVG icon rendering, interactive form factor switching, mechanical switch audio feedback, and an integrated real-time WYSIWYG layout editor.

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

### 1. `ProgrammerInputMethodService` (`com.programmerkeyboard`)
* **Role**: Primary entry point implementing Android `InputMethodService`.
* **Responsibilities**:
  - Manages `InputConnection` dispatches (`commitText`, `sendDownUpKeyEvents`, `performEditorAction`).
  - Target application detection via `isTerminalTarget()` to differentiate between raw terminal shells (`TYPE_NULL`) and text fields.
  - Monitors system clipboard changes with `ClipboardManager.OnPrimaryClipChangedListener` and persists up to 30 clipboard entries in `SharedPreferences`.
  - Manages active modifier states (`SHIFT`, `CTRL`, `ALT`, `SUPER`, `META`).
  - Handles screen mode transitions (`FULL_WIDTH_DOCKED`, `SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, `FLOATING`).

### 2. `KeyboardView` (`com.programmerkeyboard.view`)
* **Role**: Custom high-FPS canvas surface for dynamic key layout rendering and touch interaction.
* **Responsibilities**:
  - Renders staggered and rectangular key rows, keycaps, primary/secondary labels, and native SVG vector icon paths (`drawSvgCopyIcon`, `drawSvgCutIcon`, `drawSvgPasteIcon`, `drawSvgSelectAllIcon`, `drawSvgPaperclipIcon`, `drawSvgClipboardIcon`, `drawSvgMicIcon`, `drawSvgTtsIcon`).
  - Multi-touch gesture processing (swipe-up for secondary symbols, swipe-down, long-press popups, trackpad gestures).
  - Integrates `SoundPool` for asset-based mechanical switch audio feedback and `Vibrator` for haptics.

### 3. `KeyPopupOverlay` & `ClipboardHistoryOverlay` (`com.programmerkeyboard.view`)
* **Role**: Floating overlay windows for action menus and history management.
* **Responsibilities**:
  - **`KeyPopupOverlay`**: Renders 3D tactile button caps with SVG vector icons and relaxed gesture tracking (28dp movement threshold, 40% hysteresis, and direct tap-to-select support).
  - **`ClipboardHistoryOverlay`**: Renders a floating scrollable history view displaying index numbers, character lengths, individual item deletion (`🗑`), long-press removal, clear-all, and quick paste.

### 4. `LayoutParser` (`com.programmerkeyboard.engine`)
* **Role**: Declarative JSON layout parser and theme engine.
* **Responsibilities**:
  - Parses JSON layout descriptors (`main.json`, `function.json`, `mobile.json`, `mobile_number.json`, `mobile_symbol.json`).
  - Merges styles, row offsets, split keys, and preset color themes (`themes.json`).
  - Implements System Dynamic Day/Night theme resolution.

### 5. `InteractiveLayoutEditorView` & `SettingsActivity` (`com.programmerkeyboard.settings`)
* **Role**: WYSIWYG layout editor and configuration manager.
* **Responsibilities**:
    - Provides a 6-tab configuration UI (Geometry, Behavior, Haptics, Audio, Themes, Layout Editor).
    - Displays App Version (`v0.1.0`) and Build Number in Tab 1 (Geometry).
    - Features a package-aware default layout setting in Tab 2 (Behavior) using spinner preference `pref_default_unseen_layout` (defaulting to the mobile keyboard layout) to serve unseen apps.
    - Real-time drag-and-drop key reordering and undo/redo history stack (`ArrayDeque<LayoutDefinition>`).

---

## Emoji Layout Generation Pipeline

To ensure a comprehensive and modern emoji selection without bloating the code or restricting emoji ranges, Infinikey IME utilizes an automated, layout-driven generation pipeline during compilation.

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
* **Layout Generation**: Splits emojis by categories (mapping `Flags` to `Symbols`), chunks them into rows of 8, and writes separate asset layouts (`emoji.json`, `emoji_body.json`, etc.) with category headers andABC footers.

### 2. Runtime Recents Tracker (`emoji_recents`)
* Whenever an emoji is selected, [`ProgrammerInputMethodService`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/app/src/main/java/com/programmerkeyboard/ProgrammerInputMethodService.kt) logs it to the user's `SharedPreferences` history (capped at 24).
* Tapping the main emoji key `😀` targets the dynamic `"emoji_recents"` layout. [`LayoutParser`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/app/src/main/java/com/programmerkeyboard/engine/LayoutParser.kt) generates the keyboard on the fly from the history list, displaying your most frequently used emojis.
* Long-pressing `😀` bypasses recents and opens the main complete Smileys layout directly.
