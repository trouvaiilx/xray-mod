# xray-mod

Client-side Fabric mod: toggleable X-ray with a Sodium (mc26.2-0.9.1-fabric) render backend.
Toggle in-game with `/trigger xray` (client-only command, no server/datapack needed).

## What's actually implemented

- `XrayState` — the on/off flag + ore whitelist (edit the block list directly in that file for now).
- `XrayCommand` — registers `/trigger xray` and `/trigger xray <true|false>` as a **client-only**
  Brigadier command via Fabric API. Doesn't touch the server.
- `BlockRendererMixin` — cancels Sodium's per-block mesh emission for anything not in the
  whitelist, while X-ray is on. This is what makes stone/dirt/etc. actually stop rendering.
- `AbstractBlockRenderContextMixin` — forces whitelisted ore blocks to render on every face
  regardless of what's touching them, so ore embedded in hidden stone doesn't get culled away.
- `SodiumMixinPlugin` — gates both mixins above so the mod doesn't crash if Sodium isn't
  installed; it just becomes a no-op (you'll see a warning in the log).

Both mixins were written against Sodium's actual current source (CaffeineMC/sodium, `dev`
branch, commit `27bbd7f`, 2026-08-07) — not guessed from memory. Exact files referenced:
- `common/.../render/chunk/compile/pipeline/BlockRenderer.java`
- `common/.../render/model/AbstractBlockRenderContext.java`

## What's NOT implemented yet (be aware before you call this "done")

- **No vanilla (non-Sodium) fallback.** Without Sodium, the command runs but nothing visually
  changes. If you want X-ray to work for players without Sodium too, that's a second,
  separate rendering path (hooking vanilla's own chunk builder) — meaningfully more code,
  ask if you want it scaffolded.
- **No config file / no keybind** — whitelist is a hardcoded `Set<Block>` in `XrayState`,
  edit it directly for now.
- **Not tested against a live Sodium build.** I wrote these mixins by reading Sodium's actual
  source, which gives high confidence the method signatures are right, but I have not run
  this in a real dev-client + Sodium combo — you need to do that verification step (see below).

## Setup

You need, locally:
- **JDK 25** — Minecraft 26.x requires it (Minecraft 26.1 raised the minimum from 21 to 25).
  Get Temurin/Microsoft build of OpenJDK 25.
- **IntelliJ IDEA** with the "Minecraft Development" plugin (for Mixin support), or any IDE +
  command-line Gradle.
- A premium Minecraft account, to launch the dev client.

Steps:

```bash
# from inside this folder
./gradlew idea      # or ./gradlew eclipse, or just open build.gradle in IntelliJ directly
```

If you don't have a Gradle wrapper yet (this scaffold doesn't ship gradle-wrapper.jar — binary
files don't travel well through this chat), run once, using any locally installed Gradle 9.x:

```bash
gradle wrapper --gradle-version 9.5.1
```

Then commit the generated `gradle/wrapper/` folder so future clones don't need a system Gradle.

## Build & run

```bash
./gradlew runClient   # launches a real dev client with your mod + Sodium loaded
./gradlew build       # jar ends up in build/libs/
```

## First things to verify once you have it running

1. **Does it load at all?** Check the log for `Sodium detected — X-ray render hooks active.`
   If you see the "Sodium not found" warning instead, your `sodium_version` in
   `gradle.properties` isn't resolving — check the Modrinth maven coordinate.
2. **Do the mixins apply?** Look for any `Mixin apply failed` / `ERROR` lines mentioning
   `BlockRendererMixin` or `AbstractBlockRenderContextMixin` at startup. If Sodium has
   shipped a newer version since Aug 2026 with a changed `BlockRenderer.renderModel` or
   `AbstractBlockRenderContext.isFaceCulled` signature, this is where it'll surface —
   re-check those two files against whatever Sodium version you're actually running.
3. **Does `/trigger xray` do anything?** Stand in a cave, run it, confirm stone disappears
   and ore stays visible from all sides.
4. **Sanity-check collision**: with X-ray on, walk into a wall that's now invisible — you
   should still bump into it. If you fall through, something is (incorrectly) touching
   collision, not just rendering — none of the code here should do that, so that'd be a bug.

## Updating the ore whitelist

Edit the `static { ... }` block in `XrayState.java`. Any block registered in
`BuiltInRegistries.BLOCK` works — modded ores too, once the mod that adds them is loaded.
