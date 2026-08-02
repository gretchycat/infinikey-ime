# WYSIWYG Layout Editor Specification

## Overview
The WYSIWYG Layout Editor allows users to visually build, edit, reorder, and save custom keyboard layout configurations directly inside the app.

---

## Editor Features

1. **Interactive Canvas View (`InteractiveLayoutEditorView`)**:
   - Renders keys in real time matching active layout ratios and styles.
   - Touch drag-and-drop key reordering (horizontally within a row or vertically across rows).
   - Semi-transparent ghost preview during touch drag.

2. **Row & Key Controls**:
   - **➕ Add Key**: Button on the right edge of each row to append new keys (default weight: `1.0`).
   - **➕ Add New Key Row**: Button at the bottom of the canvas to append new rows.

3. **Key Properties Popup Modal**:
   - Primary Label (`EditText`)
   - Secondary / Swipe-Up Label (`EditText`)
   - Key Category Style (`Spinner`: `alphaKey`, `numberKey`, `modifierKey`, `functionKey`, `actionKey`, `navigationKey`, `editingKey`)
   - Width Weight (`EditText`: Float, default `1.0`)
   - Action Type (`Spinner`: `SEND_TEXT`, `SEND_CODE`, `TOGGLE_MODIFIER`, `SWITCH_LAYOUT`, `SHOW_WIDGET`, `CLIPBOARD`)
   - Action Parameter (`EditText`)
   - 🗑️ Delete Key (`Button`)

4. **Undo / Redo History Stack Engine**:
   - Full state snapshotting using JSON serialization (`ArrayDeque<LayoutDefinition>`).
   - ↩️ Undo and ↪️ Redo buttons with dynamic enabled states.
   - 💾 Save button to persist custom layout descriptors (`pref_custom_layout_json`).
