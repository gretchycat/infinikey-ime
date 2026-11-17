# InfiniForge ecosystem

This file is generated from the authoritative [ecosystem manifest](https://github.com/infiniforge/.github/blob/main/ecosystem.json) (inventory checked 2026-11-17). Do not edit this copy; changes are synchronized from that repository through reviewable pull requests.

InfiniForge is a set of independently buildable and released Android applications and reusable library repositories. Each app owns its application identity, UI, lifecycle, settings, user data, permissions, and standalone build. Libraries provide reusable APIs and do not make one application depend on another.

## Component inventory

The inventory contains 9 applications (4 with standalone builds and 5 planned placeholders), 11 shared libraries, and 1 canonical asset repository.

### Applications

#### [infinifolder](https://github.com/infiniforge/infinifolder)

**State:** active · **Build:** standalone app build · **Versions:** no published release verified; build declares `0.1.2`

**Purpose:** Android file manager and advanced storage companion. Owns the file manager UI and operations and composes libinfinicore, libinfinistore, and libinfinivault through its providers for local/SAF, remote, archive, and filesystem-image resources. It implements Android document/provider integration and remains optional to companion apps; shared backend code stays in the libraries.

**Pinned source dependency closure:** [libinfinicanvas](https://github.com/infiniforge/libinfinicanvas), [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfinimeta](https://github.com/infiniforge/libinfinimeta), [libinfiniscribe](https://github.com/infiniforge/libinfiniscribe), [libinfiniselect](https://github.com/infiniforge/libinfiniselect), [libinfinisound](https://github.com/infiniforge/libinfinisound), [libinfinistore](https://github.com/infiniforge/libinfinistore), [libinfinistream](https://github.com/infiniforge/libinfinistream), [libinfiniui](https://github.com/infiniforge/libinfiniui), [libinfinivault](https://github.com/infiniforge/libinfinivault), [libinfinivideo](https://github.com/infiniforge/libinfinivideo)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/infinifolder/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [architecture.md](https://github.com/infiniforge/infinifolder/blob/main/docs/architecture.md), [libinfiniselect.md](https://github.com/infiniforge/infinifolder/blob/main/docs/libinfiniselect.md), [runtime-capabilities.md](https://github.com/infiniforge/infinifolder/blob/main/docs/runtime-capabilities.md)

#### [infinimedia](https://github.com/infiniforge/infinimedia)

**State:** active · **Build:** standalone app build · **Versions:** no published release verified; build declares `0.1.2`

**Purpose:** Standalone Android audio, video, and image player. Owns playback UI and lifecycle, queues, playlists, player settings, casting, and media presentation. Reusable decoding and playback APIs belong to shared libraries. Local and SAF workflows remain available without Infinifolder.

**Pinned source dependency closure:** [libinfinicanvas](https://github.com/infiniforge/libinfinicanvas), [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfiniscribe](https://github.com/infiniforge/libinfiniscribe), [libinfiniselect](https://github.com/infiniforge/libinfiniselect), [libinfinisound](https://github.com/infiniforge/libinfinisound), [libinfinistream](https://github.com/infiniforge/libinfinistream), [libinfiniui](https://github.com/infiniforge/libinfiniui), [libinfinivideo](https://github.com/infiniforge/libinfinivideo)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/infinimedia/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** [infinifolder](https://github.com/infiniforge/infinifolder)

**Public API and architecture documentation:** [architecture.md](https://github.com/infiniforge/infinimedia/blob/main/docs/architecture.md), [storage.md](https://github.com/infiniforge/infinimedia/blob/main/docs/storage.md), [audio-formats.md](https://github.com/infiniforge/infinimedia/blob/main/docs/audio-formats.md), [video-formats.md](https://github.com/infiniforge/infinimedia/blob/main/docs/video-formats.md)

#### [infiniscribe](https://github.com/infiniforge/infiniscribe)

**State:** active · **Build:** standalone app build · **Versions:** no published release verified; build declares `0.1.1`

**Purpose:** Standalone Android document viewer/editor and ebook reader; format coverage is incomplete. Owns document viewer and editing UI, navigation, and app state. Reusable document parsing and format backends belong to shared libraries. Local and SAF workflows remain available without Infinifolder.

**Pinned source dependency closure:** [libinfinicanvas](https://github.com/infiniforge/libinfinicanvas), [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfiniscribe](https://github.com/infiniforge/libinfiniscribe), [libinfiniselect](https://github.com/infiniforge/libinfiniselect), [libinfinisound](https://github.com/infiniforge/libinfinisound), [libinfinistream](https://github.com/infiniforge/libinfinistream), [libinfiniui](https://github.com/infiniforge/libinfiniui)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/infiniscribe/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** [infinifolder](https://github.com/infiniforge/infinifolder)

**Public API and architecture documentation:** [architecture.md](https://github.com/infiniforge/infiniscribe/blob/main/docs/architecture.md), [storage.md](https://github.com/infiniforge/infiniscribe/blob/main/docs/storage.md), [text-and-document-formats.md](https://github.com/infiniforge/infiniscribe/blob/main/docs/text-and-document-formats.md), [document-and-ebook-readers.md](https://github.com/infiniforge/infiniscribe/blob/main/docs/document-and-ebook-readers.md)

#### [infinikey-ime](https://github.com/infiniforge/infinikey-ime)

**State:** active · **Build:** standalone app build · **Versions:** released `0.3.9`; build declares `0.3.9`; tag `v0.3.9`

**Purpose:** Independent Android keyboard input method. Owns its IME service, keyboard UI, layouts, themes, settings, and user data. It shares InfiniForge branding only and does not use libinfiniselect or Infinifolder's storage backend.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [ARCHITECTURE.md](https://github.com/infiniforge/infinikey-ime/blob/main/docs/ARCHITECTURE.md), [LAYOUT_DESCRIPTOR_SPEC.md](https://github.com/infiniforge/infinikey-ime/blob/main/docs/LAYOUT_DESCRIPTOR_SPEC.md), [THEMING_SPEC.md](https://github.com/infiniforge/infinikey-ime/blob/main/docs/THEMING_SPEC.md), [WYSIWYG_EDITOR_SPEC.md](https://github.com/infiniforge/infinikey-ime/blob/main/docs/WYSIWYG_EDITOR_SPEC.md)

#### [infinicanvas](https://github.com/infiniforge/infinicanvas)

**State:** planned placeholder · **Build:** no build yet · **Versions:** no published release verified; not versioned yet

**Purpose:** Planned Android image editor. Placeholder only; no application source or standalone build is present. Future editing workflows and app-owned state belong here.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** None published yet

#### [infiniwave](https://github.com/infiniforge/infiniwave)

**State:** planned placeholder · **Build:** no build yet · **Versions:** no published release verified; not versioned yet

**Purpose:** Planned Android audio file editor, with Audacity as a product reference. Placeholder only; no application source or standalone build is present. Future editing workflows and project state belong here.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** None published yet

#### [infinitrack](https://github.com/infiniforge/infinitrack)

**State:** planned placeholder · **Build:** no build yet · **Versions:** no published release verified; not versioned yet

**Purpose:** Planned Android music tracker and digital audio workstation. Placeholder only; no application source or standalone build is present. Future sequencing and production workflows and session state belong here.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** None published yet

#### [infinilaunch](https://github.com/infiniforge/infinilaunch)

**State:** planned placeholder · **Build:** no build yet · **Versions:** no published release verified; not versioned yet

**Purpose:** Planned Android application launcher. Placeholder only; no application source or standalone build is present. Future package discovery and launch functions belong here and must use public Android APIs.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** None published yet

#### [infiniplay](https://github.com/infiniforge/infiniplay)

**State:** planned placeholder · **Build:** no build yet · **Versions:** no published release verified; not versioned yet

**Purpose:** Planned universal-emulator frontend using the Libretro app as its backend. Placeholder only; no application source or standalone build is present. The intended integration is through the backend's supported public Android interfaces, not imported source or private files.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** None published yet

### Shared libraries

#### [libinfinicanvas](https://github.com/infiniforge/libinfinicanvas)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Reusable image decoding, thumbnails, bitmap/frame adapters, and image animation. Owns image codec and decode APIs and image data adapters; host applications own viewer/editor UI and lifecycle. It depends on Stream and declared codec adapters, not on Infinifolder or other applications.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinicanvas/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinicanvas/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinicanvas/blob/main/CAPABILITIES.md), [svg-palette-cycling.md](https://github.com/infiniforge/libinfinicanvas/blob/main/docs/svg-palette-cycling.md)

#### [libinfinicore](https://github.com/infiniforge/libinfinicore)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Provider-neutral resource contracts and Android local/SAF support. Owns resource identities, capability/error types, stream and block adapters, guarded content replacement and recovery, disk cache, diagnostics, MIME identification, transfer coordination, and Android local/SAF adapters. It does not own remote clients, mounted filesystems, or app UI.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinicore/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinicore/blob/main/README.md)

#### [libinfinimeta](https://github.com/infiniforge/libinfinimeta)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Identifies byte streams and extracts bounded, format-specific metadata through reusable registry and parser APIs. Owns metadata-facing library APIs extracted from the former application storage code; application presentation and app-owned metadata policy remain with consumers.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinimeta/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinimeta/blob/main/README.md)

#### [libinfiniscribe](https://github.com/infiniforge/libinfiniscribe)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Reusable document, text, ebook, and format-processing backends. Owns reusable parsers and format services. The Infiniscribe application owns document navigation, editing workflows, and viewer UI.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfiniscribe/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfiniscribe/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfiniscribe/blob/main/CAPABILITIES.md), [document-readers.md](https://github.com/infiniforge/libinfiniscribe/blob/main/docs/document-readers.md), [dos-text.md](https://github.com/infiniforge/libinfiniscribe/blob/main/docs/dos-text.md)

#### [libinfiniselect](https://github.com/infiniforge/libinfiniselect)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Reusable Android file open/save selection and resource-resolution client APIs. Owns the optional InfiniFolder picker integration, Android SAF fallback, grant-aware result parsing, distinction between authoritative resource references and current access URIs, and relative playlist resource resolution. It does not depend on the Infinifolder application.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfiniselect/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** [infinifolder](https://github.com/infiniforge/infinifolder)

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfiniselect/blob/main/README.md)

#### [libinfinisound](https://github.com/infiniforge/libinfinisound)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.2.0`

**Purpose:** Reusable audio parsing, decoding, synthesis, metadata, playlists, and signal processing. Owns shared audio processing and PCM APIs, portable decoders and synthesis, Android media adapters, and audio capability discovery. Consuming applications own playback UI, queues, and app state.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinisound/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinisound/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinisound/blob/main/CAPABILITIES.md), [architecture.md](https://github.com/infiniforge/libinfinisound/blob/main/docs/architecture.md), [third-party.md](https://github.com/infiniforge/libinfinisound/blob/main/docs/third-party.md)

#### [libinfinistore](https://github.com/infiniforge/libinfinistore)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Storage providers and remote/backend resource access. Owns local/remote storage backend implementations and provider integration, including network clients and storage protocol adapters. Shared resource primitives belong to libinfinicore; archive and filesystem-image handling belongs to libinfinivault.

**Pinned source dependency closure:** [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinistore/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinistore/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinistore/blob/main/CAPABILITIES.md), [dependencies.md](https://github.com/infiniforge/libinfinistore/blob/main/docs/dependencies.md)

#### [libinfinistream](https://github.com/infiniforge/libinfinistream)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Portable byte-stream, positional I/O, resource, metadata, and processing contracts. Owns JVM stream and processor APIs, capability and format discovery contracts, bounded probing, and processing negotiation/execution. Domain libraries provide actual handlers; it does not provide Android application UI.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinistream/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinistream/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinistream/blob/main/CAPABILITIES.md)

#### [libinfiniui](https://github.com/infiniforge/libinfiniui)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Shared Android viewer UI, themes, source adaptation, and common dialogs. Owns common UI and viewer dispatch, shared image viewer composition, theme/icon handling, external-editor handoff, diagnostics, and cache/recovery dialogs. Application screens and decoding implementations remain in their owners.

**Pinned source dependency closure:** [libinfinicanvas](https://github.com/infiniforge/libinfinicanvas), [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfiniscribe](https://github.com/infiniforge/libinfiniscribe), [libinfiniselect](https://github.com/infiniforge/libinfiniselect), [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfiniui/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** [infini-icon-theme](https://github.com/infiniforge/infini-icon-theme)

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfiniui/blob/main/README.md)

#### [libinfinivault](https://github.com/infiniforge/libinfinivault)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Archive and filesystem-image readers, writers, and mounted resource providers. Owns container parsing and writes, nested image filesystems, archive entry sources, compression, mutation batches, and native filesystem integration. It consumes storage/stream APIs and does not own the host app's cache policy or UI.

**Pinned source dependency closure:** [libinfinicore](https://github.com/infiniforge/libinfinicore), [libinfinisound](https://github.com/infiniforge/libinfinisound), [libinfinistore](https://github.com/infiniforge/libinfinistore), [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinivault/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinivault/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinivault/blob/main/CAPABILITIES.md), [dependencies.md](https://github.com/infiniforge/libinfinivault/blob/main/docs/dependencies.md)

#### [libinfinivideo](https://github.com/infiniforge/libinfinivideo)

**State:** active · **Build:** standalone library build · **Versions:** no published release verified; build declares `0.1.0`

**Purpose:** Reusable video playback contracts and Android/libVLC backend. Owns backend-neutral video controls/events and Android playback backend, native player integration, surfaces, playback events, and audio focus. Consuming applications own controls, navigation, lifecycle decisions, and source leases.

**Pinned source dependency closure:** [libinfinistream](https://github.com/infiniforge/libinfinistream)

**Exact dependency pins:** [dependencies.lock.json](https://github.com/infiniforge/libinfinivideo/blob/main/dependencies.lock.json)

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/libinfinivideo/blob/main/README.md), [CAPABILITIES.md](https://github.com/infiniforge/libinfinivideo/blob/main/CAPABILITIES.md), [libvlc.md](https://github.com/infiniforge/libinfinivideo/blob/main/docs/libvlc.md)

### Canonical assets

#### [infini-icon-theme](https://github.com/infiniforge/infini-icon-theme)

**State:** active · **Build:** documented asset build · **Versions:** no published release verified

**Purpose:** Canonical vector artwork and deterministic build tools for the InfiniFolder default icon theme. Owns icon-theme source artwork and its importable export. libinfiniui pins and packages the canonical theme; applications should not maintain independent copies of the theme source.

**Pinned source dependency closure:** None declared

**Exact dependency pins:** Use the component's Gradle/package metadata; no ecosystem source lock is declared.

**Optional/runtime integrations:** None recorded

**Public API and architecture documentation:** [README.md](https://github.com/infiniforge/infini-icon-theme/blob/main/README.md)

Exact dependency commits and package versions remain authoritative in each repository's Gradle/package metadata, source lock, or submodule gitlink. Dependency lists above identify ecosystem components in the locked source closure and can include transitive libraries. Optional integrations are runtime relationships, not build dependencies.

## Responsibility boundaries and resource flow

- **Applications** own their installable entry points and app-specific UI, state, data, and permissions. Infinifolder is the file manager and advanced storage companion, but it is optional. Infinimedia owns playback and playlists; Infiniscribe owns document viewing/editing; Infinikey owns its IME and its private layouts/themes. Canvas, Wave, Track, Launch, and Play are planned placeholders.
- **Libraries** own reusable public APIs. libinfinicore supplies provider-neutral resource contracts and Android local/SAF support; libinfinistore owns storage backends; libinfinivault owns archive/filesystem-image handling; libinfiniselect owns file selection and resource-resolution client APIs; libinfinistream owns stream/resource/processor contracts. Format, metadata, media, and shared UI responsibilities are assigned in the component inventory above.
- Apps must consume reusable functionality through the library's public API. They must not import another app's classes, database, private files, build output, or Gradle project. Each app must build from its own checkout and declared/pinned libraries.
- Cross-app operations use documented Android contracts: SAF/DocumentsProvider, Android intents, content URIs, and URI permission grants. Missing companion apps and denied permissions require a local/SAF or explicit error path.

### Authoritative resource identity and current access

A resource's authoritative URI/reference records its persistent identity and location. It can identify a local/SAF item, remote object, archive member, or filesystem-image resource. A resolved content URI is the current Android access handle, often accompanied by a time-limited grant. Keep these values distinct: persist the authoritative reference when supplied, pass access URIs only with valid grants, and do not treat a temporary relay as a durable identity. Relative references are resolved against their authoritative parent through libinfiniselect's existing APIs; apps must not independently parse private backend URI formats.

Infinifolder's DocumentsProvider exposes opaque document IDs through Android SAF. Its libinfiniselect relay currently expires after 24 hours. Android SAF document URIs can themselves be both identity and access URI when the provider defines them as canonical; preserve only grants Android actually issued. Infinikey's DocumentsProvider serves Infinikey-owned data and is separate from Infinifolder storage.

## Implemented interoperability contracts

The protocol versions below describe the current codebase. `null` means no InfiniForge protocol version field or negotiation mechanism was found; library version equality is not a substitute for protocol compatibility.

### Android Storage Access Framework (SAF)

**ID:** `android-saf` · **Protocol version:** `null` (platform-defined; no InfiniForge protocol version or negotiation is implemented) · **Implemented by:** [infinifolder](https://github.com/infiniforge/infinifolder), [infinikey-ime](https://github.com/infiniforge/infinikey-ime), [libinfiniselect](https://github.com/infiniforge/libinfiniselect)

- **Resource identity:** A provider's document or tree content URI is opaque. Preserve the URI and Android grants; do not decode document IDs or assume they are filesystem paths.
- **Request:** Clients use Android DocumentsContract/ContentResolver and ACTION_OPEN_DOCUMENT, ACTION_OPEN_DOCUMENT_TREE, or ACTION_CREATE_DOCUMENT. Requested read/write access is expressed through Android URI permission flags.
- **Response:** The selected provider returns document/tree URIs and only the permissions it grants. Operations include root/document listing, metadata queries, document reads, and provider-supported writes.
- **Errors:** Android provider and permission failures are surfaced through framework exceptions/results. Lack of a provider or denied grants must be handled by the caller.
- **Compatibility:** This is the Android platform contract. Infinifolder exposes com.infinifolder.documents; Infinikey exposes its own provider for Infinikey-owned layout/theme data. Those providers do not share storage or private data.
- **Fallback:** libinfiniselect uses Android SAF when the optional Infinifolder picker/provider cannot be used. Each application retains its own local/SAF workflow.
- **Negotiation:** No InfiniForge-specific version negotiation exists. Check provider availability and actual grants/capabilities at operation time.

### libinfiniselect open/save Activity contract

**ID:** `infini-select-intents` · **Protocol version:** `null` (unversioned; no compatibility handshake is implemented) · **Implemented by:** [libinfiniselect](https://github.com/infiniforge/libinfiniselect), [infinifolder](https://github.com/infiniforge/infinifolder)

- **Resource identity:** A result can carry a current grantable content URI and a separate authoritative URI. Persist the authoritative URI when it is supplied; a Folder relay is a temporary access handle.
- **Request:** FileSelectionContract defines ACTION_SELECT and ACTION_SAVE plus request extras for client identity, MIME/extensions, multiplicity, initial directory, and save options. Requests are sent to the optional Infinifolder selector when present.
- **Response:** Activity results include selected content URI(s), names/MIME/size, and authoritative reference(s) in separate fields. RESULT_SELECTED is 1, RESULT_CANCELLED is 0, and RESULT_FALLBACK is -1; grants are carried by Android flags/ClipData only for access URIs. Multi-item selection can return a generated JSPF document and per-item references.
- **Errors:** Cancellation is distinct from fallback. Fallback results may include EXTRA_FALLBACK_TO_SOURCES and EXTRA_FAILURE_MESSAGE. Source resolution APIs return null when callers choose nullable methods, or throw SourceResolutionException with provider details from strict methods.
- **Compatibility:** The constants and request/result model live in libinfiniselect's FileSelectionContract. No explicit protocol version or range is present. Exact library revisions in consumers' source locks do not by themselves determine runtime interoperability.
- **Fallback:** When the optional Infinifolder selector is absent/unavailable, the library offers Android SAF and app-local source choices. Save requests fall back to ACTION_CREATE_DOCUMENT.
- **Negotiation:** None. Consumers should check provider/app availability, preserve URI grants, and retain local/SAF handling.

### Infinifolder resource-resolution ContentProvider calls

**ID:** `folder-resource-resolution` · **Protocol version:** `null` (unversioned; no compatibility handshake is implemented) · **Implemented by:** [infinifolder](https://github.com/infiniforge/infinifolder), [libinfiniselect](https://github.com/infiniforge/libinfiniselect)

- **Resource identity:** Requests use authoritative source URIs, which may identify local, SAF, remote, archive-member, or filesystem-image resources. Document IDs are opaque. Returned content URIs are access handles and may be short-lived.
- **Request:** libinfiniselect calls ContentResolver.call on com.infinifolder.selection. Methods include RESOLVE_SOURCE, RESOLVE_RELATIVE_ARCHIVE, DESCRIBE_ARCHIVE, and CANONICALIZE_SOURCE. Requests use the STORAGE_* Bundle keys in FileSelectionContract; relative archive resolution takes an authoritative base URI and relative path.
- **Response:** RESOLVE_SOURCE returns a read-granted content URI plus authoritative URI and optional display name, MIME type, and size. Relative resolution returns an authoritative URI. Describe returns metadata. Canonicalize returns an authoritative URI.
- **Errors:** Missing/invalid locations and unsupported operations surface as provider exceptions or unavailable results; strict library calls preserve provider failure details. Callers must not downgrade a permission failure to unrestricted filesystem access.
- **Compatibility:** The method and Bundle constants are defined in libinfiniselect and implemented by Infinifolder. There is no version field, negotiation, or separately declared protocol compatibility range. Library SemVer alone is not a runtime protocol version.
- **Fallback:** libinfiniselect resolves ordinary hierarchical and SAF references locally where supported and uses SAF/local source selection when Folder is absent. Archive traversal and advanced remote/container access require the installed Folder provider; if unavailable, report the provider error and retain the authoritative reference.
- **Negotiation:** None. Clients should check for the provider, catch access/provider errors, and use supported local/SAF fallback paths.

### Android Intent and content-URI handoff

**ID:** `android-uri-handoff` · **Protocol version:** `null` (platform-defined; no InfiniForge protocol version or negotiation is implemented) · **Implemented by:** [infinifolder](https://github.com/infiniforge/infinifolder), [infinimedia](https://github.com/infiniforge/infinimedia), [infiniscribe](https://github.com/infiniforge/infiniscribe), [libinfiniui](https://github.com/infiniforge/libinfiniui), [libinfiniselect](https://github.com/infiniforge/libinfiniselect)

- **Resource identity:** The content URI in data, EXTRA_STREAM, or ClipData identifies the current Android access path and is usable only with the attached URI grant. Where a separate authoritative URI is available, keep that for later reopening.
- **Request:** Applications use Android ACTION_VIEW/send-style intents, MIME types, data/EXTRA_STREAM, ClipData, and read/write/persistable grant flags as appropriate to the operation.
- **Response:** The receiving app opens the granted URI under its own UID and owns its own UI/state. The sender may provide display metadata and an authoritative reference using documented library extras.
- **Errors:** A missing handler, revoked grant, or denied permission must be handled in the sending/receiving app. No app may read another app's private files or assume a URI grant exists.
- **Compatibility:** Standard Android intent and content-provider behavior, with optional InfiniForge extras documented by libinfiniselect. No suite-wide version negotiation is implemented.
- **Fallback:** Keep an in-app local/SAF path when an optional companion app is missing; report access failure instead of treating a URI string as a file path.
- **Negotiation:** None beyond Android package/intent/provider discovery and runtime permission checks.

## Independent component releases and compatibility

Semantic Versioning MAJOR.MINOR.PATCH for independently released applications and libraries where practical. Each repository releases on its own schedule.

- **MAJOR:** Incompatible change to that component's public API or documented behavior.
- **MINOR:** Backward-compatible public functionality addition.
- **PATCH:** Backward-compatible fix.
- **Tags:** Tag each component repository independently as vMAJOR.MINOR.PATCH and attach its GitHub Release to that tag. Do not advance unrelated components to the same version.
- **Release notes:** A release description must identify the component version, exact shared-library versions or pinned source commits used, implemented protocol IDs and versions/status, public API changes, breaking changes, and migration steps.
- **Version evidence:** Build version declarations are not automatically releases. The component repository's Gradle/Cargo/package metadata and dependency lock files remain authoritative for build inputs. released_version is populated only from an observed published release; null means no release was verified on audited_on.
- **Dependency pins:** Do not copy dependency versions or commit pins into this manifest. Existing Gradle/package declarations and dependencies.lock.json (or the source submodule gitlink) are authoritative. Release notes record the versions/commits actually used.
- **Protocol compatibility:** Protocol versions are independent of library versions. Current cross-app contracts listed here have no InfiniForge version field or negotiation; their version is null. Introduce a protocol version only with an explicit contract and compatibility/fallback plan. A library version mismatch alone does not establish protocol incompatibility.

## Finding and changing an API

Start at the component's repository README and API/architecture links in the inventory. Read that component's `AGENTS.md` and `docs/README.md` before editing. Before adding shared behavior, inspect the owning library repository; update that library's public API documentation and dependent application docs when behavior changes. For cross-repository changes, check both the source dependency pin and the implemented protocol contract, and preserve optional-app and permission fallback paths.
