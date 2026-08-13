package com.example.xray.config;

import com.example.xray.XrayClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * All persisted X-ray settings, plus the render-thread-facing reads of them.
 *
 * Reads (isFullbright / getRenderDistance / isWhitelisted / isWithinXrayDistance) happen from
 * Sodium's chunk-build WORKER threads, same as the rest of this mod's hot path. Writes only
 * ever happen from the main client thread (the config screen, or a command). Every mutable
 * field is therefore a plain Atomic* or an AtomicReference swapped wholesale (copy-on-write --
 * build a new collection, then publish it in one set() call) rather than mutated in place, so
 * a worker thread reading mid-update always sees either the old state or the new state, never
 * a half-built one. This is the same reasoning XrayState (the older, now-delegating single-
 * flag version of this class) already documented for its ENABLED field.
 */
public final class XrayConfig {
    public static final int MIN_RENDER_DISTANCE = 2;
    public static final int MAX_RENDER_DISTANCE = 32;
    private static final int DEFAULT_RENDER_DISTANCE = 8;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final AtomicInteger RENDER_DISTANCE = new AtomicInteger(DEFAULT_RENDER_DISTANCE);
    private static final AtomicBoolean FULLBRIGHT = new AtomicBoolean(true);
    private static final AtomicBoolean ALWAYS_SHOW_FLUIDS = new AtomicBoolean(true);
    private static final AtomicReference<String> ACTIVE_PRESET = new AtomicReference<>(XrayPresets.DEFAULT);

    // Resolved, render-thread-fast view (see XrayState's doc comment on why
    // ReferenceOpenHashSet rather than java.util.HashSet). Rebuilt wholesale on every change.
    private static final AtomicReference<Set<Block>> WHITELIST =
            new AtomicReference<>(new ReferenceOpenHashSet<>());

    // The raw ids, kept alongside WHITELIST so an id for a modded block that isn't currently
    // loaded (e.g. that mod was temporarily removed) isn't silently dropped from the saved
    // config -- it stays in here (and gets written back to disk) even though it can't be
    // resolved to a Block, and reappears in WHITELIST automatically once that mod is back.
    private static final AtomicReference<Set<String>> WHITELIST_IDS =
            new AtomicReference<>(new LinkedHashSet<>());

    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private static volatile int playerChunkX = 0;
    private static volatile int playerChunkZ = 0;
    private static volatile boolean playerPosValid = false;

    private XrayConfig() {
    }

    public static void updatePlayerChunkPos(int cx, int cz) {
        playerChunkX = cx;
        playerChunkZ = cz;
        playerPosValid = true;
    }

    // ---- lifecycle ----

    /** Called once from XrayClient#onInitializeClient. */
    public static void load() {
        Path path = configPath();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    applyLoaded(data);
                    LOADED.set(true);
                    return;
                }
            } catch (IOException | JsonParseException e) {
                XrayClient.LOGGER.warn("Failed to read xray-mod config, falling back to the Default preset", e);
            }
        }
        // First run, or a corrupt/missing file: start from the Default preset and write it out
        // immediately, so a fresh install has a real config file on disk right away rather than
        // only after the user's first change.
        RENDER_DISTANCE.set(DEFAULT_RENDER_DISTANCE);
        FULLBRIGHT.set(true);
        ALWAYS_SHOW_FLUIDS.set(true);
        applyPresetInternal(XrayPresets.DEFAULT);
        LOADED.set(true);
        saveNow();
    }

    private static void applyLoaded(Data data) {
        RENDER_DISTANCE.set(clampDistance(data.renderDistance));
        FULLBRIGHT.set(data.fullbright);
        ALWAYS_SHOW_FLUIDS.set(data.alwaysShowFluids);
        ACTIVE_PRESET.set(data.activePreset != null ? data.activePreset : XrayPresets.CUSTOM);
        publishWhitelist(data.whitelist != null ? data.whitelist : XrayPresets.blockIds(XrayPresets.DEFAULT));
    }

    /** Called once a client tick (see XrayClient); writes at most once per tick, not per frame. */
    public static void tick() {
        if (DIRTY.compareAndSet(true, false)) {
            saveNow();
        }
    }

    // ---- render-thread hot-path reads ----

    public static boolean isFullbright() {
        return FULLBRIGHT.get();
    }

    public static boolean isAlwaysShowFluids() {
        return ALWAYS_SHOW_FLUIDS.get();
    }

    public static boolean isFluidBlock(Block block) {
        return block == net.minecraft.world.level.block.Blocks.WATER
                || block == net.minecraft.world.level.block.Blocks.LAVA;
    }

    public static int getRenderDistance() {
        return RENDER_DISTANCE.get();
    }

    public static boolean isWhitelisted(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return true;
        }
        return WHITELIST.get().contains(block);
    }

    /**
     * Chebyshev (square) chunk distance from the player -- same shape vanilla's own render
     * distance option uses, so "X-ray render distance" behaves the way players already expect
     * a Minecraft distance slider to behave. Blocks/sections outside this range render exactly
     * as they would with the mod off; only the ones inside it get X-rayed.
     *
     * Reads Minecraft.getInstance().player from a worker thread, same as
     * SodiumRenderRefresher/XrayCommand already do elsewhere in this mod -- a plain field read
     * of a value that only changes a handful of times a second, not a correctness-critical
     * transaction, so no synchronization beyond that field already being effectively volatile
     * (Minecraft assigns a new Player reference on join/respawn, doesn't mutate one in place).
     */
    public static boolean isWithinXrayDistance(int blockX, int blockZ) {
        return isChunkWithinXrayDistance(blockX >> 4, blockZ >> 4);
    }

    public static boolean isChunkWithinXrayDistance(int chunkX, int chunkZ) {
        if (!playerPosValid) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return true; // fail open -- never silently hide things because of a null player
            }
            playerChunkX = player.getBlockX() >> 4;
            playerChunkZ = player.getBlockZ() >> 4;
            playerPosValid = true;
        }
        int px = playerChunkX;
        int pz = playerChunkZ;
        int distance = RENDER_DISTANCE.get();
        return Math.max(Math.abs(chunkX - px), Math.abs(chunkZ - pz)) <= distance;
    }

    // ---- GUI-facing writes ----

    public static void setRenderDistance(int chunks) {
        int clamped = clampDistance(chunks);
        if (RENDER_DISTANCE.getAndSet(clamped) != clamped) {
            markDirty(); // dragged live -- debounced, see #tick()
            XrayClient.refreshRender();
        }
    }

    public static void setFullbright(boolean value) {
        if (FULLBRIGHT.getAndSet(value) != value) {
            saveNow(); // discrete toggle, not a drag -- write immediately
            XrayClient.refreshRender();
        }
    }

    public static void setAlwaysShowFluids(boolean value) {
        if (ALWAYS_SHOW_FLUIDS.getAndSet(value) != value) {
            saveNow();
            XrayClient.refreshRender();
        }
    }

    public static Set<String> getWhitelistIds() {
        return WHITELIST_IDS.get();
    }

    public static boolean isWhitelistedId(String blockId) {
        return WHITELIST_IDS.get().contains(blockId);
    }

    public static void addToWhitelist(Block block) {
        String id = idOf(block);
        Set<String> next = new LinkedHashSet<>(WHITELIST_IDS.get());
        if (next.add(id)) {
            ACTIVE_PRESET.set(XrayPresets.CUSTOM);
            publishWhitelist(next);
            saveNow();
            XrayClient.refreshRender();
        }
    }

    public static void removeFromWhitelist(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return; // blocked by always-show-fluids toggle
        }
        String id = idOf(block);
        Set<String> next = new LinkedHashSet<>(WHITELIST_IDS.get());
        if (next.remove(id)) {
            ACTIVE_PRESET.set(XrayPresets.CUSTOM);
            publishWhitelist(next);
            saveNow();
            XrayClient.refreshRender();
        }
    }

    public static void toggleWhitelist(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return; // blocked by always-show-fluids toggle
        }
        if (isWhitelisted(block)) {
            removeFromWhitelist(block);
        } else {
            addToWhitelist(block);
        }
    }

    public static String getActivePreset() {
        return ACTIVE_PRESET.get();
    }

    public static void applyPreset(String presetName) {
        applyPresetInternal(presetName);
        saveNow();
        XrayClient.refreshRender();
    }

    private static void applyPresetInternal(String presetName) {
        ACTIVE_PRESET.set(presetName);
        publishWhitelist(XrayPresets.blockIds(presetName));
    }

    private static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static void publishWhitelist(Set<String> ids) {
        Set<String> idsCopy = new LinkedHashSet<>(ids);
        Set<Block> resolved = new ReferenceOpenHashSet<>(idsCopy.size());
        for (String id : idsCopy) {
            Identifier rl = Identifier.tryParse(id);
            if (rl == null) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(rl);
            // A null/AIR result means this id isn't resolvable in the CURRENTLY loaded
            // registry (typically: a modded block from a mod that isn't installed this
            // session). We keep the id in WHITELIST_IDS (so it round-trips through save/load
            // and reappears if that mod comes back) but simply can't put an unresolvable
            // Block into the fast lookup set the render thread uses.
            if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                resolved.add(block);
            }
        }
        WHITELIST_IDS.set(idsCopy);
        WHITELIST.set(resolved);
    }

    private static int clampDistance(int value) {
        return Math.max(MIN_RENDER_DISTANCE, Math.min(MAX_RENDER_DISTANCE, value));
    }

    // ---- persistence ----

    private static void markDirty() {
        DIRTY.set(true);
    }

    private static void saveNow() {
        if (!LOADED.get()) {
            return; // never overwrite an on-disk config with in-memory defaults before load() ran
        }
        Data data = new Data();
        data.renderDistance = RENDER_DISTANCE.get();
        data.fullbright = FULLBRIGHT.get();
        data.alwaysShowFluids = ALWAYS_SHOW_FLUIDS.get();
        data.activePreset = ACTIVE_PRESET.get();
        data.whitelist = WHITELIST_IDS.get();

        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            // Write-then-atomic-rename so a crash/kill mid-write can never leave a truncated,
            // unparsable config file behind -- the old file stays valid until the new one is
            // fully flushed. This is what "should survive... normal game shutdowns" means in
            // practice, not just "we call save() often enough."
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileSystemException e) {
                // ATOMIC_MOVE isn't guaranteed on every filesystem (e.g. some network drives) --
                // fall back to a plain (non-atomic, but still correct in the common case) move.
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            XrayClient.LOGGER.warn("Failed to save xray-mod config", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("xray-mod.json");
    }

    private static final class Data {
        int renderDistance = DEFAULT_RENDER_DISTANCE;
        boolean fullbright = true;
        boolean alwaysShowFluids = true;
        String activePreset = XrayPresets.DEFAULT;
        Set<String> whitelist = new LinkedHashSet<>();
    }
}
