# WYSIWYG Layout Editor Specification

The **WYSIWYG Layout Editor** allows users to visually design, reorder, edit, and save custom keyboard layout configurations directly inside the Infinikey IME application settings.

---

## 1. Editor Subsystem Architecture

The layout editor consists of two primary components:
- **`SettingsActivity`** (`com.infinikey_ime.settings`): Host configuration activity providing navigation tabs, control toolbars, and layout saving/export actions.
- **`InteractiveLayoutEditorView`** (`com.infinikey_ime.view`): Interactive touch canvas rendering keys in real time and managing drag-and-drop state.

---

## 2. Interactive Canvas Features

1. **Real-Time Key Rendering**:
   - Renders keycaps matching active layout dimension weights (`weight`) and visual style tokens.
   - Touch drag-and-drop key reordering (horizontally within a row or vertically across adjacent rows).
   - Displays semi-transparent ghost key preview during active touch drag.

2. **Row & Key Structure Controls**:
   - **➕ Add Key**: Button on the right edge of each row to append new keycaps (default weight: `1.0`).
   - **➕ Add New Key Row**: Button at the bottom of the canvas surface to append new empty rows.

3. **Key Properties Modal Dialog**:
   - **Primary Label** (`EditText`): Specifies text string rendered on key face (doubles as **Spacer Text** when configured as a spacer key).
   - **Secondary / Swipe-Up Label** (`EditText`): Specifies secondary character badge or swipe-up action payload.
   - **Key Category Style** (`Spinner`): Selects category style (`alphaKey`, `numberKey`, `modifierKey`, `functionKey`, `actionKey`, `navigationKey`, `editingKey`, `macroKey`, `launcherKey`).
   - **Spacer Toggle** (`CheckBox`): Toggles key as an invisible structural spacing gap while enabling centered spacer text rendering.
   - **Width Weight** (`EditText`: Float, default `1.0`): Key width weight multiplier.
   - **Action Type** (`Spinner`): Selects action type (`SEND_TEXT`, `SEND_CODE`, `MACRO`, `TOGGLE_MODIFIER`, `SWITCH_LAYOUT`, `SHOW_WIDGET`, `LAUNCH_APP`, `SET_SCREEN_MODE`).
   - **Action Parameter** (`EditText`): Specifies action payload parameter (e.g. macro ID `M1`, keycode integer `67`, package name, or target layout ID).
   - **🗑️ Delete Key** (`Button`): Removes keycap from the active row.

4. **Accessory Region Controls**:
   - **Accessory Layout Target** (`Spinner`): Selects default accessory layout (`navigation`, `mobile_number`, `function`, `macro`, `media`, `launcher`, `mobile_symbol`, `none`).
   - **Accessory Area Text & Image** (`EditText`): Edits custom text string and asset image path rendered inside the accessory panel during split or docked modes.

5. **Undo / Redo History Stack Engine**:
   - Full layout state snapshotting using JSON serialization (`ArrayDeque<LayoutDefinition>`).
   - **↩️ Undo** and **↪️ Redo** buttons with dynamic enabled states.
   - **💾 Save** button to persist custom layout descriptors to user storage (`pref_custom_layout_json_<id>`).
