# Color Themes & Preset Palette Specification

## 1. Theme Architecture & Philosophy

Infinikey IME supports a completely decoupled, per-preset color theme architecture. 

```
Layout Descriptors
  └── Define rows, key arrangement, and style references ("style": "alphaKey")

Theme Descriptors & Preset Palettes
  └── Define color tokens, keycap fills, text colors, and visual overrides

Key-Level Overrides
  └── Explicit per-key colors (fgColor, bgColor) that supersede style class defaults
```

Because themes are decoupled from layouts, any visual theme can be applied to any layout configuration without modifying key definitions or action logic.

Themes can be loaded from:
1. Built-in preset asset files (`assets/themes.json` and `assets/themes/*.json`).
2. User-edited theme files stored in user space (`Android/data/com.infinikey_ime/files/themes/*.json`).
3. Custom HSL/RGB palette generator controls in `SettingsActivity`.

---

## 2. Supported Built-In Theme Presets

Infinikey IME includes 9 built-in theme presets:

| Index | Identifier | Display Name | Theme Description |
| :---: | :--- | :--- | :--- |
| `0` | `system_auto` | System Dynamic | Automatically follows OS Light Mode (`system_light`) and Dark Mode (`system_dark`). |
| `1` | `system_light` | System Light | Clean light mode theme with light slate background and crisp keycaps. |
| `2` | `system_dark` | System Dark | Deep dark mode theme for low-light environments. |
| `3` | `slate` | Slate Dark (Default) | Deep slate blue background `#0F172A` with bright cyan `#38BDF8` and amber `#F59E0B` accents. |
| `4` | `cyberpunk` | Cyberpunk Neon | High-contrast neon purple background `#12092B` with neon yellow `#FACC15`, magenta `#EC4899`, and cyan `#00F0FF` keycaps. |
| `5` | `oled` | OLED True Black | `#000000` pitch black background optimized for OLED display energy efficiency. |
| `6` | `matrix` | Matrix Terminal | Hacker green `#10B981` terminal theme on deep black `#030712` background. |
| `7` | `retro` | Retro Vintage | Warm tan keycaps `#B8AD9C`/`#ADA291` on taupe beige `#6E6454` background. |
| `8` | `muted_slate` | Low Saturation Slate | Low-contrast monochromatic slate palette for minimal distraction. |
| `9` | `custom` | Custom Theme | Reads user-configured JSON color palettes from storage (`pref_custom_theme_json`). |

---

## 3. Category Style Tokens

Key descriptors reference named category style classes (`styles`). Each key category maps to specific visual color tokens:

- **`alphaKey`**: Standard letter and punctuation keys.
- **`numberKey`**: Number row and numeric keypad keys.
- **`modifierKey`**: `Shift`, `Ctrl`, `Alt`, `Super`, `Fn` modifier keys.
- **`functionKey`**: `F1` through `F12` function keys.
- **`actionKey`**: Primary action keys (`Enter`, `Backspace`, `Space`, `Tab`, `Escape`).
- **`navigationKey`** / **`arrowKey`**: Navigation cluster keys (`PageUp`, `PageDown`, `Home`, `End`, `Arrows`).
- **`editingKey`**: Clipboard and text editing keys (`SelectAll`, `Copy`, `Cut`, `Paste`).
- **`macroKey`**: Macro pad keycaps (`M1`–`M10`).
- **`launcherKey`**: Application launcher keycaps (`slot_0`–`slot_24`).

---

## 4. Theme Specification Schema

```json
{
  "version": 1,
  "userEdited": false,
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
  }
}
```

### Style Properties
- **`bgColor`** (`hex string`): Default background color for keycaps in this category.
- **`pressedBgColor`** (`hex string`): Fill color when touched or pressed.
- **`activeBgColor`** (`hex string`): Fill color when key/modifier is active or locked.
- **`fgColor`** (`hex string`): Primary text label and icon color.
- **`secondaryFgColor`** (`hex string`): Secondary badge label color.
- **`activeFgColor`** (`hex string`): Text color when key is active/locked.
- **`borderColor`** (`hex string`): Outline border stroke color.
- **`borderWidth`**: Border thickness (`int` dp, `float` ratio).
- **`cornerRadius`**: Corner rounding radius (`int` dp, `float` ratio).
- **`fontSize`**: Primary label font size (`int` sp/pt, `float` ratio).
- **`secondaryFontSize`**: Secondary badge font size (`int` sp/pt, `float` ratio).

---

## 5. Persistence & Versioning (`ThemeManager`)

Themes are managed by `ThemeManager`:
- Default preset theme JSON files are synchronized to external user storage upon first launch.
- If a user customizes a theme preset, `ThemeManager` sets `"userEdited": true`.
- App updates never overwrite user-edited themes (`userEdited == true`).
- If an unedited theme preset (`userEdited == false`) receives an update in a newer app release, `ThemeManager` safely upgrades the file.
