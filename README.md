<div align="center">
  <img src="icon.png" width="128" height="128" alt="X-Ray Mod Icon" />
  <h1>X-Ray Mod</h1>

  [![Build](https://github.com/trouvaiilx/xray-mod/actions/workflows/build.yml/badge.svg)](https://github.com/trouvaiilx/xray-mod/actions/workflows/build.yml)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Ko-fi](https://img.shields.io/badge/Ko--fi-Support-ff5e5b?logo=ko-fi&logoColor=white)](https://ko-fi.com/trouvaiilx)

  <p>Client-side Fabric mod for <b>Minecraft 26.2</b> by <b><a href="https://github.com/trouvaiilx">trouvaiilx</a></b>: toggleable X-ray featuring an in-game configuration GUI and Sodium (<code>mc26.2-0.9.1-fabric</code>) rendering hooks.</p>
</div>

> **Sodium Required**: This mod **ONLY works when the [Sodium](https://modrinth.com/mod/sodium) mod is installed**. Sodium provides the chunk rendering pipeline that this mod hooks into to hide non-whitelisted blocks and apply fullbright lighting. Without Sodium installed, X-ray rendering will **NOT** function (attempting to use commands or keybinds will display a warning message in chat).

> **Why This Mod Was Created**: After searching everywhere for a modern Fabric X-ray mod, none were available that natively supported Sodium's custom rendering pipeline without issues. This mod was built from the ground up to solve that, hooking directly into Sodium for smooth, glitch-free X-ray rendering!

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

## Screenshots

<div align="center">
  <h3>In-Game Configuration Menu</h3>
  <img src="docs/images/gui.png" alt="In-Game Settings GUI" width="800" />

  <br/><br/>

  <table>
    <tr>
      <td align="center" width="50%">
        <b>Overworld X-Ray</b><br/>
        <img src="docs/images/overworld.png" alt="Overworld X-Ray" width="400" />
      </td>
      <td align="center" width="50%">
        <b>Nether X-Ray</b><br/>
        <img src="docs/images/nether.png" alt="Nether X-Ray" width="400" />
      </td>
    </tr>
  </table>
</div>

---

## Controls & Commands

| Feature | Input / Command | Description |
| --- | --- | --- |
| **Toggle X-Ray** | `/xray` / `/xray toggle` / `/xray on` / `/xray off` | Turns X-ray on or off with immediate chunk re-meshing |
| **Open Menu** | **Right Shift** | Opens the interactive settings GUI |
| **Legacy Alias** | `/trigger xray [true or false]` | Client-side command alias for scoreboard trigger compatibility |

---

## Requirements

- **Sodium**: `mc26.2-0.9.1-fabric` (**STRICTLY REQUIRED** — X-ray rendering only works when Sodium is installed!)
- **Minecraft**: 26.2
- **Fabric Loader**: 0.19.3+
- **Java JDK**: 25 (Required for Minecraft 26.1+)

If Sodium is missing, X-ray visuals will **NOT** render, and attempting to toggle X-ray or open the settings menu will display a warning notice in chat.

---

## Building & Updating

Ensure `JAVA_HOME` is pointed to a JDK 25 installation:

```bash
./gradlew build
```

The compiled mod JAR will be output to `build/libs/xray-mod-<version>+<mc_version>.jar`.

For detailed guidelines on updating the mod for new Minecraft/Sodium versions or contributing changes, see:
- [Updating Guide](docs/UPDATING.md)
- [Contributing Guidelines](CONTRIBUTING.md)

---

## Support

If you enjoy using this mod and would like to support its ongoing development, consider buying me a coffee!

[![Support on Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/trouvaiilx)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
