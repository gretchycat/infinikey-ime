# Programmer Keyboard Architecture Specification

## Overview
Programmer Keyboard is a high-performance, layout-driven, customizable Android Input Method Editor (IME). It provides a full desktop-class programming layout with function keys, navigation keys, custom alternate keys, interactive themes, mechanical switch audio feedback, and an integrated real-time WYSIWYG layout editor.

---

## System Architecture

```
                               +-------------------------------------+
                               |   ProgrammerInputMethodService      |
                               |    (Android IME Lifecycle Engine)   |
                               +------------------+------------------+
                                                  |
                                                  v
                               +------------------+------------------+
                               |            KeyboardView             |
                               |  (Custom Dynamic Render Surface)    |
                               +--------+-------------------+--------+
                                        |                   |
                                        v                   v
                     +------------------+----+         +----+-------------------+
                     |     LayoutParser      |         |  Sound & Haptic Engine |
                     | (JSON Descriptor)     |         |  (SoundPool & Assets)  |
                     +-----------------------+         +------------------------+
```

### 1. `ProgrammerInputMethodService` (`com.programmerkeyboard`)
* **Role**: Primary entry point implementing `InputMethodService`.
* **Responsibilities**:
  - Handles key code dispatches to `InputConnection` (`sendText`, `sendDownUpKeyEvents`, `performEditorAction`).
  - Manages active modifier state (`SHIFT`, `CTRL`, `ALT`, `SUPER`, `META`).
  - Executes special key actions (layout switching, clipboard operations, IME switching, voice input).

### 2. `KeyboardView` (`com.programmerkeyboard.view`)
* **Role**: Custom high-fps canvas surface for dynamic key layout rendering and touch handling.
* **Responsibilities**:
  - Renders rows, keycaps, primary/secondary labels, latched/locked modifier dots, and active visual themes.
  - Multi-touch gesture processing (swipe-up for alternate symbols, swipe-down, long-press popups).
  - Integrates `SoundPool` for asset-based audio feedback and `Vibrator` for haptics.

### 3. `LayoutParser` (`com.programmerkeyboard.engine`)
* **Role**: Declarative JSON layout parser and theme engine.
* **Responsibilities**:
  - Parses JSON layout descriptors (`main.json`, `function.json`, `mobile.json`, `meta.json`).
  - Merges themes, style overrides, and preset color themes (`themes.json`).
  - Implements System Dynamic Day/Night theme resolution.

### 4. `InteractiveLayoutEditorView` & `SettingsActivity` (`com.programmerkeyboard.settings`)
* **Role**: WYSIWYG layout editor and configuration manager.
* **Responsibilities**:
  - Provides a 5-tab configuration UI (WYSIWYG Editor, Geometry, Behavior, Audio/Haptics, Themes).
  - Interactive touch drag-and-drop key reordering.
  - Real-time undo/redo history stack (`ArrayDeque<LayoutDefinition>`).
  - Modal key property editor dialogs.
