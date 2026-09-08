# WYSIWYG Layout Editor Specification

## Overview
The WYSIWYG Layout Editor allows users to visually build, edit, reorder, and save custom keyboard layout configurations directly inside the app.

---

## Editor Features

1. **Interactive Canvas View (`com.infinikey_ime.view.InteractiveLayoutEditorView`)**:
   - Renders keys in real time matching active layout ratios and styles.
   - Touch drag-and-drop key reordering (horizontally within a row or vertically across rows).
   - Semi-transparent ghost preview during touch drag.

2. **Row & Key Controls**:
   - **➕ Add Key**: Button on the right edge of each row to append new keys (default weight: `1.0`).
   - **➕ Add New Key Row**: Button at the bottom of the canvas to append new rows.

3. **Key Properties Popup Modal**:
   - Primary Label (`EditText`) — doubles as **Spacer Text** when key is set as a spacer.
   - Secondary / Swipe-Up Label (`EditText`)
   - Key Category Style (`Spinner`: `alphaKey`, `numberKey`, `modifierKey`, `functionKey`, `actionKey`, `navigationKey`, `editingKey`, `macroKey`)
   - Spacer Toggle (`CheckBox`): Marks key as an invisible structural spacer while allowing centered spacer text rendering.
   - Width Weight (`EditText`: Float, default `1.0`)
   - Action Type (`Spinner`: `SEND_TEXT`, `SEND_CODE`, `MACRO`, `TOGGLE_MODIFIER`, `SWITCH_LAYOUT`, `SHOW_WIDGET`, `CLIPBOARD`)
   - Action Parameter (`EditText`): Specifies macro ID (e.g. `M1`), character string, or keycode.
   - 🗑️ Delete Key (`Button`)

4. **Accessory Layout, Accessory Text & Image Controls**:
   - **Accessory Space Layout Dropdown**: (`Spinner`): Selects default accessory layout (`navigation`, `mobile_number`, `function`, `macro`, `media`, `mobile_symbol`, `none`).
   - **Accessory Area Text & Image Inputs**: (`EditText`): Edits custom text string and asset/file image path rendered in the center/side accessory card during split or docked modes. Renders image above text with dynamic proportional height scaling.

5. **Undo / Redo History Stack Engine**:
   - Full state snapshotting using JSON serialization (`ArrayDeque<LayoutDefinition>`).
   - ↩️ Undo and ↪️ Redo buttons with dynamic enabled states.
   - 💾 Save button to persist custom layout descriptors (`pref_custom_layout_json`).
