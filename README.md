# xray-mod

Client-side Fabric mod for **Minecraft 26.2**: toggleable X-ray featuring an in-game configuration GUI, Sodium (mc26.2-0.9.1-fabric) rendering hooks, and a vanilla rendering fallback.

---

## Features

- **In-Game Settings GUI**: Press **Right Shift** (or configure in Options > Controls) to open the interactive settings menu.
- **Commands**: `/xray toggle`, `/xray on`, `/xray off` with full tab-completion (plus `/trigger xray [true|false]` as a legacy alias).
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
- **Sodium & Vanilla Backends**: Advanced Sodium 0.9.1+ mixin hooks (occlusion graph patching, block skipping, fluid face culling, and fullbright lighting) with an automatic fallback for vanilla level renderer reloads.
- **Persistence**: Auto-saves configuration to `config/xray-mod.json` with write-then-atomic-rename safety.

---

## Controls & Commands

| Feature | Input / Command | Description |
| --- | --- | --- |
| **Toggle X-Ray** | `/xray` / `/xray toggle` / `/xray on` / `/xray off` | Turns X-ray on or off with immediate chunk re-meshing |
| **Open Menu** | **Right Shift** | Opens the interactive settings GUI |
| **Legacy Alias** | `/trigger xray [true|false]` | Client-side command alias for scoreboard trigger compatibility |

---

## Architecture & Mixin Highlights

1. **`XrayConfigScreen`**: Custom 26.2 GUI using `extractRenderState` and `AbstractWidget` rendering, housing the search box, category carousel, block grid, distance slider, fullbright toggle, fluid protection toggle, preset button, and close handler.
2. **`BlockGridWidget`**: Custom 18x18px cell grid widget with hover tooltip rendering, mouse click/scroll handlers, and stable whitelist-first sorting.
3. **`XrayConfig`**: Thread-safe configuration manager backing `config/xray-mod.json` with atomic fields (`AtomicInteger`, `AtomicBoolean`, `AtomicReference<Set<Block>>`) for fast worker-thread reads during chunk compilation.
4. **`XrayClient`**: Mod initializer and client tick handler tracking player chunk coordinate changes (`cx != lastChunkX || cz != lastChunkZ`).
5. **`SodiumRenderRefresher`**: Schedules background section rebuild tasks using Sodium's `scheduleRebuildForChunks` or vanilla's `resetLevelRenderData()`.
6. **`BlockRendererMixin`**: Cancels block model meshing for non-whitelisted blocks (unconditionally) and whitelisted blocks beyond X-ray distance.
7. **`ChunkBuilderMeshingTaskMixin`**: Directs Sodium's section occlusion graph (`DirectionalVisGraph`) to treat hidden blocks as non-opaque, resolving occlusion culling glitches and frustum mouse-flicker.
8. **`DefaultFluidRendererMixin`**: Injects into `DefaultFluidRenderer.render` to cancel un-whitelisted fluids (water/lava) and handles exposed fluid faces and fullbright fluid lighting.
9. **`AbstractBlockRenderContextMixin`**: Overrides face culling and ambient occlusion for whitelisted blocks.

---

## Build Requirements

- **Minecraft**: 26.2
- **Fabric Loader**: 0.19.3+
- **Java JDK**: 25 (Required for Minecraft 26.1+)
- **Gradle**: 9.x (Wrapper included)

### Building

Ensure `JAVA_HOME` is pointed to a JDK 25 installation:

```bash
./gradlew build
```

The compiled mod JAR will be output to `build/libs/xray-mod-0.1.0.jar`.
