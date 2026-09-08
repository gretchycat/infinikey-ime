# ⌨️ Infinikey IME

An open-source, layout-driven, highly customizable soft keyboard for Android designed for power users, software developers, terminal environments (Termux, SSH, X11, VNC, RDP), and modern mobile typing.

---

## ✨ Key Highlights & Features

- **🎨 Declarative Layout & Theme Engine**: JSON-driven keyboard layouts with row visibility toggles, Fn layer switching, custom staggered vs ortholinear key arrangements, and HSL/RGB palette generation with theme JSON overrides.
- **🚀 5x5 App Launcher Grid & Categorized App Selector**: Bind keys to `LAUNCH` / `LAUNCH_APP` actions with slot persistence (`launcher.json`), dynamic app icon bitmap rendering directly on keycaps, and a 9-category Application Selector (`AppPickerActivity`) discovering 100% of installed apps.
- **🎵 Multimedia & Control Pad Layout**: Built-in `media.json` layout with pure monochrome text Unicode glyphs (volume `⊘`, `−`, `+`, playback `|◄`, `▶/❚❚`, `►|`, seek `◄◄`, `■`, `►►`, shortcuts `🖩`, `⌖`, `♫`, `✉`, and brightness `☼`, `☀`), OS volume slider HUD integration (`AudioManager`), and auto-repeating volume, seek, and brightness keys.
- **⚙️ System Settings Permission (`WRITE_SETTINGS`)**: Integrated Write System Settings permission management in the Configuration App for direct OS screen brightness controls.
- **🧭 Accessory Layout & Text System**: Side-docked and split screen modes feature configurable secondary layout targets (`navigation`, `mobile_number`, `function`, `macro`, `media`, `launcher`, `mobile_symbol`, `none`) and custom multi-line text labels inside accessory area background cards.
- **⚡ Powerful Macro Keys & Gestures**: `MACRO` key action bindings with long-press recording for M1–M10 macro slots, single-tap replay, step persistence, directional swipe actions (`onSwipeUp`, `onSwipeDown`, etc.), keycode auto-repeats, and app launchers.
- **✍️ Key Spacing & Spacer Text**: Render custom static text labels, headers, and section names directly inside key spacing gaps (`spacer` keys with `label`, `fgColor`, `fontSize`) and within accessory cards.
- **📋 Clipboard History Overlay**: Persistent overlay saving up to 30 copied items with index badges, character lengths, individual item deletion (`🗑`), clear-all, and direct echo-paste connection to terminal streams.
- **🔊 Authentic Mechanical Switch Audio**: Integrated Mechvibes switch packs (Cherry MX Blue, Brown, Red, Black, NovelKeys Cream, EG Oreo, EG Crystal Purple, Topre Silent Purple, IBM Model M Buckling Spring) with key-down/up split audio pipeline and tactile haptics.
- **🎯 Trackpad & Form Factors**: Spacebar trackpad and arrow key joystick cursor navigation, plus Docked, Left-Docked, Right-Docked, Split Thumb-Cluster, and Floating Window modes with drag repositioning and persistent offset memory.
- **📐 Visual WYSIWYG Editor**: Built-in layout editor with real-time drag-and-drop key reordering, row properties, accessory layout dropdown, accessory text inputs, and live interactive preview.

---

*Open Source under the MIT License • Built for Android*
