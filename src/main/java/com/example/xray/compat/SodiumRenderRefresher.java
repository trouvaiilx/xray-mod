package com.example.xray.compat;

import com.example.xray.XrayClient;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;

/**
 * Deliberately kept in its own tiny class, separate from XrayCommand. XrayCommand must stay
 * loadable even when Sodium isn't installed, and the JVM resolves a class's referenced types
 * lazily on first real use -- so as long as nothing outside an "if Sodium is loaded" branch
 * ever references SodiumWorldRenderer, this class is simply never linked/loaded when Sodium
 * is absent, and nothing breaks. Putting the reference in its own file makes that guarantee
 * obvious rather than relying on subtle bytecode-verification behavior inside a bigger class.
 */
public final class SodiumRenderRefresher {

    private SodiumRenderRefresher() {
    }

    /**
     * Schedules a normal async rebuild for every chunk section within the player's current
     * render distance -- the same background mechanism Sodium already uses every time a
     * block is broken or placed nearby, just applied to every loaded section instead of one.
     *
     * This is necessary because Sodium only rebuilds a section's mesh when something marks
     * that specific section dirty; flipping our own XrayState flag doesn't do that on its
     * own, so without this call only sections that happen to get touched some other way
     * (breaking a block near them) would ever pick up the new toggle state.
     *
     * Deliberately NOT using SodiumWorldRenderer#reload() here: that call tears down and
     * reinitializes the whole renderer synchronously (discarding all GPU buffers and
     * recreating the render dispatcher), which is what caused the noticeable frame-drop/hitch
     * on toggle. scheduleRebuildForChunks just queues normal background rebuild tasks -- the
     * same lightweight path used during ordinary mining -- so it stays smooth.
     */
    public static void refreshAllChunks() {
        try {
            var renderer = SodiumWorldRenderer.instanceNullable();
            var client = Minecraft.getInstance();
            var player = client.player;

            if (renderer == null || player == null) {
                return;
            }

            int renderDistance = client.options.getEffectiveRenderDistance();
            int cx = player.blockPosition().getX() >> 4;
            int cz = player.blockPosition().getZ() >> 4;

            // X/Z: loaded chunks are already a (2*renderDistance+1) square around the player,
            // same shape scheduleRebuildForChunks is given here, so there's no real waste on
            // those two axes -- scheduleRebuild() no-ops for the handful of corner sections
            // outside Sodium's actual (roughly cylindrical) view volume anyway.
            //
            // Y is different: it was previously also expanded by +-renderDistance, i.e. up to
            // 65 section-Y values scheduled at a high render distance, when the world itself
            // (level.getMinSectionY()..getMaxSectionY()) only ever has on the order of ~24
            // sections to begin with, regardless of render distance. Every out-of-bounds Y
            // still costs a real scheduleRebuildForChunks loop iteration and a map lookup
            // before it no-ops, so clamping to the level's actual section range (the same
            // bound Sodium's own RenderSectionManager/OcclusionCuller iterate over) cuts that
            // wasted work instead of just calling it harmless.
            int minSectionY = client.level.getMinSectionY();
            int maxSectionY = client.level.getMaxSectionY();

            renderer.scheduleRebuildForChunks(
                    cx - renderDistance, minSectionY, cz - renderDistance,
                    cx + renderDistance, maxSectionY, cz + renderDistance,
                    true // playerChanged: prioritizes sections near the player, same as vanilla block-edit rebuilds do
            );
        } catch (Throwable t) {
            // Never let a render-refresh failure take down the command itself -- worst case,
            // the toggle silently doesn't force a repaint and behaves like before this fix.
            XrayClient.LOGGER.warn("Failed to force a Sodium chunk refresh after X-ray toggle", t);
        }
    }
}

