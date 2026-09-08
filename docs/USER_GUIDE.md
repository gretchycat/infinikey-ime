# Infinikey IME User Guide

Welcome to the **Infinikey IME User Guide**. This guide explains how to install, configure, and use Infinikey IME on Android devices.

---

## Table of Contents

1. [Getting Started & Installation](#1-getting-started--installation)
2. [Selecting & Switching Layouts](#2-selecting--switching-layouts)
3. [Form Factors & Screen Docking Modes](#3-form-factors--screen-docking-modes)
4. [Using the Macro System](#4-using-the-macro-system)
5. [Clipboard History Overlay](#5-clipboard-history-overlay)
6. [App Launcher Grid & App Selector](#6-app-launcher-grid--app-selector)
7. [Trackpad & Cursor Navigation](#7-trackpad--cursor-navigation)
8. [Multimedia & System Controls](#8-multimedia--system-controls)
9. [Themes, Audio & Haptics Customization](#9-themes-audio--haptics-customization)
10. [Visual Layout Editor (WYSIWYG)](#10-visual-layout-editor-wysiwyg)

---

## 1. Getting Started & Installation

### Step 1: Enable Infinikey IME in Android Settings
1. Open your Android device **Settings** -> **System** -> **Languages & Input** -> **On-screen Keyboard** (or **Manage Keyboards**).
2. Toggle **Infinikey IME** to **On**.

### Step 2: Set Infinikey IME as Active Input Method
1. Tap any text field or open the Infinikey configuration app (`SettingsActivity`).
2. Tap **Enable & Select IME** button or tap the keyboard switcher icon `🌐` on the navigation bar to select **Infinikey IME**.

---

## 2. Selecting & Switching Layouts

Infinikey IME supports multiple built-in layouts tailored for different input tasks:

| Layout ID | Layout Name | Description |
| :--- | :--- | :--- |
| `main` | Full Programmer Layout | 5-row QWERTY layout with Function keys (`F1`–`F12`), arrow navigation, and modifier toggles. |
| `mobile` | Standard Mobile Layout | Compact mobile typing layout with secondary long-press symbols. |
| `mobile_number` | Numeric Keypad | Calculator/numpad cluster layout. |
| `mobile_symbol` | Symbols & Math | Specialized layout for bracket pairs, punctuation, and mathematical symbols. |
| `function` | Function & Navigation Layer | Dedicated `F1`–`F12` and navigation block layer. |
| `phone` | Phone Dialer | Telephone keypad layout. |
| `media` | Multimedia Control Pad | Volume, playback, track seek, calculator, and brightness controls. |
| `launcher` | 5x5 App Launcher Grid | 25 customizable app shortcut slots. |
| `macro` | Macro Pad Layout | 2x5 grid featuring `M1` through `M10` macro keys. |

### Switching Layouts at Runtime
- **Layout Switcher Key**: Tap the layout key (e.g. `Fn`, `123`, `🌐`, or `ABC`) to swap the active layout.
- **Meta Picker**: Long-pressing or tapping the layout switcher can open the dynamic **Meta Layout Picker**, allowing you to pick any installed or custom user layout.

---

## 3. Form Factors & Screen Docking Modes

Infinikey IME supports 5 screen docking modes:

```
┌─────────────────────────────────────────────────────────────┐
│                      FULL_WIDTH_DOCKED                      │
└─────────────────────────────────────────────────────────────┘

┌──────────────────────┬──────────────────────┬───────────────┐
│     LEFT_DOCKED      │     RIGHT_DOCKED     │     SPLIT     │
└──────────────────────┴──────────────────────┴───────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      FLOATING WINDOW                        │
└─────────────────────────────────────────────────────────────┘
```

1. **Full Width Docked (`FULL_WIDTH_DOCKED`)**: Standard full-width keyboard docked at the bottom of the screen.
2. **Left Docked (`LEFT_DOCKED`)**: Aligned to the left border of the screen, creating an accessory panel on the right for single-handed typing.
3. **Right Docked (`RIGHT_DOCKED`)**: Aligned to the right border of the screen, creating an accessory panel on the left for single-handed typing.
4. **Split Mode (`SPLIT`)**: Divides keys into left and right thumb clusters separated by a central accessory region (ideal for tablets and foldables).
5. **Floating Window (`FLOATING`)**: Renders a draggable floating window overlay with top handle bar and persistent window position memory.

### Switching Docking Modes
- **Surface Touch Gestures**:
  - Two-finger swipe left: `LEFT_DOCKED`
  - Two-finger swipe right: `RIGHT_DOCKED`
  - Two-finger swipe up: `FLOATING`
  - Two-finger swipe down: `FULL_WIDTH_DOCKED`
  - Two-finger pinch out: `SPLIT`
- **Settings Menu**: Select form factor via **Settings -> Geometry & Form Factor -> Screen Docking Mode**.

---

## 4. Using the Macro System

The macro engine allows you to record and replay multi-step keystroke sequences on keys configured with the `"MACRO"` action (`M1`–`M10`).

### Replaying a Macro
- **Single Tap**: Tap any recorded macro key (e.g., `M1`) to execute all saved keystrokes in order.

### Recording a New Macro
1. **Long-press** any macro key (`M1`–`M10`). The keyboard enters macro recording mode for that slot (a recording indicator appears).
2. Type the desired keystrokes, characters, or key sequences.
3. **Tap** the recording macro key again to stop recording and save the macro. A toast summary confirms the step count.

---

## 5. Clipboard History Overlay

Infinikey IME automatically monitors copied text and maintains a persistent history overlay saving up to 30 items.

- **Opening Clipboard History**: Tap the clipboard icon `📋` or trigger the `CLIPBOARD_HISTORY` widget action.
- **Pasting an Item**: Tap any history entry in the floating list to insert text directly into your target input field or raw terminal shell.
- **Echo Paste**: Uses `PASTE_ECHO` to feed clipboard contents line-by-line into shell environments (e.g., Termux or SSH).
- **Managing Items**: Tap `🗑` to delete an individual item, or tap **Clear All** to reset clipboard history.

---

## 6. App Launcher Grid & App Selector

The built-in `launcher.json` layout provides a 5x5 grid with 25 app launcher slots (`slot_0` through `slot_24`).

- **Launching an App**: Tap any assigned key slot to launch the application. App icon graphics render directly on keycaps.
- **Assigning / Changing Apps**:
  - Tap an unassigned slot or **long-press** any slot to launch the **Application Selector** (`AppPickerActivity`).
  - Search or browse through 9 thematic categories:
    - 🌐 **Internet & Browsers**
    - 💬 **Social & Communication**
    - 🎵 **Audio & Music**
    - 🎥 **Video & Movies**
    - 📷 **Photos & Graphics**
    - 🛠️ **Productivity & Utilities**
    - 🎮 **Games**
    - ⚙️ **System & Settings**
    - 📱 **Other Applications**
  - Tap an app entry to bind it to the target slot. Assignments persist across app restarts.

---

## 7. Trackpad & Cursor Navigation

Infinikey IME includes dual trackpad cursor emulation modes for precise text navigation:

1. **Spacebar Trackpad**: Long-press or drag your finger along the spacebar (`␣`) to transform the spacebar into an analog trackpad for precise cursor placement.
2. **Arrow Key Trackpad / Joystick**: Drag across the arrow key cluster or trigger the `JOYSTICK` widget to activate an analog cursor trackpad with visual feedback.

---

## 8. Multimedia & System Controls

The built-in `media.json` layout provides system media and utility controls using monochrome Unicode glyphs:

- **Volume Controls**: Volume Down (`−`), Volume Up (`+`), Mute (`⊘`) with OS volume slider HUD integration (`AudioManager`).
- **Media Playback**: Previous Track (`|◄`), Play/Pause (`▶/❚❚`), Next Track (`►|`), Rewind (`◄◄`), Stop (`■`), Fast Forward (`►►`).
- **Shortcuts**: Calculator (`🖩`), System Settings (`⌖`), Audio Player (`♫`), Email Client (`✉`).
- **Brightness Controls**: Brightness Down (`☼`), Brightness Up (`☀`) with auto-repeat. *Note: Screen brightness adjustment requires granting System Settings Write permission (`WRITE_SETTINGS`) in the app settings.*

---

## 9. Themes, Audio & Haptics Customization

### Preset Color Themes
Select from 9 built-in theme presets or create your own:
- **System Dynamic (`system_auto`)**: Automatically follows device OS Light / Dark mode.
- **System Light (`system_light`)**: Clean light mode theme.
- **System Dark (`system_dark`)**: Deep dark mode theme.
- **Slate Dark (`slate`)**: Default dark slate blue `#0F172A` theme with cyan `#38BDF8` accents.
- **Cyberpunk Neon (`cyberpunk`)**: High-contrast neon purple, yellow, and cyan.
- **OLED True Black (`oled`)**: `#000000` pitch black background optimized for OLED power saving.
- **Matrix Terminal (`matrix`)**: Hacker green monochrome text on deep black.
- **Retro Vintage (`retro`)**: Classic beige mechanical keyboard aesthetic.
- **Muted Slate (`muted_slate`)**: Low-saturation slate for distraction-free typing.
- **Custom HSL/RGB Palette**: Build custom color themes in **Settings -> Themes -> Custom Palette Picker**.

### Mechanical Switch Audio Engine
Choose from recorded mechanical switch audio packs sourced from Mechvibes, or synthesized click tones:
- **Recorded Switch Packs**: Cherry MX Blue, Brown, Red, Black; NovelKeys Cream; EG Oreo, EG Crystal Purple; Topre Silent Purple; IBM Model M Buckling Spring.
- **Synthesized Click Audio**: 5 adjustable audio click modes with volume control.

### Haptic Feedback
Configure vibration intensity, duration, and tactile pulse styles (`SHARP_CLICK`, `CRISP_TICK`, `HEAVY_CLICK`, `DOUBLE_CLICK`, `CUSTOM_PULSE`).

---

## 10. Visual Layout Editor (WYSIWYG)

Build and customize keyboard layouts visually using the built-in layout editor in **Settings -> Layout Editor**:

- **Real-Time Key Dragging**: Touch and drag keycaps to reorder keys within a row or move them across rows.
- **Key Properties Modal**: Tap any key in editor mode to change its label, secondary label, width weight, style class, action type, parameters, or spacer toggle.
- **Row Management**: Add new keys to existing rows or append new rows.
- **Accessory Space Controls**: Select the default accessory layout target, or enter custom multi-line text and asset image paths to display in side/center accessory panels.
- **Undo / Redo History**: Undo (`↩️`) and Redo (`↪️`) state stack preserves layout edits safely before saving.

---

## Visual Reference

Refer to the included screenshot captures for visual examples:
- Layout canvas: [`Screenshot_20260825-192241.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192241.png)
- Settings & theme configuration: [`Screenshot_20260825-192254.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192254.png)
- Layout variations & editing: [`Screenshot_20260825-192450.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192450.png), [`Screenshot_20260825-192553.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192553.png), [`Screenshot_20260825-192659.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192659.png), [`Screenshot_20260825-192836.png`](file:///data/data/com.termux/files/home/Projects/infinikey-ime/screenshots/Screenshot_20260825-192836.png).

> [!NOTE]
> Additional visual walkthroughs for Split Mode and App Launcher Grid are planned for future documentation releases.
