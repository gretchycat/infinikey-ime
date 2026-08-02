### Bugs & Architectural Rules

* **Layout-Driven Engine:** Layout files are the ultimate source of truth for key mapping, organization, rendering, and modifier actions. Every key using modifiers must have an explicit action for each unique combination used.

* **Enter Key Behavior:**
  * **Plain Enter:** Sends a standard carriage return / newline (`\n`).
  * **Ctrl + Enter:** Triggers form/chat submission (`performEditorAction`) in standard Android text fields (e.g., chat apps, UI text boxes). In Terminal applications (e.g., Termux), falls back to sending a standard `Enter` key code (`^M`).

* **Shift & Alternate Key Hierarchy:**
  * **Alphabetic Keys:** Shift selects the uppercase version of the key.
  * **Non-Alphabetic Keys:** Shift selects the **first alternate key** listed for that key.
  * **Alternate Key Case:** If an alternate key has an uppercase variant, the Shift modifier applies to the alternate key as well.

* **Universal Modifiers (`Ctrl`, `Alt`, `Super`, `Shift`):**
  * Modifiers must be capable of modifying every key and combination (e.g., `Alt+Enter`, `Alt+PageUp`, `Ctrl+PageUp`, `Ctrl+PageDown`).
  * `Ctrl+Enter` is the only unique exception specifically handled for non-terminal text fields as a submit action.

* **Theme Isolation:** Each color theme must reside in its own dedicated theme file.

* **Layout State Isolation:** Row hide/visibility status must be exclusive per layout so state does not leak between different layouts.

