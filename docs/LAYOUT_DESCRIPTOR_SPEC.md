# Layout Descriptor Format Specification

Infinikey IME uses a declarative JSON layout descriptor format to define standalone keyboard layouts, key rows, key actions, visual styles, multi-touch surface gestures, and dynamic screen geometry. Each layout is stored in its own JSON file (e.g. `main.json`, `function.json`, `mobile.json`).

---

## 1. Architectural Philosophy

The Infinikey layout engine enforces a strict separation of concerns across three system layers:

```
Layout
  ├── Geometry (rows, key weights, spacing)
  ├── Keys & Labels
  ├── Action Definitions
  └── Touch & Surface Gestures

Theme
  └── Visual Styling (color palettes, borders, corner radiuses)

Runtime
  └── Display State (docking form factor, screen mode, modifiers, active widgets)
```

- **Layouts** describe *what* exists and *how* it behaves.
- **Themes** describe *how* it looks visually.
- **Runtime** governs *where* and *how* it is displayed on screen.

Because layout structure and visual themes are decoupled, any layout descriptor can be rendered with any theme preset or custom color palette without altering key definitions or action handlers.

---

## 2. Dimensioning Value Rule

All numeric dimensioning parameters (such as key `weight`/`width`, `height`, gap spacing `horizontalSpacing`/`verticalSpacing`, font sizes, and corner radiuses) follow a strict type convention:

- **Floating Point Numbers** (e.g., `1.0`, `1.5`, `2.5`, `0.02`, `0.15`): Represent **ratios** relative to the parent container's size (e.g., relative key width proportion within a row, ratio of container height, or font size ratio relative to key height).
- **Integers** (e.g., `4`, `8`, `12`, `14`, `48`): Represent **fixed absolute units in density-independent pixels / scale-independent pixels (dp / sp)**.

---

## 3. Complete Root Layout Schema

The root layout descriptor is a declarative JSON object defining top-level identity attributes, global metadata rules, theme styling overrides, category style classes, surface gestures, and row/key geometry definitions.

### A. Comprehensive Root Layout Example

```json
{
  "id": "main",
  "name": "Main Base Layout",
  "version": "2.0",
  "author": "Infinikey IME Team",
  "description": "Primary 5-row desktop-style layout with key style classes, surface gestures, and accessory region defaults.",
  "isGenerated": false,
  "metadata": {
    "horizontalSpacing": 4,
    "verticalSpacing": 4,
    "defaultScreenMode": "FULL_WIDTH_DOCKED",
    "defaultHeightPercentage": 30,
    "longPressTimeoutMs": 350,
    "autoRepeatIntervalMs": 50,
    "splitClusterRatio": 0.2,
    "showKeyPreview": true,
    "maxFontSize": 18,
    "scrollDirection": "NONE",
    "maxVisibleRows": 5,
    "maxVisibleColumns": 14,
    "accessoryLayout": "macro",
    "accessoryText": "INFINIKEY IME\nSplit Mode",
    "accessoryTextColor": "#94A3B8",
    "accessoryTextSize": 12,
    "accessoryImage": "images/logo.png",
    "showPartial": false
  },
  "theme": {
    "backgroundColor": "#0F172A",
    "fontFamily": "Monospace",
    "modifierOffDotColor": "#475569",
    "modifierLatchedDotColor": "#38BDF8",
    "modifierLockedDotColor": "#F59E0B"
  },
  "styles": {
    "alphaKey": {
      "bgColor": "#1E2230",
      "fgColor": "#F8FAFC",
      "pressedBgColor": "#334155",
      "activeBgColor": "#1E2230",
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
    },
    "navigationKey": {
      "bgColor": "#1E293B",
      "fgColor": "#38BDF8",
      "pressedBgColor": "#334155",
      "cornerRadius": 8
    },
    "editingKey": {
      "bgColor": "#0F172A",
      "fgColor": "#10B981",
      "pressedBgColor": "#1E293B",
      "cornerRadius": 8
    }
  },
  "gestures": {
    "onTwoFingerSwipeLeft": { "type": "SET_SCREEN_MODE", "mode": "LEFT_DOCKED" },
    "onTwoFingerSwipeRight": { "type": "SET_SCREEN_MODE", "mode": "RIGHT_DOCKED" },
    "onTwoFingerSwipeUp": { "type": "SET_SCREEN_MODE", "mode": "FLOATING" },
    "onTwoFingerSwipeDown": { "type": "SET_SCREEN_MODE", "mode": "FULL_WIDTH_DOCKED" },
    "onTwoFingerPinchOut": { "type": "SET_SCREEN_MODE", "mode": "SPLIT" },
    "onTwoFingerPinchIn": { "type": "SET_SCREEN_MODE", "mode": "FULL_WIDTH_DOCKED" }
  },
  "rows": [
    {
      "id": 1,
      "hidden": true,
      "splitIndex": 2,
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
      "hidden": false,
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
          "label": "Space",
          "style": "alphaKey",
          "weight": 3.0,
          "flexible": true,
          "isSplitKey": true,
          "onPress": { "type": "SEND_TEXT", "text": " " }
        },
        {
          "label": "Backspace",
          "style": "actionKey",
          "weight": 1.5,
          "icon": "ic_backspace",
          "onPress": { "type": "SEND_CODE", "code": 67 },
          "onLongPress": { "type": "AUTO_REPEAT", "code": 67, "intervalMs": 50 }
        }
      ]
    }
  ]
}
```

### B. Root-Level Property Reference

| Property | Type | Requirement | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`id`** | `string` | **Required** | `"custom_layout"` | Unique identifier string used for layout layer switching and persistence (e.g. `"main"`, `"mobile"`, `"function"`, `"navigation"`). |
| **`name`** | `string` | **Required** | `"Custom Layout"` | Human-readable title displayed in layout selection menus, meta picker UI, and settings. |
| **`version`** | `string` | Optional | `"1.0"` | Schema version string used for engine compatibility checks and automatic user upgrades. |
| **`author`** | `string` | Optional | `"Unknown"` | Creator name, organization, or maintainer attribution string. |
| **`description`** | `string` | Optional | `""` | Detailed description explaining the key arrangement, target use-case, or feature set. |
| **`isGenerated`** | `boolean` | Optional | `false` | Indicates whether the layout is dynamically generated by the engine at runtime (e.g. emoji pickers, meta layer). Supported aliases: `is_generated`, `generated`. |
| **`metadata`** | `object` | Optional | `{}` | Key gap dimensions, docking mode defaults, scroll rules, touch timeouts, and accessory region configuration (see Section 4). |
| **`theme`** | `object` | Optional | `{}` | Global layout theme overrides including background color, font family, and modifier state indicator colors. |
| **`styles`** | `object` | Optional | `{}` | Dictionary of category style class definitions (`alphaKey`, `numberKey`, `modifierKey`, `functionKey`, `actionKey`, `navigationKey`, `editingKey`, etc.) (see Section 5). |
| **`gestures`** | `object` | Optional | `{}` | Map of surface-wide multi-touch gesture handlers (`onTwoFingerSwipeLeft`, `onTwoFingerPinchOut`, etc.) mapped to key action objects. |
| **`rows`** | `array<KeyRow>` | **Required** | `[]` | Ordered list of horizontal key row objects defining key layout geometry, split positions, and key descriptors (see Section 6). |


---

## 4. Metadata Parameters (`metadata`)

The `metadata` block specifies layout constraints, gap spacing, initial screen mode, and accessory area defaults:

* **`horizontalSpacing`**: Gap space between adjacent keys in a row (`int` for dp, `float` for ratio).
* **`verticalSpacing`**: Gap space between adjacent rows (`int` for dp, `float` for ratio).
* **`defaultScreenMode`** (`string`): Initial screen dock mode (`"FULL_WIDTH_DOCKED"`, `"LEFT_DOCKED"`, `"RIGHT_DOCKED"`, `"FLOATING"`, `"SPLIT"`). Aliases supported: `"SIDE_DOCKED"` / `"DOCK_LEFT"`, `"DOCK_RIGHT"`, `"FLOAT"`.
* **`defaultHeightPercentage`** (`int`): Default vertical height percentage of the screen space (`15` to `60`).
* **`longPressTimeoutMs`** (`long`, optional): Timeout in milliseconds before a touch-and-hold triggers long press action (default `350`).
* **`autoRepeatIntervalMs`** (`long`, optional): Auto-repeat trigger interval in milliseconds for held keycodes (default `50`).
* **`splitClusterRatio`** (`float`, optional): Center gap ratio proportion when rendered in split mode.
* **`showKeyPreview`** (`boolean`): Whether to display pop-up key magnification bubbles on tap.
* **`maxFontSize`**: Maximum font size ceiling for text labels (`int` for dp/sp, `float` for ratio).
* **`scrollDirection`** (`string`, optional): Set to `"VERTICAL"` or `"HORIZONTAL"` to enable row scrolling (e.g. for emojis).
* **`maxVisibleRows`** (`int`, optional): Specifies maximum concurrent visible rows when vertical scrolling is enabled.
* **`accessoryLayout`** (`string`, optional): Secondary layout ID rendered inside the open accessory region during docked or split modes (`"navigation"`, `"mobile_number"`, `"function"`, `"macro"`, `"media"`, `"launcher"`, `"mobile_symbol"`, `"none"`). *Legacy alias: `deadspaceLayout`.*
* **`accessoryText`** (`string`, optional): Custom label or multi-line text rendered centered in the accessory region. Supports newlines (`\n`). *Legacy alias: `deadspaceText`.*
* **`accessoryTextColor`** (`hex string`, optional): Text color for the accessory text string.
* **`accessoryTextSize`**: Font size for accessory text (`int` dp/sp, `float` ratio).
* **`accessoryImage`** (`string`, optional): Path to an asset image, file URI, icon identifier, or graphic file rendered inside the accessory region. *Legacy alias: `deadspaceImage`.*

---

## 5. Key Style Classes (`styles`)

The root `styles` dictionary defines named category style classes. Keys reference a style class via `"style": "styleName"` to inherit visual attributes.

### Category Style Attributes (`StyleObject`)
* **`bgColor`** (`hex string`): Default idle keycap background color.
* **`pressedBgColor`** (`hex string`): Fill color when touched/pressed.
* **`activeBgColor`** (`hex string`): Fill color when key or modifier is active/locked.
* **`fgColor`** (`hex string`): Primary text label and icon color.
* **`secondaryFgColor`** (`hex string`): Secondary badge label color.
* **`activeFgColor`** (`hex string`): Text color when key is active/locked.
* **`borderColor`** (`hex string`): Keycap border stroke color.
* **`borderWidth`**: Border thickness (`int` dp, `float` ratio).
* **`cornerRadius`**: Corner rounding radius (`int` dp, `float` ratio).
* **`fontSize`**: Primary label font size (`int` sp/pt, `float` ratio).
* **`secondaryFontSize`**: Secondary badge font size (`int` sp/pt, `float` ratio).
* **`backgroundImage`** (`string`, optional): Custom asset texture image path.

---

## 6. Key Object Properties

### Structure & Layout
* **`style`** (`string`, optional): Style class to inherit from (e.g. `"alphaKey"`, `"modifierKey"`, `"actionKey"`).
* **`label`** (`string`, required): Main text label rendered on key cap.
* **`secondaryLabel`** (`string`, optional): Small secondary badge text (e.g., top-right corner character).
* **`topLeftLabel`** (`string`, optional): Small top-left corner badge label.
* **`topRightLabel`** (`string`, optional): Small top-right corner badge label.
* **`weight`**: Key width weight multiplier (`float` ratio relative to row weight sum, or `int` fixed dp). *Alias: `width`.*
* **`height`**: Custom key height multiplier (`float` ratio relative to row height, or `int` fixed dp).
* **`startOffset`**: Horizontal offset preceding the key (`float` ratio or `int` dp).
* **`isSplitKey`** (`boolean`, optional): Identifies if the key spans across the split line in split screen mode.
* **`splitLeftWeight`** / **`splitRightWeight`**: Width weights for left and right portions of a split key.
* **`flexible`** (`boolean`, optional): Allows key to dynamically stretch to fill available row space (e.g., Spacebar).
* **`spacer`** (`boolean`, optional): Renders key as an invisible structural spacing gap without keycap backgrounds or touch hit-testing.
* **`label` / `secondaryLabel` on Spacers**: When specified on a spacer key (`"spacer": true` or `"style": "spacer"`), renders custom text centered inside the spacing gap. Supports multi-line strings (`\n`), custom text color (`fgColor`), and font sizing (`fontSize`).
* **`showPreview`** / **`showKeyPreview`** (`boolean`, optional): Overrides popup preview magnification bubble for this specific key.
* **`alternates`** (`array<string>`, optional): List of alternate characters/symbols displayed in long-press popup menus. *Legacy alias: `alternateKeys`.*
* **`icon`** (`string`, optional): Vector SVG or icon identifier (e.g. `"mic"`, `"paperclip"`, `"clipboard"`, `"copy"`, `"cut"`, `"paste"`, `"select_all"`, `"keyboard"`).
* **`backgroundImage`** (`string`, optional): Custom key texture image path.

### Event Handlers
* **`onPress`** (`Action object`, optional): Action performed on single tap.
* **`onLongPress`** (`Action object`, optional): Action performed on touch and hold.
* **`onSwipeUp`** / **`onSwipeDown`** / **`onSwipeLeft`** / **`onSwipeRight`** (`Action object`, optional): Directional swipe actions.

---

## 7. Categorized Action System

Actions are declared as strongly-typed JSON objects with a `type` discriminator.

### A. Input Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SEND_TEXT"` | `"text"` (`string`) | Sends raw string text or single character directly to the input connection. |
| `"SEND_CODE"` | `"code"` (`int`) | Sends Android `KeyEvent` keycode (e.g. `67` Backspace, `66` Enter, `131` F1). |
| `"AUTO_REPEAT"` | `"code"` (`int`), `"intervalMs"` (`int`) | Auto-repeats keycode action continuously while key is held down. |

### B. State Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SWITCH_LAYOUT"` | `"target"` (`string`) | Swaps active layout layer to target layout ID (e.g. `"function"`, `"mobile"`, `"main"`). |
| `"TOGGLE_MODIFIER"`| `"modifier"` (`string`) | Toggles modifier state (`"SHIFT"`, `"CTRL"`, `"ALT"`, `"SUPER"`, `"META"`). |
| `"LOCK_MODIFIER"`  | `"modifier"` (`string`) | Locks modifier state (`"SHIFT"`, `"CTRL"`, `"ALT"`, `"SUPER"`, `"META"`). |
| `"TOGGLE_ROW"`     | `"rowId"` (`int` \| `string`)| Dynamically shows or hides individual row IDs (or `"all_hidden"` for layer toggle). |

### C. Macro Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"MACRO"` | `"id"` (`string`) | Replays or records macro step sequence (e.g. `"M1"` through `"M10"`). Tap to replay/stop, long-press to record. |

### D. UI Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SHOW_POPUP"` | `"options"` (`array<string>`) | Displays 3D tactile character/action popup selection menu. |
| `"SHOW_WIDGET"`| `"widget"` (`string`) | Spawns interactive overlay widget (`"JOYSTICK"`, `"EMOJI_PICKER"`, `"CLIPBOARD_HISTORY"`, `"VOICE_INPUT"`). |

### E. Geometry Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SET_SCREEN_MODE"`| `"mode"` (`string`) | Changes screen docking form factor (`"FULL_WIDTH_DOCKED"`, `"SPLIT"`, `"LEFT_DOCKED"`, `"RIGHT_DOCKED"`, `"FLOATING"`). |
| `"ADJUST_HEIGHT"`  | `"delta"` (`int`) \| `"percentage"` (`int`) | Dynamically resizes keyboard height percentage (15% to 60%). |

### F. Editing Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SELECT_ALL"` | None | Selects all text in target input field. |
| `"COPY"` | None | Copies current selection to system clipboard. |
| `"CUT"` | None | Cuts current selection to system clipboard. |
| `"PASTE"` | None | Direct paste from system clipboard. |
| `"PASTE_ECHO"` | None | Echo-pastes primary clip to text input or raw terminal stream. |

### G. System Actions
| Action `type` | Parameters | Description |
| :--- | :--- | :--- |
| `"SWITCH_IME"` | None | Opens Android system Input Method picker dialog. |
| `"LAUNCH_APP"`, `"LAUNCH"` | `"packageName"` (`string`), `"slotId"` (`string`) | Launches target app package. Supports slot persistence (`slotId`), rendering app icon on keycaps, and Application Selector popup on long-press. |
| `"NONE"` | None | No operation. |

---

## 8. Accessory Area Architecture

When Infinikey IME operates in docked form factors (`SPLIT`, `LEFT_DOCKED`, `RIGHT_DOCKED`, `SIDE_DOCKED`), the keyboard uses unused screen width as an **Accessory Area**.

```
┌──────────────────────────────────────────────┐
│                                              │
│                Main Keyboard                 │
│                                              │
├──────────────────────────────┬───────────────┤
│                              │ Navigation /  │
│                              │  Media /      │
│                              │  Macro Pad    │
└──────────────────────────────┴───────────────┘
```

### Configurable Metadata Parameters

```json
"metadata": {
  "accessoryLayout": "navigation",
  "accessoryImage": "images/logo.png",
  "accessoryText": "INFINIKEY IME\nSplit Mode",
  "accessoryTextColor": "#94A3B8",
  "accessoryTextSize": 12
}
```

- **`accessoryLayout`**: Specifies a secondary layout ID (e.g. `"navigation"`, `"mobile_number"`, `"function"`, `"macro"`, `"media"`, `"launcher"`, `"mobile_symbol"`, `"none"`).
- **`accessoryImage`**: Path to an asset image, file URI, icon identifier, or graphic file.
- **`accessoryText`**: Custom multi-line text displayed inside the accessory container (`\n` supported).
- **`accessoryTextColor`**: Color formatting for accessory text string (hex string).
- **`accessoryTextSize`**: Font size dimension (`int` dp/sp or `float` relative ratio).

---

## 9. Baseline Layout Example (`main.json`)

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
    "defaultHeightPercentage": 30,
    "accessoryLayout": "macro"
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
    "onTwoFingerSwipeLeft": { "type": "SET_SCREEN_MODE", "mode": "LEFT_DOCKED" },
    "onTwoFingerSwipeRight": { "type": "SET_SCREEN_MODE", "mode": "RIGHT_DOCKED" },
    "onTwoFingerSwipeUp": { "type": "SET_SCREEN_MODE", "mode": "FLOATING" },
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
