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
  - Displays App Version (`v0.0.2`) and Build Number in Tab 1 (Geometry).
  - Real-time drag-and-drop key reordering and undo/redo history stack (`ArrayDeque<LayoutDefinition>`).
