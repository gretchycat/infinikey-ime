# Project Specification: Advanced Modular Android Keyboard

## Overview
An open-source, highly customizable soft keyboard for Android designed for power users, developers, and terminal environments (e.g., Termux, X11 applications). It brings full desktop-style keyboard layouts, extensive remapping capabilities, ergonomic layout transformations, and integrated mouse emulation into a modern touch interface.

---

## Configuration & Settings Interface

### Geometry & Positioning Parameters
* **Height Percentage:** Configurable slider ranging from **20% to 50%** of total display height (default: **30%**).
* **Screen Mode Toggle:** Switchable between four core states:
  * **Full-Width Docked:** Standard bottom-anchored layout spanning 100% display width.
  * **Split Mode:** Divides keys into left and right thumb clusters separated by a central trackpad zone.
  * **Dock Left:** Snaps the layout to the left screen margin.
  * **Dock Right:** Snaps the layout to the right screen margin.
  * **Float Mode:** Detaches the keyboard into a moveable overlay window.
* **Aspect Ratio / Width Factor:** Defines keyboard width relative to height where a factor of `1.0` yields square proportions and `2.0` yields a width twice its height.

---

## Layout Management & Storage Engine

### File Hierarchy & Presets
* **Preset Core Layouts:** Bundled with three default factory definitions:
  1. `main` (Default 5-row staggered base layout)
  2. `function` (6-row expanded utility & numpad layout)
  3. `emoji` (Sub-keyboard layout for character selection)
* **Writable Layer & Restoration:**
  * On initial launch, bundled presets are copied to a user-writable directory (`/sdcard/Android/data/...` or internal app storage).
  * The settings menu provides a "Reset to Defaults" option to restore writable layout files back to factory configurations.
* **Extensibility:** Users can add, modify, or delete custom layout files without limit.

### Runtime Navigation & Defaults
* **Default Fallback:** The engine always defaults to loading the `main` layout upon initialization.
* **Action Primitive:** Key bindings and gestures can invoke a `switch_layout(layout_name)` action to swap active pages seamlessly (e.g., toggling from `main` to `function` or launching `emoji`).

---

## Layout Geometry, Sizing, & Alignment Logic

* **Dynamic Grid Engine:** Layout geometry (staggered vs. ortholinear) is calculated implicitly directly from key width factors and offsets within the layout definition descriptor.
* **Inter-Key Spacing Parameters:**
  * `horizontal_spacing`: Defines the fixed pixel/dp padding gap between adjacent keys in a row (defaults to global layout config value).
  * `vertical_spacing`: Defines the fixed pixel/dp padding gap between adjacent key rows (defaults to global layout config value).
* **Vertical Dimensioning:** 
  $$\text{Key Height} = \frac{\text{Total Keyboard Height} - (\text{vertical\_spacing} \times (N_{\text{rows}} - 1))}{N_{\text{rows}}}$$
* **Horizontal Dimensioning:**
  $$\text{Key Width} = (\text{Keyboard Width} - \text{Total Row Spacing}) \times \left( \frac{\text{Width Factor}}{\sum \text{Width Factors in Row}} \right)$$
  * Standard key `width_factor` defaults to `1.0`. Keys like `Space` use a higher factor (e.g., `4.0`).

---

## Layout & Key Mapping Definitions

#### Default Mode (`main`)
```csv
Row,Key1,Key2,Key3,Key4,Key5,Key6,Key7,Key8,Key9,Key10,Key11,Key12
1,`,1,2,3,4,5,6,7,8,9,0,-,=,Backspace
2,Tab,q,w,e,r,t,y,u,i,o,p,[,],|
3,Ctrl,a,s,d,f,g,h,j,k,l,;,",",Enter
4,Shift,z,x,c,v,b,n,m,",",.,/,UpArrow,Shift
5,Esc,Mic,Alt,Super,Space,Fn,Emoji,LeftArrow,DownArrow,RightArrow
```

#### Function / Alternate Mode (`function`)
```csv
Row,Key1,Key2,Key3,Key4,Key5,Key6,Key7,Key8,Key9,Key10,Key11,Key12
1,Mic,F1,F2,F3,F4,7,8,9,/,Home,Backspace
2,Tab,F5,F6,F7,F8,4,5,6,*,End,Ins,Delete
3,Ctrl,F9,F10,F11,F12,1,2,3,-,PgUp,Enter
4,Shift,SysRq,ScrL,Brk,NumL,0,",",.,+,PgDn,UpArrow,Shift
5,Esc,Settings,Alt,Super,Space,Fn,Emoji,LeftArrow,DownArrow,RightArrow
```

---

## Declarative Layout Descriptor Engine

### Functional Bindings & Sub-Widgets
* **Key Echoing:** Sends standard keycodes or text strings.
* **Layout Switching:** Executes `switch_layout(target)` primitives.
* **Sub-Widget Launching:** Triggers overlays (e.g., virtual joystick simulator or settings page).

### Modifier States & Visual Indicators
* **Toggleable Modifiers:** `Shift`, `Ctrl`, `Alt`, and `Super` support sticky/latching toggle behaviors.
* **Shift Modes:** Single-tap for standard shift; double-tap cycles to **Shift Lock** or **Caps Lock**.
* **Visual State Indicators:** Explicit visual cues (highlights or icons) denote inactive, active, and locked modifier states.

### Visual, Spacing, & Sizing Styling Per Layout/Key
* `horizontal_spacing`: Padding gap between horizontal keys.
* `vertical_spacing`: Padding gap between vertical rows.
* `width_factor`: Relative width weighting (`1.0` standard, `4.0` spacebar).
* `background_color`: Custom hex/rgba value per state (normal, pressed, toggled).
* `foreground_color`: Custom label and icon color.
* `background_image`: Optional asset path for key textures/icons.

---

## Ergonomics, Screen Modes, & Mouse Emulation

* **Margin Trackpad Area:** In non-full-width docked modes (Split, Dock Left, Dock Right), unallocated screen space acts as a touch trackpad for mouse cursor and click emulation (optimized for Termux/X11).
* **Floating Exemption:** Float mode disables margin trackpad mapping to preserve interaction with underlying applications.

