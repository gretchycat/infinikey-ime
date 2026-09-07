# Layout Descriptor Format Specification

The **Infinikey IME** uses a declarative JSON layout descriptor format to define standalone keyboard layouts, physical rows, key actions, visual styling, multi-touch gestures, and dynamic geometry. Each layout is stored in its own JSON file (e.g. `main.json`, `function.json`, `emoji.json`).

---

## 1. Dimensioning Value Rule

All numeric dimensioning parameters (such as key `weight`/`width`, `height`, spacing gaps `horizontalSpacing`/`verticalSpacing`, font sizes, and corner radiuses) follow a strict type convention:

* **Floating Point Numbers** (e.g., `1.0`, `1.5`, `2.5`, `0.02`, `0.15`): Represent **ratios** relative to the parent container's total size (e.g. relative key width proportion within a row, ratio of container height, or font size ratio relative to key height).
* **Integers** (e.g., `4`, `12`, `14`, `48`): Represent **fixed absolute units in pixels/points (dp / sp)**.

---

## 2. Complete Root Schema

```json
{
  "id": "main",
  "name": "Main Base Layout",
  "version": "2.0",
  "author": "Infinikey IME Team",
  "description": "Primary 5-row desktop-style layout with key style classes and gesture definitions.",
  "metadata": {
    "horizontalSpacing": 4,
    "verticalSpacing": 4,
    "defaultScreenMode": "FULL_WIDTH_DOCKED",
    "defaultHeightPercentage": 30
  },
  "theme": {
    "backgroundColor": "#0F172A",
    "fontFamily": "Monospace"
  },
  "styles": {
    "alphaKey": {
      "bgColor": "#1E2230",
      "fgColor": "#F8FAFC",
      "pressedBgColor": "#334155",
      "cornerRadius": 8
    },
    "numberKey": {
      "bgColor": "#1E293B",
      "fgColor": "#38BDF8",
      "pressedBgColor": "#334155",
      "cornerRadius": 8
    },
    "modifierKey": {
      "bgColor": "#334155",
      "fgColor": "#38BDF8",
      "activeBgColor": "#0284C7",
      "activeFgColor": "#FFFFFF",
      "cornerRadius": 8
    },
    "functionKey": {
      "bgColor": "#0F172A",
      "fgColor": "#F59E0B",
      "pressedBgColor": "#1E293B",
      "cornerRadius": 6
    },
    "actionKey": {
      "bgColor": "#0284C7",
      "fgColor": "#FFFFFF",
      "pressedBgColor": "#0369A1",
      "cornerRadius": 8
    }
  },
  "gestures": {
    "onTwoFingerSwipeLeft": { "type": "SET_SCREEN_MODE", "mode": "DOCK_LEFT" },
    "onTwoFingerSwipeRight": { "type": "SET_SCREEN_MODE", "mode": "DOCK_RIGHT" },
    "onTwoFingerSwipeUp": { "type": "SET_SCREEN_MODE", "mode": "FLOAT" },
    "onTwoFingerSwipeDown": { "type": "SET_SCREEN_MODE", "mode": "FULL_WIDTH_DOCKED" },
    "onTwoFingerPinchOut": { "type": "SET_SCREEN_MODE", "mode": "SPLIT" }
  },
  "rows": [
    {
      "id": 1,
      "hidden": true,
      "keys": [ ... ]
    },
    {
      "id": 2,
      "keys": [ ... ]
    }
  ]
}
```

---

## 2b. Layout Metadata Parameters (`metadata`)

The `metadata` block specifies keyboard constraints, dimensions, scaling, and scrolling features:

* **`horizontalSpacing`**: Gap space between adjacent keys in a row (`int` for dp, `float` for ratio).
* **`verticalSpacing`**: Gap space between adjacent rows (`int` for dp, `float` for ratio).
* **`defaultScreenMode`** (`string`): Initial screen dock mode (`"FULL_WIDTH_DOCKED"`, `"LEFT_DOCKED"`, `"RIGHT_DOCKED"`, `"FLOATING"`, `"SPLIT"`).
* **`defaultHeightPercentage`** (`int`): Default vertical height percentage of the screen space (from `15` to `60`).
* **`longPressTimeoutMs`** (`long`, optional): Timeout in milliseconds before a touch-and-hold triggers long press action (default `350`).
* **`autoRepeatIntervalMs`** (`long`, optional): Auto-repeat trigger interval in milliseconds for held keycodes (default `50`).
* **`splitClusterRatio`** (`float`, optional): Center gap ratio proportion when rendered in split mode.
* **`showKeyPreview`** (`boolean`): Whether to display pop-up key magnification bubbles on tap.
* **`maxFontSize`**: Maximum font size ceiling for text labels (`int` for dp/sp, `float` for ratio).
* **`scrollDirection`** (`string`, optional): Set to `"VERTICAL"` or `"HORIZONTAL"` to enable scrolling of rows (e.g. for emojis).
* **`maxVisibleRows`** (`int`, optional): When vertical scrolling is enabled, specifies how many middle rows are rendered concurrently within the scrolling viewport between the top pinned row (row index 0) and the bottom pinned row (last row).
* **`maxVisibleColumns`** (`int`, optional): Specifies max columns when horizontal scrolling is enabled.
* **`accessoryLayout`** / **`deadspaceLayout`** (`string`, optional): Target ID of the layout rendered in the accessory space during side-docked or split modes (`"navigation"`, `"mobile_number"`, `"function"`, `"macro"`, `"mobile_symbol"`, `"none"`).
* **`accessoryText`** / **`deadspaceText`** (`string`, optional): Custom label or multi-line text rendered centered in the accessory region. Supports newlines (`\n`).
* **`accessoryTextColor`** / **`deadspaceTextColor`** (`hex string`, optional): Color of the accessory text string (default `"#94A3B8"`).
* **`accessoryTextSize`** (`DimensionValue`, optional): Font size for accessory text (`int` for fixed dp/sp, `float` for container height ratio).

---

## 3. Key Style Classes (`styles`)

The root `styles` dictionary defines named style classes. Each key object can specify a `"style": "styleName"` property to inherit visual attributes, while still retaining the ability to provide custom property overrides.

### Style Class Definition (`StyleObject`)
Each style object can define any combination of visual properties:

* **`bgColor`** (`hex string`): Default idle fill color.
* **`pressedBgColor`** (`hex string`): Fill color when touched/pressed.
* **`activeBgColor`** (`hex string`): Fill color when key/modifier is active or locked.
* **`fgColor`** (`hex string`): Main text label and icon color.
* **`secondaryFgColor`** (`hex string`): Secondary badge label color.
* **`activeFgColor`** (`hex string`): Text color when key is active/locked.
* **`borderColor`** (`hex string`): Outline border stroke color.
* **`borderWidth`**: Border thickness (`int` for dp, `float` for ratio).
* **`cornerRadius`**: Corner rounding radius (`int` for dp, `float` for ratio).
* **`fontSize`**: Primary label font size (`int` for sp/pt, `float` for ratio).
* **`secondaryFontSize`**: Secondary badge font size (`int` for sp/pt, `float` for ratio).
* **`backgroundImage`** (`string`, optional): Asset texture image path.

---

## 4. Key Object Properties

### Style & Layout
* **`style`** (`string`, optional): Name of the key style class to inherit from (e.g. `"modifierKey"`, `"numberKey"`).
* **`label`** (`string`, required): Main text label rendered on key face.
* **`secondaryLabel`** (`string`, optional): Small secondary text badge (e.g. top-right corner character).
* **`topLeftLabel`** (`string`, optional): Small top-left corner badge label.
* **`topRightLabel`** (`string`, optional): Small top-right corner badge label.
* **`weight`** / **`width`**: Key width multiplier (`float` ratio relative to row weight sum, or `int` fixed dp).
* **`height`**: Custom key height multiplier (`float` ratio relative to default row height, or `int` fixed dp).
* **`startOffset`**: Horizontal starting offset preceding the key (`float` ratio or `int` dp).
* **`isSplitKey`** (`boolean`, optional): Identifies if the key spans across the split line in split screen mode.
* **`splitLeftWeight`** / **`splitRightWeight`**: Width weights for left and right portions of a split key.
* **`flexible`** (`boolean`, optional): Allows key to dynamically stretch to fill remaining row space (e.g. Spacebar).
* **`spacer`** (`boolean`, optional): Renders key as an invisible structural spacing gap without keycap backgrounds or touch hit-testing.
* **`label` / `secondaryLabel` on Spacers**: When specified on a spacer key (`"spacer": true` or `"style": "spacer"`), renders custom text centered inside the spacing gap. Supports multi-line strings (`\n`), custom text color (`fgColor`, default `#64748B`), and font sizing (`fontSize`).
* **`showPreview`** / **`showKeyPreview`** (`boolean`, optional): Overrides popup preview magnification bubble for this specific key.
* **`alternates`** / **`alternateKeys`** (`array<string>`, optional): List of alternate characters/symbols displayed in long-press popup menus.
* **`icon`** (`string`, optional): Vector SVG or drawable icon resource identifier (e.g. `"mic"`, `"paperclip"`, `"clipboard"`, `"copy"`, `"cut"`, `"paste"`, `"select_all"`, `"keyboard"`).
* **`backgroundImage`** (`string`, optional): Custom key texture image path.

### Visual Overrides Per Key
A key can override any visual attribute inherited from its `style` class:
* `fgColor`, `secondaryFgColor`, `bgColor`, `pressedBgColor`, `activeBgColor`, `borderColor`, `borderWidth`, `cornerRadius`, `fontSize`, `maxFontSize`, `secondaryFontSize`.

### Event Action Handlers
* **`onPress`** (`Action object`, optional): Action performed on single tap.
* **`onLongPress`** (`Action object`, optional): Action performed on touch and hold.
* **`onSwipeUp`** (`Action object`, optional): Action performed on upward swipe.
* **`onSwipeDown`** (`Action object`, optional): Action performed on downward swipe.
* **`onSwipeLeft`** (`Action object`, optional): Action performed on leftward swipe.
* **`onSwipeRight`** (`Action object`, optional): Action performed on rightward swipe.

---

## 5. Action Schema (`Action`)

Actions are strongly-typed JSON objects with a `type` field:

| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SEND_TEXT"` | `"text"` (`string`) | Sends raw string macro or single character to the input connection. |
| `"SEND_CODE"` | `"code"` (`int`) | Sends Android `KeyEvent` keycode (e.g. `67` for Backspace, `66` for Enter). |
| `"MACRO"` | `"id"` (`string`) | Replays or records macro step sequence (e.g. `"M1"`). Tap to replay/stop, long-press to record. |
| `"SWITCH_LAYOUT"` | `"target"` (`string`) | Swaps active layout file to target layout ID (e.g. `"function"`, `"mobile"`, `"main"`). |
| `"SET_SCREEN_MODE"` | `"mode"` (`string`) | Geometry Action: Changes layout docking mode (`"FULL_WIDTH_DOCKED"`, `"SPLIT"`, `"LEFT_DOCKED"`, `"RIGHT_DOCKED"`, `"FLOATING"`). |
| `"ADJUST_HEIGHT"` | `"delta"` (`int`) \| `"percentage"` (`int`) | Geometry Action: Dynamically increases/decreases keyboard display height percentage (15% to 60%). |
| `"SHOW_POPUP"` | `"options"` (`array<string>`) | Displays 3D tactile character/action popup selection menu. |
| `"SHOW_WIDGET"` | `"widget"` (`string`) | Displays sub-widget overlay (`"JOYSTICK"`, `"EMOJI_PICKER"`, `"CLIPBOARD_HISTORY"`, `"VOICE_INPUT"`). |
| `"AUTO_REPEAT"` | `"code"` (`int`), `"intervalMs"` (`int`) | Auto-repeats keycode action continuously while key is held. |
| `"TOGGLE_ROW"` | `"rowId"` (`int` \| `string`) | Toggles visibility state of row(s) dynamically (or `"all_hidden"` for layer toggle). |
| `"TOGGLE_MODIFIER"` | `"modifier"` (`string`) | Toggles modifier state (`"SHIFT"`, `"CTRL"`, `"ALT"`, `"SUPER"`, `"META"`). |
| `"LOCK_MODIFIER"` | `"modifier"` (`string`) | Locks modifier state (`"SHIFT"`, `"CTRL"`, `"ALT"`, `"SUPER"`, `"META"`). Automatically set for long-press on modifier keys. |
| `"SELECT_ALL"` | None | Selects all text in target input control (`performContextMenuAction` with key fallback). |
| `"COPY"` | None | Copies current selection to system clipboard. |
| `"CUT"` | None | Cuts current selection to system clipboard. |
| `"PASTE"` | None | Directly reads primary clip from ClipboardManager and commits text to input connection. |
| `"PASTE_ECHO"` | None | Echo-pastes primary clip to text input or raw terminal stream. |
| `"SWITCH_IME"` | None | Opens Android system Input Method picker dialog. |
| `"LAUNCH_APP"` | `"packageName"` (`string`) | Launches specified application package directly from keyboard key tap. |
| `"NONE"` | None | No operation. |

---

## 6. Complete Baseline Layout Example (`main.json`)

```json
{
  "id": "main",
  "name": "Main Base Layout",
  "version": "2.0",
  "author": "Infinikey IME Team",
  "description": "5-row QWERTY base layout with key style classes, hidden Fn row, and gesture actions",
  "metadata": {
    "horizontalSpacing": 4,
    "verticalSpacing": 4,
    "defaultScreenMode": "FULL_WIDTH_DOCKED",
    "defaultHeightPercentage": 30
  },
  "theme": {
    "backgroundColor": "#0F172A",
    "fontFamily": "Monospace"
  },
  "styles": {
    "alphaKey": {
      "bgColor": "#1E2230",
      "fgColor": "#F8FAFC",
      "pressedBgColor": "#334155",
      "cornerRadius": 8
    },
    "numberKey": {
      "bgColor": "#1E293B",
      "fgColor": "#38BDF8",
      "pressedBgColor": "#334155",
      "cornerRadius": 8
    },
    "modifierKey": {
      "bgColor": "#334155",
      "fgColor": "#38BDF8",
      "activeBgColor": "#0284C7",
      "activeFgColor": "#FFFFFF",
      "cornerRadius": 8
    },
    "functionKey": {
      "bgColor": "#0F172A",
      "fgColor": "#F59E0B",
      "pressedBgColor": "#1E293B",
      "cornerRadius": 6
    },
    "actionKey": {
      "bgColor": "#0284C7",
      "fgColor": "#FFFFFF",
      "pressedBgColor": "#0369A1",
      "cornerRadius": 8
    }
  },
  "gestures": {
    "onTwoFingerSwipeLeft": { "type": "SET_SCREEN_MODE", "mode": "DOCK_LEFT" },
    "onTwoFingerSwipeRight": { "type": "SET_SCREEN_MODE", "mode": "DOCK_RIGHT" },
    "onTwoFingerSwipeUp": { "type": "SET_SCREEN_MODE", "mode": "FLOAT" },
    "onTwoFingerSwipeDown": { "type": "SET_SCREEN_MODE", "mode": "FULL_WIDTH_DOCKED" },
    "onTwoFingerPinchOut": { "type": "SET_SCREEN_MODE", "mode": "SPLIT" }
  },
  "rows": [
    {
      "id": 1,
      "hidden": true,
      "keys": [
        {
          "label": "F1",
          "style": "functionKey",
          "onPress": { "type": "SEND_CODE", "code": 131 }
        },
        {
          "label": "F2",
          "style": "functionKey",
          "onPress": { "type": "SEND_CODE", "code": 132 }
        }
      ]
    },
    {
      "id": 2,
      "keys": [
        {
          "label": "1",
          "secondaryLabel": "!",
          "style": "numberKey"
        },
        {
          "label": "a",
          "secondaryLabel": "á",
          "style": "alphaKey",
          "onPress": { "type": "SEND_TEXT", "text": "a" },
          "onLongPress": { "type": "SHOW_POPUP", "options": ["á", "à", "ä", "â", "å"] },
          "onSwipeUp": { "type": "SEND_TEXT", "text": "A" }
        },
        {
          "label": "Shift",
          "style": "modifierKey",
          "weight": 1.4,
          "onPress": { "type": "TOGGLE_MODIFIER", "modifier": "SHIFT" }
        },
        {
          "label": "Backspace",
          "style": "actionKey",
          "weight": 1.5,
          "icon": "ic_backspace",
          "onPress": { "type": "SEND_CODE", "code": 67 },
          "onLongPress": { "type": "AUTO_REPEAT", "code": 67, "intervalMs": 50 }
        },
        {
          "label": "Fn",
          "style": "modifierKey",
          "weight": 1.2,
          "fgColor": "#F59E0B",
          "onPress": { "type": "SWITCH_LAYOUT", "target": "function" },
          "onLongPress": { "type": "TOGGLE_ROW", "rowId": 1 }
        }
      ]
    }
  ]
}
```

---

## 7. Accessory Layout System & Accessory Text

When Infinikey IME operates in docked form factors (`SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, `SIDE_DOCKED`), the primary key layout occupies only part of the screen width. The remaining screen region is designated as the **Accessory Area**.

### Configurable Metadata Parameters

```json
"metadata": {
  "accessoryLayout": "navigation",
  "accessoryText": "INFINIKEY IME\nSplit Mode",
  "accessoryTextColor": "#94A3B8",
  "accessoryTextSize": 12
}
```

* **`accessoryLayout`**: Specifies a secondary layout file ID (e.g., `"navigation"`, `"mobile_number"`, `"function"`, `"macro"`, `"mobile_symbol"`, or `"none"`). When active, this secondary keyboard renders inside the open accessory space.
* **`accessoryText`**: Custom string displayed inside the accessory card container. Multi-line strings can be specified using `\n`.
* **`accessoryTextColor`**: Color formatting for the accessory text label (hex string).
* **`accessoryTextSize`**: Font size dimension for the accessory text (`int` dp/sp or `float` relative ratio).
* **Settings Override**: Users can globally override layout-defined accessory layouts via **Settings -> Keyboard Layout -> Accessory Layout**.

---

## 8. Macro Key System (`MACRO` Action)

Macro keys allow users to record multi-step keystroke sequences on the fly and replay them with a single tap.

### Macro Action JSON Schema

```json
{
  "label": "M1",
  "style": "macroKey",
  "onPress": { "type": "MACRO", "id": "M1" },
  "onLongPress": { "type": "MACRO", "id": "M1" }
}
```

### Recording & Replay Logic
1. **Replay (Tap)**: Single-tapping a macro key with an existing recorded sequence executes all saved keystrokes in order.
2. **Record Mode (Long-Press)**: Long-pressing a macro key puts the keyboard into recording mode for that specific macro `id` (`M1` through `M10`). Keystrokes typed while recording are captured.
3. **Stop & Save (Tap while recording)**: Tapping the recording macro key again stops recording, saves the sequence to `SharedPreferences` (`pref_macro_<id>`), and displays a toast summary (e.g. `💾 Macro M1 saved (8 steps)`).
4. **Baseline Layout (`macro.json`)**: Built-in 2x5 grid layout featuring `M1` through `M10` macro keys, designed to be used either as a standalone layout layer or embedded as an **accessory layout**.

---

## 9. Placing Text in Key Spacing (Spacer Text) & Accessory Areas

Infinikey IME allows rendering text directly inside structural spacing gaps between keys as well as within the accessory area.

### A. Spacer Key Text (Spacing Between Keys)
Key objects marked as spacers (`"spacer": true` or `"style": "spacer"`) omit keycap background drawing and touch registration. If a `label` or `secondaryLabel` is provided on a spacer key, text is rendered centered within the open gap space.

```json
{
  "spacer": true,
  "label": "NAV PAD\nCluster",
  "fgColor": "#64748B",
  "fontSize": 11,
  "weight": 1.0
}
```

* **Multi-Line Text**: Supports newline breaks (`\n`) for section headers or cluster labels.
* **Color & Font Size**: Customize label color via `fgColor` (default `#64748B`) and size via `fontSize`.

### B. Accessory Area Text
Text specified in `metadata.accessoryText` is drawn centered inside the background card of the accessory space when form factor is set to `SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, or `SIDE_DOCKED`.

```json
"metadata": {
  "accessoryText": "CUSTOM WORKSPACE\nMode Active",
  "accessoryTextColor": "#38BDF8",
  "accessoryTextSize": 14
}
```
