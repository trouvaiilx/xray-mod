package io.github.trouvaiilx.xray.core.state;

import io.github.trouvaiilx.xray.XrayClient;
import io.github.trouvaiilx.xray.compat.SodiumCompat;
import io.github.trouvaiilx.xray.core.config.XrayConfigData;
import io.github.trouvaiilx.xray.core.config.XrayConfigManager;
import io.github.trouvaiilx.xray.core.model.XrayPresets;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe runtime state manager. Holds active X-ray toggles, whitelist collections,
 * and configuration properties.
 */
public final class XrayState {

    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private static final AtomicInteger RENDER_DISTANCE = new AtomicInteger(XrayConfigData.DEFAULT_RENDER_DISTANCE);
    private static final AtomicBoolean FULLBRIGHT = new AtomicBoolean(true);
    private static final AtomicBoolean ALWAYS_SHOW_FLUIDS = new AtomicBoolean(true);
    private static final AtomicBoolean PEEK_ENABLED = new AtomicBoolean(true);
    private static final AtomicInteger PEEK_RADIUS = new AtomicInteger(XrayConfigData.DEFAULT_PEEK_RADIUS);
    private static final AtomicInteger PEEK_OPACITY = new AtomicInteger(XrayConfigData.DEFAULT_PEEK_OPACITY);
    private static final AtomicInteger PEEK_THICKNESS = new AtomicInteger(Float.floatToRawIntBits(XrayConfigData.DEFAULT_PEEK_THICKNESS));
    private static final AtomicInteger PEEK_COLOR = new AtomicInteger(XrayConfigData.DEFAULT_PEEK_COLOR);

    private static final AtomicReference<String> ACTIVE_PRESET = new AtomicReference<>(XrayPresets.DEFAULT);
    private static final AtomicReference<Set<Block>> WHITELIST = new AtomicReference<>(new ReferenceOpenHashSet<>());
    private static final AtomicReference<Set<String>> WHITELIST_IDS = new AtomicReference<>(new LinkedHashSet<>());

    private XrayState() {
    }

    public static void init() {
        XrayConfigData data = XrayConfigManager.loadConfig();
        applyLoaded(data);
        if (!XrayConfigManager.isLoaded()) {
            XrayConfigManager.saveNow(exportConfigData());
        }
    }

    public static void applyLoaded(XrayConfigData data) {
        RENDER_DISTANCE.set(clampDistance(data.renderDistance));
        FULLBRIGHT.set(data.fullbright);
        ALWAYS_SHOW_FLUIDS.set(data.alwaysShowFluids);
        PEEK_ENABLED.set(data.peekEnabled);
        PEEK_RADIUS.set(clampPeekRadius(data.peekRadius));
        PEEK_OPACITY.set(clampPeekOpacity(data.peekOpacity));
        PEEK_THICKNESS.set(Float.floatToRawIntBits(clampPeekThickness(data.peekThickness > 0.0F ? data.peekThickness : XrayConfigData.DEFAULT_PEEK_THICKNESS)));
        PEEK_COLOR.set(data.peekColor != 0 ? data.peekColor : XrayConfigData.DEFAULT_PEEK_COLOR);
        ACTIVE_PRESET.set(data.activePreset != null ? data.activePreset : XrayPresets.CUSTOM);
        publishWhitelist(data.whitelist != null ? data.whitelist : XrayPresets.blockIds(XrayPresets.DEFAULT));
    }

    public static XrayConfigData exportConfigData() {
        XrayConfigData data = new XrayConfigData();
        data.renderDistance = RENDER_DISTANCE.get();
        data.fullbright = FULLBRIGHT.get();
        data.alwaysShowFluids = ALWAYS_SHOW_FLUIDS.get();
        data.peekEnabled = PEEK_ENABLED.get();
        data.peekRadius = PEEK_RADIUS.get();
        data.peekOpacity = PEEK_OPACITY.get();
        data.peekThickness = getPeekThickness();
        data.peekColor = PEEK_COLOR.get();
        data.activePreset = ACTIVE_PRESET.get();
        data.whitelist = WHITELIST_IDS.get();
        return data;
    }

    // ---- Runtime State Getters/Setters ----

    public static boolean isEnabled() {
        if (!SodiumCompat.isAvailable()) {
            return false;
        }
        return ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
    }

    public static boolean toggle() {
        boolean newValue = !ENABLED.get();
        ENABLED.set(newValue);
        return newValue;
    }

    public static boolean isFullbright() {
        return FULLBRIGHT.get();
    }

    public static void setFullbright(boolean value) {
        if (FULLBRIGHT.getAndSet(value) != value) {
            XrayConfigManager.saveNow(exportConfigData());
            XrayClient.refreshRender();
        }
    }

    public static boolean isPeekEnabled() {
        return PEEK_ENABLED.get();
    }

    public static void setPeekEnabled(boolean value) {
        if (PEEK_ENABLED.getAndSet(value) != value) {
            XrayConfigManager.saveNow(exportConfigData());
        }
    }

    public static int getPeekRadius() {
        return PEEK_RADIUS.get();
    }

    public static void setPeekRadius(int radius) {
        int clamped = clampPeekRadius(radius);
        if (PEEK_RADIUS.getAndSet(clamped) != clamped) {
            XrayConfigManager.markDirty();
        }
    }

    public static int getPeekOpacity() {
        return PEEK_OPACITY.get();
    }

    public static void setPeekOpacity(int opacity) {
        int clamped = clampPeekOpacity(opacity);
        if (PEEK_OPACITY.getAndSet(clamped) != clamped) {
            XrayConfigManager.markDirty();
        }
    }

    public static float getPeekThickness() {
        return Float.intBitsToFloat(PEEK_THICKNESS.get());
    }

    public static void setPeekThickness(float thickness) {
        float clamped = clampPeekThickness(thickness);
        int bits = Float.floatToRawIntBits(clamped);
        if (PEEK_THICKNESS.getAndSet(bits) != bits) {
            XrayConfigManager.markDirty();
        }
    }

    public static int getPeekColor() {
        return PEEK_COLOR.get();
    }

    public static void setPeekColor(int color) {
        if (PEEK_COLOR.getAndSet(color) != color) {
            XrayConfigManager.saveNow(exportConfigData());
        }
    }

    public static boolean isAlwaysShowFluids() {
        return ALWAYS_SHOW_FLUIDS.get();
    }

    public static void setAlwaysShowFluids(boolean value) {
        if (ALWAYS_SHOW_FLUIDS.getAndSet(value) != value) {
            XrayConfigManager.saveNow(exportConfigData());
            XrayClient.refreshRender();
        }
    }

    public static int getRenderDistance() {
        return RENDER_DISTANCE.get();
    }

    public static void setRenderDistance(int chunks) {
        int clamped = clampDistance(chunks);
        if (RENDER_DISTANCE.getAndSet(clamped) != clamped) {
            XrayConfigManager.markDirty();
            XrayClient.refreshRender();
        }
    }

    public static boolean isFluidBlock(Block block) {
        return block == Blocks.WATER || block == Blocks.LAVA;
    }

    public static boolean isWhitelisted(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return true;
        }
        return WHITELIST.get().contains(block);
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
            XrayConfigManager.saveNow(exportConfigData());
            XrayClient.refreshRender();
        }
    }

    public static void removeFromWhitelist(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return;
        }
        String id = idOf(block);
        Set<String> next = new LinkedHashSet<>(WHITELIST_IDS.get());
        if (next.remove(id)) {
            ACTIVE_PRESET.set(XrayPresets.CUSTOM);
            publishWhitelist(next);
            XrayConfigManager.saveNow(exportConfigData());
            XrayClient.refreshRender();
        }
    }

    public static void toggleWhitelist(Block block) {
        if (ALWAYS_SHOW_FLUIDS.get() && isFluidBlock(block)) {
            return;
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
        ACTIVE_PRESET.set(presetName);
        publishWhitelist(XrayPresets.blockIds(presetName));
        XrayConfigManager.saveNow(exportConfigData());
        XrayClient.refreshRender();
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
            if (block != null && block != Blocks.AIR) {
                resolved.add(block);
            }
        }
        WHITELIST_IDS.set(idsCopy);
        WHITELIST.set(resolved);
    }

    private static int clampDistance(int value) {
        return Math.max(XrayConfigData.MIN_RENDER_DISTANCE, Math.min(XrayConfigData.MAX_RENDER_DISTANCE, value));
    }

    private static int clampPeekRadius(int value) {
        return Math.max(XrayConfigData.MIN_PEEK_RADIUS, Math.min(XrayConfigData.MAX_PEEK_RADIUS, value));
    }

    private static int clampPeekOpacity(int value) {
        return Math.max(XrayConfigData.MIN_PEEK_OPACITY, Math.min(XrayConfigData.MAX_PEEK_OPACITY, value));
    }

    private static float clampPeekThickness(float value) {
        return Math.max(XrayConfigData.MIN_PEEK_THICKNESS, Math.min(XrayConfigData.MAX_PEEK_THICKNESS, value));
    }
}
