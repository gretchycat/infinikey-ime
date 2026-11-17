# Infinikey IME application

## Purpose

Infinikey is a customizable Android input method and programmable text-entry environment. It targets ordinary mobile typing as well as workflows where compact touch keyboards are inefficient, including terminals, software development, remote desktops, tablets, foldables, and users who benefit from custom input controls. Accessibility-related customization is a product goal; this document does not claim formal accessibility conformance.

## Primary use cases

- Select or author layouts for mobile, desktop-style, numeric, symbol, function, emoji, navigation, media, macro, or launcher workflows.
- Send text and Android key events, use modifier states, gestures, alternate characters, macros, and configurable action keys.
- Change keyboard size/position and use full-width, split, side-docked, or floating presentation modes.
- Use clipboard history, trackpad/joystick navigation, haptics, key sounds, themes, and supported voice-input engines.
- Edit layout descriptors in the visual editor and exchange app-owned layouts/themes through its SAF DocumentsProvider.

## Implementation status

Status note: implementation statements below reflect inspected source and current repository documentation. This audit did not rerun builds or device acceptance; existing release/build checkpoints and device limits remain in the README and architecture guide.

### Implemented in this checkout

This is an implemented, standalone Android IME with an InputMethodService, a declarative JSON layout parser/rendering system, configurable actions and modifier states, multiple form factors, themes, an in-app layout editor, macro recording/replay, clipboard history, app-launcher slots, trackpad and joystick controls, and voice-input integrations. The source also includes system, local-model, and cloud speech engine adapters; usable engines depend on Android services, model assets, runtime permissions, and endpoint configuration. User layouts and themes are app-owned and can be exposed through InfinikeyDocumentsProvider using Android SAF.

Infinikey must remain useful as the active input method without any other Infiniforge app installed. It has its own settings, layouts, themes, storage provider, and lifecycle.

## Responsibilities and shared libraries

Infinikey owns the IME service, key dispatch, layout and theme behavior, customizations, keyboard UI, speech-to-text integration, and its own user data. It has no declared InfiniForge library dependency and does not use libinfiniselect or Infinifolder's storage provider/backend. Its provider is a separate Android SAF contract for Infinikey-owned files; it is not the suite storage provider.

Optional application launch or media actions use public Android APIs and remain dependent on target-app availability and platform rules. The keyboard does not own a suite-wide file selector, media player, launcher, or filesystem implementation.

## Interoperability

The [generated ecosystem map](ECOSYSTEM.md) describes this app's branding-only relationship to the other components. The IME sends input through Android's InputConnection and uses Android's input-method binding, permission, settings, clipboard, and activity contracts. Its launcher slots can target installed apps, but this is a keyboard convenience rather than a dependency on InfiniLaunch. Layout/theme file exchange uses URI grants from its own DocumentsProvider. No special InfiniFolder or libinfiniselect integration is implemented.

## Planned and potential work

Future work may improve accessibility controls, context-sensitive layouts, user dictionaries, phrase expansion/text transformations, speech engine availability, and optional integration actions for other InfiniForge applications. Those possibilities are not established as current features merely because the layout action system can insert text or launch apps. Keep keyboard text entry independent of companion-app availability.

## Limitations and outstanding work

- Input method behavior is constrained by Android editor targets, the active IME lifecycle, overlay/secure-setting policies, and permissions. Some key events or terminal behaviors depend on the target application.
- Speech-to-text availability varies by selected backend; cloud services require configuration and network access, while offline engines require compatible model assets/device resources.
- The current layout/action system does not establish a general predictive-text dictionary or full text-transformation engine. Accessibility and behavior across all Android app targets still need dedicated validation.
- Layout/theme documents belong to Infinikey and are distinct from filesystem resources selected through Infinifolder.

## Documentation audit

- Consistent: the app's architecture, user guide, and README describe a self-contained Android IME with its own settings and provider.
- Outdated: an old README block named version 0.3.3 APK files; it now directs readers to the standalone build and current Gradle-derived artifact naming.
- Incomplete: prior overview did not clearly distinguish keyboard-level launch/media conveniences from suite applications or spell out limits around dictionaries/accessibility.
- Unverified: formal accessibility conformance and behavior across all target editors are not established by the available docs.
