# xray-mod

Client-side Fabric mod for **Minecraft 26.2**: toggleable X-ray featuring an in-game configuration GUI and Sodium (mc26.2-0.9.1-fabric) rendering hooks.

---

## Features

- **In-Game Settings GUI**: Press **Right Shift** (or configure in Options > Controls) to open the interactive settings menu.
- **Commands**: `/xray`, `/xray toggle`, `/xray on`, `/xray off` with full tab-completion (plus `/trigger xray [true|false]` as a legacy alias).
- **Search & Category Filters**: Easily filter blocks by category (*All Blocks*, *Ores*, *Nether*, *End*, *Fluids*, *Utility*) or search by block ID and localized name.
- **Interactive Block Grid**:
  - Creative-inventory style icon grid with hover tooltips and green highlights for whitelisted blocks.
  - In the **All Blocks** category, whitelisted blocks automatically sort to the top.
  - Toggling whitelist items automatically updates the preset label to `Preset: Custom` in real time while preserving grid scroll position.
- **Preset System**: Quick-select presets (*Default*, *Ores*, *Fluids*, *Valuables*, or *Custom*).
- **X-Ray Render Distance**:
  - **Non-whitelisted blocks** (stone, dirt, etc.) are always rendered as transparent (hidden) across the entire view distance.
  - **Whitelisted blocks** (ores, chests, fluids) render only within the configured X-ray render distance (2–32 chunks).
  - **Dynamic Movement Updating**: Automatically tracks player chunk boundary crossings (`16`-block steps) to update the X-ray view as you travel across the world.
- **Fullbright Lighting**: Forces fullbright lightmap values and disables ambient occlusion shading for whitelisted blocks and fluids when enabled.
- **Always Fluids Safety Toggle**: A dedicated toggle (`Always Fluids: ON/OFF`) that locks water and lava as whitelisted, preventing accidental removal.
- **Instant Live Update**: Any GUI setting change or menu close immediately re-meshes chunk rendering without requiring an off/on toggle.
- **Sodium Requirement Guard**: Safe fallback check: if Sodium is not installed, executing commands or pressing the keybind safely displays a clear warning message in chat (`Sodium is not installed! X-ray rendering requires Sodium.`) without crashing or corrupting level render data.
- **Persistence**: Auto-saves configuration to `config/xray-mod.json` with write-then-atomic-rename safety.

---

## Controls & Commands

| Feature | Input / Command | Description |
| --- | --- | --- |
| **Toggle X-Ray** | `/xray` / `/xray toggle` / `/xray on` / `/xray off` | Turns X-ray on or off with immediate chunk re-meshing |
| **Open Menu** | **Right Shift** | Opens the interactive settings GUI |
| **Legacy Alias** | `/trigger xray [true|false]` | Client-side command alias for scoreboard trigger compatibility |

---

## Requirements

- **Minecraft**: 26.2
- **Fabric Loader**: 0.19.3+
- **Sodium**: `mc26.2-0.9.1-fabric` (Required for X-ray rendering)
- **Java JDK**: 25 (Required for Minecraft 26.1+)

If Sodium is missing, attempting to toggle X-ray or open the settings menu will print a friendly red notice in chat advising you to install Sodium, preventing any game crash.

---

## Building

Ensure `JAVA_HOME` is pointed to a JDK 25 installation:

```bash
./gradlew build
```

The compiled mod JAR will be output to `build/libs/xray-mod-0.1.0.jar`.
