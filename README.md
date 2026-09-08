# Infinikey IME

An open-source, layout-driven, programmable soft keyboard for Android designed for power users, software developers, terminal environments (Termux, SSH, X11, VNC, RDP), and modern mobile typing.

![Infinikey IME Banner](app/src/main/res/mipmap-hdpi/ic_launcher.png)

---

## Why Infinikey?

Infinikey is a programmable Android input environment whose primary surface is a soft keyboard.

Rather than treating a keyboard as a fixed arrangement of keys, Infinikey treats it as a configurable input environment. The core power of Infinikey comes from its composability: layout geometry, touch actions, surface gestures, function layers, modifier states, keystroke macros, accessory panels, interactive widgets, screen docking modes, and visual color themes can be independently combined into tailored interfaces for different workflows.

Infinikey is particularly useful for:
- Software developers
- Termux users
- SSH & terminal environment operators
- Remote desktop (VNC/RDP) users
- Tablets, foldables, and large-screen devices
- Users who want desktop-style controls on Android
- Users who want a fully programmable and customizable keyboard surface

---

## What Makes Infinikey Different?

- **Declarative Layout Engine**: Keyboard layouts defined entirely in human-readable JSON files, separating geometry, key definitions, and touch actions from visual styling.
- **Composable Component Architecture**: Combine layouts, modifier states, surface gestures, macro pads, accessory panels, interactive widgets, screen modes, and color themes.
- **Programmable Actions & Keystroke Macros**: Bind custom keystrokes, touch gestures, keycode auto-repeats, app launchers, and multi-step macro sequences (`M1`–`M10`).
- **5 Screen Docking Modes**: Full-width docked, split thumb clusters, left-docked, right-docked, and floating window modes with persistent position memory.
- **Accessory Area Panel System**: Repurposes unused screen space in split or docked modes as a programmable control surface for navigation, media, macros, numeric keypads, or custom asset graphics.
- **Power-User & Terminal Controls**: Dedicated Function row (`F1`–`F12`), multi-modifier states (`Shift`, `Ctrl`, `Alt`, `Super`, `Meta`), unbuffered shell keycode dispatches, and persistent clipboard history.
- **Decoupled Theme Engine**: 9 built-in theme presets plus custom HSL/RGB palette generation and theme JSON file loading.
- **Extensible Overlays & Widgets**: Built-in 5x5 application launcher grid, floating clipboard history overlay, trackpad cursor navigation, and speech-to-text integration.

---

## Features

### Keyboard & Layouts
- **Desktop & Mobile Layouts**: Full 5-row QWERTY base layout ([`main.json`](app/src/main/assets/layouts/main.json)), compact mobile layout ([`mobile.json`](app/src/main/assets/layouts/mobile.json)), numeric keypad ([`mobile_number.json`](app/src/main/assets/layouts/mobile_number.json)), symbols & math ([`mobile_symbol.json`](app/src/main/assets/layouts/mobile_symbol.json)), and phone dialer ([`phone.json`](app/src/main/assets/layouts/phone.json)).
- **Function Layers & Visibility Toggling**: Instant Fn layer switching ([`function.json`](app/src/main/assets/layouts/function.json)) and dynamic per-row visibility toggling (`TOGGLE_ROW`).
- **Emoji Layout Engine**: Dynamic recent emojis tracking layout (`emoji_recents`) and category layouts ([`emoji.json`](app/src/main/assets/layouts/emoji.json), [`emoji_animals.json`](app/src/main/assets/layouts/emoji_animals.json), etc.) with skin tone alternate popups.
- **Docking Form Factors**: Full-Width Docked, Left-Docked, Right-Docked, Split Thumb-Cluster, and Floating Window modes.
- **Dynamic Layout Switching**: Swap active layout layers instantly on key press or open the dynamic Meta Layout Picker.

### Programmability
- **Keystroke Macro System**: Touch-and-hold recording and single-tap replay for slots `M1` through `M10` ([`macro.json`](app/src/main/assets/layouts/macro.json)).
- **Multi-Touch & Surface Gestures**: Two-finger swipe gestures for screen mode docking and directional keycap swipes (`onSwipeUp`, `onSwipeDown`, etc.).
- **Modifier Handling**: Latched (one-shot) and locked states for `Shift`, `Ctrl`, `Alt`, `Super`, and `Meta` modifiers.
- **Auto-Repeat Controls**: Continuous auto-repeats for Backspace, Delete, Arrow keys, volume, seek, and brightness keys.

### Power User Tools
- **Clipboard History Overlay**: Persistent overlay saving up to 30 copied items with index badges, character lengths, individual item deletion (`🗑`), clear-all, and direct echo-paste connection to terminal streams.
- **Trackpad & Cursor Navigation**: Spacebar trackpad mode and arrow key joystick trackpad overlay.
- **App Launcher Grid & App Selector**: Built-in 5x5 launcher grid ([`launcher.json`](app/src/main/assets/layouts/launcher.json)) with 25 customizable app shortcut slots, app icon bitmap rendering on keycaps, and a 9-category Application Selector discovering launchable system and user apps.
- **Multimedia Controls**: Built-in [`media.json`](app/src/main/assets/layouts/media.json) layout with Unicode glyph controls (`⊘`, `−`, `+`, `|◄`, `▶/❚❚`, `►|`, `◄◄`, `■`, `►►`, `🖩`, `⌖`, `♫`, `✉`, `☼`, `☀`), OS volume slider HUD integration, and direct OS screen brightness controls (requires `WRITE_SETTINGS` permission).

### Customization & Storage Access
- **Declarative Layout JSON Specs**: Fully customizable JSON descriptors defining geometry, spacing, rows, keys, actions, and styles.
- **Visual Layout Editor (WYSIWYG)**: Interactive canvas with drag-and-drop key reordering, row properties, key style classes, action type pickers, and undo/redo state history stack.
- **Decoupled Themes**: 9 built-in presets (System Auto, System Light, System Dark, Slate Dark, Cyberpunk Neon, OLED True Black, Matrix Terminal, Retro Vintage, Muted Slate) plus HSL/RGB custom color palette picker.
- **Accessory Area Panels**: Embed secondary layouts (`navigation`, `mobile_number`, `function`, `macro`, `media`, `launcher`, `mobile_symbol`) or custom multi-line text and asset graphics in side/center accessory space.
- **External Storage Access Framework**: Exposes custom layout and theme JSON files to external file managers and text editors via Android's Documents Provider (`InfinikeyDocumentsProvider`).

### Feedback
- **Mechanical Switch Audio Engine**: Integrated Mechvibes switch sound packs (Cherry MX Blue/Brown/Red/Black, NovelKeys Cream, EG Oreo, EG Crystal Purple, Topre Silent Purple, IBM Model M Buckling Spring) with key-down/up split audio pipeline and 5 synthesized click modes.
- **Haptic Engine**: Android `Vibrator` and `HapticFeedbackConstants` with customizable vibration intensity and pulse styles (`SHARP_CLICK`, `CRISP_TICK`, `HEAVY_CLICK`, `DOUBLE_CLICK`, `CUSTOM_PULSE`).
- **Key Previews**: Magnified key pop-up preview bubbles on touch down.

---

## Documentation

For detailed information, specifications, and guides, refer to the project documentation:

- 📖 **[User Guide](docs/USER_GUIDE.md)**: Installation, layout switching, form factors, macros, clipboard, launcher, trackpad, and customization walkthroughs.
- 📐 **[Layout Descriptor Specification](docs/LAYOUT_DESCRIPTOR_SPEC.md)**: Formal specification of JSON layout descriptors, schema properties, dimensioning rules, categorized actions, and accessory area configuration.
- 🎨 **[Theme Specification](docs/THEMING_SPEC.md)**: Specifications for color themes, preset palettes, category style tokens, and theme versioning.
- ✏️ **[WYSIWYG Editor Specification](docs/WYSIWYG_EDITOR_SPEC.md)**: Interactive canvas features, drag-and-drop key reordering, and property editor modal specifications.
- 🏗️ **[Developer Architecture](docs/ARCHITECTURE.md)**: Internal application architecture, `InputMethodService` dispatches, rendering engine, asset pipelines, STT integration, and storage providers.
- 💡 **[Design Philosophy](docs/DESIGN_PHILOSOPHY.md)**: The core design principles behind treating mobile keyboards as programmable input environments.

---

## Building the Project

Assemble debug or release APKs using Gradle:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Generated APK output locations:
- **Debug**: `app/build/outputs/apk/debug/infinikey-ime-v0.2.32-b183-debug.apk`
- **Release**: `app/build/outputs/apk/release/infinikey-ime-v0.2.32-b183-release.apk`

---

## License & Attributions

- **License**: Open-source under the [MIT License](LICENSE).
- **Attributions**: Mechanical switch audio samples are sourced from **[Mechvibes](https://mechvibes.com/)**. See [ATTRIBUTION.md](ATTRIBUTION.md) for full credits.
