package io.github.trouvaiilx.xray.config;

import io.github.trouvaiilx.xray.core.config.XrayConfigData;
import io.github.trouvaiilx.xray.core.config.XrayConfigManager;
import io.github.trouvaiilx.xray.core.state.XrayDistanceChecker;
import io.github.trouvaiilx.xray.core.state.XrayState;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/**
 * Compatibility delegation wrapper for XrayState, XrayConfigManager, and XrayDistanceChecker.
 */
public final class XrayConfig {

    public static final int MIN_RENDER_DISTANCE = XrayConfigData.MIN_RENDER_DISTANCE;
    public static final int MAX_RENDER_DISTANCE = XrayConfigData.MAX_RENDER_DISTANCE;
    public static final int MIN_PEEK_RADIUS = XrayConfigData.MIN_PEEK_RADIUS;
    public static final int MAX_PEEK_RADIUS = XrayConfigData.MAX_PEEK_RADIUS;
    public static final int MIN_PEEK_OPACITY = XrayConfigData.MIN_PEEK_OPACITY;
    public static final int MAX_PEEK_OPACITY = XrayConfigData.MAX_PEEK_OPACITY;
    public static final float MIN_PEEK_THICKNESS = XrayConfigData.MIN_PEEK_THICKNESS;
    public static final float MAX_PEEK_THICKNESS = XrayConfigData.MAX_PEEK_THICKNESS;

    private XrayConfig() {
    }

    public static void load() {
        XrayState.init();
    }

    public static void tick() {
        XrayConfigManager.tick();
    }

    public static void updatePlayerChunkPos(int cx, int cz) {
        XrayDistanceChecker.updatePlayerChunkPos(cx, cz);
    }

    public static boolean isFullbright() {
        return XrayState.isFullbright();
    }

    public static void setFullbright(boolean value) {
        XrayState.setFullbright(value);
    }

    public static boolean isPeekEnabled() {
        return XrayState.isPeekEnabled();
    }

    public static void setPeekEnabled(boolean value) {
        XrayState.setPeekEnabled(value);
    }

    public static int getPeekRadius() {
        return XrayState.getPeekRadius();
    }

    public static void setPeekRadius(int radius) {
        XrayState.setPeekRadius(radius);
    }

    public static int getPeekOpacity() {
        return XrayState.getPeekOpacity();
    }

    public static void setPeekOpacity(int opacity) {
        XrayState.setPeekOpacity(opacity);
    }

    public static float getPeekThickness() {
        return XrayState.getPeekThickness();
    }

    public static void setPeekThickness(float thickness) {
        XrayState.setPeekThickness(thickness);
    }

    public static int getPeekColor() {
        return XrayState.getPeekColor();
    }

    public static void setPeekColor(int color) {
        XrayState.setPeekColor(color);
    }

    public static boolean isAlwaysShowFluids() {
        return XrayState.isAlwaysShowFluids();
    }

    public static void setAlwaysShowFluids(boolean value) {
        XrayState.setAlwaysShowFluids(value);
    }

    public static boolean isFluidBlock(Block block) {
        return XrayState.isFluidBlock(block);
    }

    public static int getRenderDistance() {
        return XrayState.getRenderDistance();
    }

    public static void setRenderDistance(int chunks) {
        XrayState.setRenderDistance(chunks);
    }

    public static boolean isWhitelisted(Block block) {
        return XrayState.isWhitelisted(block);
    }

    public static boolean isWithinXrayDistance(int blockX, int blockZ) {
        return XrayDistanceChecker.isWithinXrayDistance(blockX, blockZ);
    }

    public static boolean isChunkWithinXrayDistance(int chunkX, int chunkZ) {
        return XrayDistanceChecker.isChunkWithinXrayDistance(chunkX, chunkZ);
    }

    public static Set<String> getWhitelistIds() {
        return XrayState.getWhitelistIds();
    }

    public static boolean isWhitelistedId(String blockId) {
        return XrayState.isWhitelistedId(blockId);
    }

    public static void addToWhitelist(Block block) {
        XrayState.addToWhitelist(block);
    }

    public static void removeFromWhitelist(Block block) {
        XrayState.removeFromWhitelist(block);
    }

    public static void toggleWhitelist(Block block) {
        XrayState.toggleWhitelist(block);
    }

    public static String getActivePreset() {
        return XrayState.getActivePreset();
    }

    public static void applyPreset(String presetName) {
        XrayState.applyPreset(presetName);
    }
}
