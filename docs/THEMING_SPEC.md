# Color Themes & Preset Palette Specification

## Theme Architecture
Programmer Keyboard supports decoupled, per-preset color themes loaded from `assets/themes.json` or custom user JSON configurations.

---

## Supported Built-In Theme Presets

| Index | Identifier | Display Name | Theme Description |
|-------|------------|--------------|-------------------|
| `0` | `system_auto` | System Dynamic | Follows device Light Mode (`system_light`) and Dark Mode (`slate`). |
| `1` | `slate` | Slate Dark (Default) | Deep slate blue background with bright cyan/amber accents. |
| `2` | `cyberpunk` | Cyberpunk Neon | Neon purple background with neon yellow, magenta, and cyan keycaps. |
| `3` | `oled` | OLED True Black | `#000000` pitch black background for OLED energy efficiency. |
| `4` | `matrix` | Matrix Terminal | Hacker green terminal theme on deep black background. |
| `5` | `retro` | Retro Vintage (Classic Beige) | Deep warm tan keycaps `#B8AD9C`/`#ADA291` on taupe beige `#6E6454`. |
| `6` | `muted_slate` | Low Saturation Slate | Low-contrast monochromatic slate palette for minimal distraction. |
| `7` | `custom` | Custom Theme | Reads `pref_custom_theme_json` for user-defined JSON color palettes. |

---

## Category Style Mapping

Each key category maps to specific visual color tokens:

- **`alphaKey`**: Standard letter and symbol keys.
- **`numberKey`**: Number row and numeric keypad keys.
- **`modifierKey`**: `Shift`, `Ctrl`, `Alt`, `Super`, `Fn` keys.
- **`functionKey`**: `F1` through `F12` keys.
- **`actionKey`**: `Enter`, `Backspace`, `Space`, `Tab`, `Escape` keys.
- **`navigationKey`**: `PageUp`, `PageDown`, `Home`, `End`, `Arrow` keys.
- **`editingKey`**: `Insert`, `Delete`, `Copy`, `Cut`, `Paste`, `SelectAll` keys.
