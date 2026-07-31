# Layout Descriptor Format Specification

The **Programmer Keyboard** uses a declarative JSON layout descriptor format to define standalone keyboard layouts, physical rows, key actions, visual styling, multi-touch gestures, and dynamic geometry. Each layout is stored in its own JSON file (e.g. `main.json`, `function.json`, `emoji.json`).

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
  "author": "Programmer Keyboard Team",
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
* **`weight`** / **`width`**: Key width multiplier (`float` ratio relative to row weight sum, or `int` fixed dp).
* **`height`**: Custom key height multiplier (`float` ratio relative to default row height, or `int` fixed dp).
* **`icon`** (`string`, optional): Drawable icon resource identifier.
* **`backgroundImage`** (`string`, optional): Custom key texture image path.

### Visual Overrides Per Key
A key can override any visual attribute inherited from its `style` class:
* `fgColor`, `secondaryFgColor`, `bgColor`, `pressedBgColor`, `activeBgColor`, `borderColor`, `borderWidth`, `cornerRadius`, `fontSize`, `secondaryFontSize`.

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
| `"SEND_TEXT"` | `"text"` (`string`) | Sends raw text to the input connection. |
| `"SEND_CODE"` | `"code"` (`int`) | Sends Android `KeyEvent` keycode. |
| `"SWITCH_LAYOUT"` | `"target"` (`string`) | Swaps active layout file to target layout ID (e.g., `"function"`). |
| `"SET_SCREEN_MODE"` | `"mode"` (`string`) | Geometry Action: Changes layout docking mode (`"FULL_WIDTH_DOCKED"`, `"SPLIT"`, `"DOCK_LEFT"`, `"DOCK_RIGHT"`, `"FLOAT"`). |
| `"ADJUST_HEIGHT"` | `"delta"` (`int`) \| `"percentage"` (`int`) | Geometry Action: Dynamically increases/decreases keyboard display height percentage. |
| `"SHOW_POPUP"` | `"options"` (`array<string>`) | Displays character selection popup menu. |
| `"SHOW_WIDGET"` | `"widget"` (`string`) | Displays sub-widget overlay (`"JOYSTICK"`, `"SETTINGS"`, `"EMOJI_PICKER"`, `"VOICE_INPUT"`). |
| `"AUTO_REPEAT"` | `"code"` (`int`), `"intervalMs"` (`int`) | Auto-repeats keycode action continuously while held. |
| `"TOGGLE_ROW"` | `"rowId"` (`int` \| `string`) | Toggles visibility state of row(s). |
| `"TOGGLE_MODIFIER"` | `"modifier"` (`string`) | Toggles modifier state (`"SHIFT"`, `"CTRL"`, `"ALT"`, `"SUPER"`). |
| `"NONE"` | None | No operation. |

---

## 6. Complete Baseline Layout Example (`main.json`)

```json
{
  "id": "main",
  "name": "Main Base Layout",
  "version": "2.0",
  "author": "Programmer Keyboard Team",
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
