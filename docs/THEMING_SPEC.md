# Color Themes & Preset Palette Specification

## Theme Architecture
Infinikey IME supports a decoupled, per-preset color theme architecture. Layout descriptors (`main.json`, `mobile.json`) define key row arrangements and style references (`"style": "alphaKey"`), while themes define color palettes and visual styling.

Themes can be loaded from:
1. Preset theme files (`assets/themes.json` and `assets/themes/*.json`).
2. Custom user JSON configurations stored in `SharedPreferences` (`pref_custom_theme_json`).
3. Custom HSL/RGB palette generator controls in `SettingsActivity`.

---

## Supported Built-In Theme Presets

| Index | Identifier | Display Name | Theme Description |
|-------|------------|--------------|-------------------|
| `0` | `system_auto` | System Dynamic | Follows device OS Light Mode (`system_light`) and Dark Mode (`slate`). |
| `1` | `slate` | Slate Dark (Default) | Deep slate blue background `#0F172A` with bright cyan `#38BDF8` and amber `#F59E0B` accents. |
| `2` | `cyberpunk` | Cyberpunk Neon | High-contrast neon purple background `#12092B` with neon yellow `#FACC15`, magenta `#EC4899`, and cyan `#00F0FF` keycaps. |
| `3` | `oled` | OLED True Black | `#000000` pitch black background for OLED display energy efficiency. |
| `4` | `matrix` | Matrix Terminal | Hacker green `#10B981` terminal theme on deep black `#030712` background. |
| `5` | `retro` | Retro Vintage (Classic Beige) | Warm tan keycaps `#B8AD9C`/`#ADA291` on taupe beige `#6E6454` background. |
| `6` | `muted_slate` | Low Saturation Slate | Low-contrast monochromatic slate palette for minimal distraction. |
| `7` | `custom` | Custom Theme | Reads `pref_custom_theme_json` for user-defined JSON color palettes. |

---

## Category Style Mapping & Tokens

Each key category maps to specific visual color tokens:

- **`alphaKey`**: Standard letter and punctuation keys.
- **`numberKey`**: Number row and numeric keypad keys.
- **`modifierKey`**: `Shift`, `Ctrl`, `Alt`, `Super`, `Fn` modifier keys.
- **`functionKey`**: `F1` through `F12` function keys.
- **`actionKey`**: Primary action keys (`Enter`, `Backspace`, `Space`, `Tab`, `Escape`).
- **`navigationKey`**: Navigation cluster keys (`PageUp`, `PageDown`, `Home`, `End`, `Arrow` keys).
- **`editingKey`**: Clipboard and text editing keys (`SelectAll`, `Copy`, `Cut`, `Paste`).

---

## Theme JSON Structure Example

```json
{
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
