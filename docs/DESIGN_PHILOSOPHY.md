# Infinikey IME Design Philosophy

Infinikey IME is designed around a fundamental premise: a mobile soft keyboard should be a programmable input environment rather than merely a simplified desktop keyboard clone or a fixed arrangement of touch keys.

On modern touch devices—phones, foldables, and tablets—the screen space allocated to text entry is a dynamic software canvas. Infinikey exploits this to give power users, developers, terminal operators, and touch-typing enthusiasts total control over layout geometry, touch actions, multi-step macros, input widgets, and visual appearance.

---

## Core Principles

### 1. The Keyboard is an Input Environment

Conventional mobile keyboards treat text entry as a rigid, static interface with minor variations (such as auto-correct bars or basic symbol rows). Infinikey treats the keyboard as a configurable, stateful input environment.

Layouts, form factors, function layers, accessory panels, and interactive overlays can be dynamically reconfigured based on user preference or active workflow. Whether editing code in Termux, managing SSH sessions, composing documents, or launching application shortcuts, the keyboard surface adapts to the context.

### 2. Mobile Does Not Have to Mean Simplified

Mobile input design often over-simplifies controls by removing key combinations, function keys, navigation arrows, and precision cursor movement under the assumption that mobile users only type short messages.

Infinikey rejects this limitation. Phones and tablets possess high computing power and high-resolution displays. When operating in terminal environments, remote desktops (VNC/RDP), or software IDEs, mobile devices require desktop-class precision, including:
- Dedicated Function keys (`F1`–`F12`)
- Multi-modifier state handling (`Shift`, `Ctrl`, `Alt`, `Super`, `Meta`)
- Direct keycode and escape sequence emission
- Precision trackpad and arrow navigation
- Keystroke macro recording and execution

### 3. Configuration Should Be Declarative

Hardcoding keyboard layouts, key sizes, or touch actions in application source code creates rigidity and restricts user customization. Infinikey enforces a declarative model:
- Keyboard layouts are defined entirely in human-readable, human-editable JSON descriptors.
- Key geometry uses a unified ratio and absolute density-independent pixel (`dp`) specification.
- Touch actions (`onPress`, `onLongPress`, `onSwipeUp`, `onSwipeDown`, etc.) are declared as data payloads rather than procedural code.

This declarative model enables live, real-time visual editing via the built-in WYSIWYG editor and allows users to export, share, and version control custom layouts.

### 4. Layout, Behavior, and Appearance Should Be Independent

Infinikey enforces a clear separation of concerns across three architectural layers:

```
Layout
  ├── Geometry (rows, key ratios, spacing)
  ├── Key Definitions & Labels
  ├── Action Bindings
  └── Touch & Surface Gestures

Theme
  └── Visual Styling (palettes, colors, borders, radiuses)

Runtime
  └── Display State (form factor, screen mode, modifiers, active widgets)
```

A layout descriptor defines *what* keys exist and *how* they behave. A theme descriptor defines *how* the surface looks. Runtime state governs *where* and *how* the layout is rendered on screen. A single layout can be paired with any visual theme without altering layout descriptors or touch actions.

### 5. Power Users Should Be First-Class Users

Terminal users, SSH engineers, software developers, and keyboard enthusiasts should not have to fight mobile operating system assumptions. Infinikey provides:
- Direct, unbuffered keycode emission for shell targets (`TYPE_NULL` / raw input connection handling).
- Persistent clipboard history with index badges and shell-friendly echo-paste capabilities.
- Multi-step keystroke macros (`M1`–`M10`) with touch-and-hold recording.
- Full access to system app launching and custom multimedia controls.

### 6. The Keyboard as a Programmable Control Surface

Text entry is only one facet of touch input. Unused screen geometry—such as side deadspace in docked or split screen modes—can be repurposed as an **Accessory Area**. 

Instead of leaving deadspace blank, Infinikey enables embedding secondary control panels:
- Arrow navigation clusters
- 5x5 Application launcher grids
- Macro keypads
- Numeric keypads
- Multimedia & system controls

By combining text entry with auxiliary control surfaces, Infinikey turns the mobile soft keyboard into a powerful human-interface device.
