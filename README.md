# Infinikey IME

An open-source, highly customizable soft keyboard for Android designed for power users, developers, and terminal environments (Termux, X11, VNC, RDP, SSH).

![Infinikey IME Banner](app/src/main/assets/images/infinikey-ime.png)

## Key Features

- **Clipboard History Manager**: Persistent overlay widget saving up to 30 copied items with index badges, character lengths, individual item deletion (`🗑`), long-press removal, clear-all action, and direct echo-paste connection to both text fields and raw terminal shells.
- **Vector SVG Icon Engine**: Crisp native canvas rendering for all key icons (`mic`, `tts`, `paperclip`, `clipboard`, `copy`, `cut`, `paste`, `select_all`) scaling cleanly across all screen densities.
- **3D Tactile Popup Overlays**: Modern glassmorphic 3D action popups with relaxed gesture tracking (28dp movement threshold, 40% item boundary hysteresis, and direct tap-to-select support).
- **Target Keyboard Detection**: Automatically inspects `EditorInfo` target (`isTerminalTarget()`) to seamlessly route clipboard operations to standard Android text controls or raw `TYPE_NULL` terminal streams.
- **Multiple Form Factors & Split Mode**: Docked, Split-Thumb Cluster, Left-Docked, Right-Docked, and Floating Window modes with instant spacebar (`␣`) long-press toggling.
- **Customizable Trackpad Navigation**: Independent configuration checkboxes for Spacebar Trackpad and Arrow Key Trackpad mouse/cursor emulation.
- **Authentic Mechanical Key Sounds**: Integrated Mechvibes sound packs (Cherry MX Blue, Brown, Red, Black, NovelKeys Cream, IBM Model M Buckling Spring).
- **WYSIWYG Layout Editor**: Built-in 6-tab Settings Activity with real-time drag-and-drop key reordering, row properties, undo/redo stack, and theme customization.

## Project Structure & Core Architecture

- **`app/src/main/java/com/programmerkeyboard/ProgrammerInputMethodService.kt`**: Core `InputMethodService` managing keyboard state, soft input connection, clipboard history listening, terminal detection, layout switching, and gesture dispatches.
- **`app/src/main/java/com/programmerkeyboard/view/`**:
  - `KeyboardView.kt`: Custom high-FPS View for rendering staggered and ortholinear key rows, vector SVG icon paths, trackpad gestures, and key states.
  - `KeyPopupOverlay.kt`: Floating 3D popup window with SVG icon caps and relaxed drag/tap touch handling.
  - `ClipboardHistoryOverlay.kt`: Interactive clipboard history overlay widget with single-item deletion and quick paste.
  - `TrackpadView.kt`: Integrated margin trackpad for cursor navigation.
- **`app/src/main/java/com/programmerkeyboard/model/`**:
  - `KeyDefinition.kt`: Data models for primary/secondary labels, vector SVG icons, and touch actions.
  - `LayoutDefinition.kt`: Layout structure for rows, offsets, and layers.
  - `KeyboardMode.kt`: Alignment modes (Staggered vs Rectangular) and Form Factors (Docked, Split, Left-Docked, Right-Docked, Floating).
- **`app/src/main/java/com/programmerkeyboard/engine/`**:
  - `LayoutParser.kt`: JSON layout engine loading layout descriptors.
  - `KeyRepeatEngine.kt`: Handles key repeat rates, long-press handlers, and analog arrow acceleration.
- **`app/src/main/assets/`**:
  - `images/`: SVG vector path assets (`copy.svg`, `cut.svg`, `paste.svg`, `select_all.svg`, `paperclip.svg`, `clipboard.svg`, `mic.svg`, `tts.svg`, `infinikey-ime.png`).
  - `layouts/`: JSON layout descriptors (`main.json`, `function.json`, `mobile.json`, `mobile_number.json`, `mobile_symbol.json`).

## Building the Project

Use Gradle to assemble the APK:

```bash
./gradlew assembleRelease assembleDebug
```

Output APKs: `app/build/outputs/apk/debug/infinikey-ime-v0.0.2-b72-debug.apk`

## License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.

## Attributions & Credits

Mechanical keyboard switch audio samples are sourced from **[Mechvibes](https://mechvibes.com/)**. See [ATTRIBUTION.md](ATTRIBUTION.md) for full sound pack credits.
