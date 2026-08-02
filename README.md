# Infinikey IME

An open-source, highly customizable soft keyboard for Android designed for power users, developers, and terminal environments (Termux, X11, VNC, RDP).

## Architecture & Base Files

- **`app/src/main/java/com/programmerkeyboard/ProgrammerInputMethodService.kt`**: Core `InputMethodService` managing keyboard state, soft input connection, layout switching, and trackpad gestures.
- **`app/src/main/java/com/programmerkeyboard/model/`**:
  - `KeyDefinition.kt`: Data models for primary/secondary labels and touch actions.
  - `LayoutDefinition.kt`: Layout structure for rows, offsets, and layers.
  - `KeyboardMode.kt`: Alignment modes (Staggered vs Rectangular) and Form Factors (Docked, Split, Side-Docked, Floating).
- **`app/src/main/java/com/programmerkeyboard/engine/`**:
  - `LayoutParser.kt`: JSON layout engine loading layout descriptors.
  - `KeyRepeatEngine.kt`: Handles key repeat rates, long-press handlers, and analog arrow joystick acceleration.
- **`app/src/main/java/com/programmerkeyboard/view/`**:
  - `KeyboardView.kt`: Custom View for rendering staggered and ortholinear key rows, swipe gestures, and active states.
  - `TrackpadView.kt`: Integrated margin trackpad for mouse emulation in split or side-docked modes.
- **`app/src/main/assets/layouts/main.json`**: JSON layout descriptor defining the default 5-row base and desktop Fn utility layers.

## Building the Project

Use Gradle to assemble the APK:

```bash
./gradlew assembleRelease assembleDebug
```

## License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.

## Attributions & Credits

Mechanical keyboard switch audio samples are sourced from **[Mechvibes](https://mechvibes.com/)**. See [ATTRIBUTION.md](ATTRIBUTION.md) for full sound pack credits.

