# Infinikey IME agent guidance

Infinikey is Infiniforge's Android keyboard input method. It is an independent application with its own IME service, UI, settings, assets, and build. The keyboard must be installable and usable without any other Infiniforge app.

## Independence and integration

- Build this repository from its own checkout with its documented Android toolchain and declared dependencies. Do not rely on sibling application source, outputs, or suite-level build paths.
- Keep the Gradle project independent and do not make another Infiniforge app a runtime or build prerequisite.
- Use Android's input method, permission, settings, and storage contracts. Request only permissions needed for the implemented feature and preserve the system's IME binding protections.
- Any optional launch, media, or file actions must work through public Android APIs and remain optional when companion apps are missing.
- Keep layouts, user configuration, and keyboard state owned by Infinikey; preserve its existing compatibility and accessibility behavior.

## Project focus

- Treat programmable keyboard input as the primary purpose. Keep layout descriptors, actions, rendering, and input dispatch consistent with the repository's documented architecture.
- Maintain its standalone build and release documentation. Never require a suite-wide checkout to assemble this app.

## Ecosystem workflow for agents

1. Read `docs/ECOSYSTEM.md` first to understand the component map and ownership boundaries.
2. Read `docs/APPLICATION.md` for this app's intended role and current status. Before changing a public interface, read the repository's architecture and API documentation listed below and inspect compatibility notes.
3. Check the owning shared library repository before implementing behavior that may already exist elsewhere. Do not conclude a capability is unavailable just because its implementation is outside the current repository.
4. Keep application responsibilities, shared library APIs, and other applications' private data/source separate.
5. Before a cross-repository change, check the exact dependency pin and the relevant interoperability contract. Dependency versions and protocol versions are separate; do not infer protocol incompatibility from a library-version mismatch.
6. Update the local documentation index and relevant API/behavior documentation when public behavior changes. `docs/ECOSYSTEM.md` is generated from the central manifest and must not be edited locally.

## Documentation index

- `README.md` — project purpose, prerequisites, and standalone build instructions.
- `AGENTS.md` — repository-specific development rules and this ecosystem workflow.
- `docs/README.md` — local documentation index.
- `docs/APPLICATION.md` — application purpose, responsibilities, implementation status, planned work, and known limits.
- `docs/ECOSYSTEM.md` — generated ecosystem-wide inventory, boundaries, dependencies, versions, and protocols.
- `docs/ARCHITECTURE.md` — public API and architecture documentation; read before changing interfaces or behavior.
- `docs/LAYOUT_DESCRIPTOR_SPEC.md` — public API and architecture documentation; read before changing interfaces or behavior.
- `docs/THEMING_SPEC.md` — public API and architecture documentation; read before changing interfaces or behavior.
- `docs/WYSIWYG_EDITOR_SPEC.md` — public API and architecture documentation; read before changing interfaces or behavior.