# Contributing to X-Ray Mod

Thank you for your interest in contributing to **X-Ray Mod**! We welcome contributions including bug reports, feature requests, documentation improvements, and code contributions.

---

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Project Architecture](#project-architecture)
- [Updating for New Minecraft Releases](#updating-for-new-minecraft-releases)
- [Style & Guidelines](#style--guidelines)

---

## Code of Conduct

Please maintain a respectful, constructive, and friendly environment when participating in issues, discussions, and pull requests.

---

## How Can I Contribute?

### Reporting Bugs

Before submitting a bug report, please check existing issues to ensure it hasn't already been reported.

When creating a bug report, please include:
- **Minecraft Version**: (e.g. `26.2`)
- **Fabric Loader Version**: (e.g. `0.19.3`)
- **Sodium Version**: (e.g. `mc26.2-0.9.1-fabric`)
- **Exact Steps to Reproduce**: Detailed description of how the issue occurs.
- **Log Files**: Relevant snippets from `.minecraft/logs/latest.log` or crash reports.

### Suggesting Enhancements

Enhancement suggestions are welcome! When opening a feature request:
- Explain **why** the feature would be useful.
- Provide examples of expected behavior or mockups for GUI changes.

### Pull Requests

1. **Fork** the repository and create a new branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Make your changes** adhering to the project's coding standards.
3. **Test thoroughly** using `.\gradlew.bat runClient` to ensure Sodium rendering, entity culling, and GUI interactions work properly.
4. **Submit a Pull Request** targeting the `main` branch with a clear description of your changes.

---

## Development Setup

### Prerequisites
- **JDK 25** (Required for Minecraft 26.1+)
- **Git**

### Build Commands

Cloning and building the project locally:

```powershell
# Clone repository
git clone https://github.com/trouvaiilx/xray-mod.git
cd xray-mod

# Build mod JAR
.\gradlew.bat build

# Launch local Minecraft client for testing
.\gradlew.bat runClient
```

The compiled mod JAR will be located in `build/libs/xray-mod-<version>+<mc_version>.jar`.

---

## Project Architecture

The codebase is organized under `src/main/java/io/github/trouvaiilx/xray/`:

| Package / File | Purpose |
| --- | --- |
| `XrayClient.java` | Main client mod entrypoint, keybind registration, and command initialization. |
| `XrayState.java` | Centralized runtime toggle state, whitelist management, and preset configurations. |
| `config/` | Persistent JSON configuration handling (`xray-mod.json`), sliders, and presets. |
| `gui/` | Custom settings menu (`XrayConfigScreen`), interactive block grid (`BlockGridWidget`), and sliders. |
| `render/` | High-performance, allocation-free Peek Mode wireframe renderer (`XrayPeekRenderer`). |
| `util/` | Entity container classification (`ContainerEntityClassifier`) and lightmap utility helpers. |
| `compat/` | Integration hooks for Sodium's chunk rendering pipeline and fullbright logic. |
| `mixin/` | Mixin classes injecting into Sodium chunk meshing, BlockEntityRenderDispatcher, and EntityRenderDispatcher. |

---

## Updating for New Minecraft Releases

If you are contributing an update for a new Minecraft, Sodium, or Fabric version, please refer to the detailed [Updating Guide](docs/UPDATING.md) for step-by-step instructions.

---

## Style & Guidelines

- **Java Version**: Code must target Java 25 compatibility.
- **Formatting**: Maintain consistent indentation (4 spaces) and standard Java code conventions.
- **Sodium Guard**: Ensure all X-Ray features check for Sodium availability to prevent crash regressions when Sodium is absent.
- **Allocation-Free Hot Path**: Keep spatial render scans in `render/` and mixins free of per-frame object allocations.
