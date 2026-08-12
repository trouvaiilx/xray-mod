package com.example.xray.compat;

import com.example.xray.XrayClient;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

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
     * Forces every currently-loaded chunk section to rebuild its mesh from scratch --
     * the same call the vanilla F3+A "reload chunks" debug keybind ends up making. This is
     * necessary because Sodium only rebuilds a section's mesh when something marks that
     * specific section dirty (a block edit, a chunk load); flipping our own XrayState flag
     * doesn't do that on its own, so without this call, only sections that get touched for
     * some other reason (breaking a block near them) would ever pick up the new state, and
     * everywhere else would keep showing whatever was true the last time IT rebuilt.
     *
     * This IS a comparatively heavy call -- it discards all built geometry and rebuilds the
     * whole visible world asynchronously, which can cause a brief re-pop-in flash on large
     * render distances. That's an acceptable trade for a manually-triggered toggle command;
     * it would NOT be acceptable to call this every tick or every frame.
     */
    public static void refreshAllChunks() {
        try {
            var renderer = SodiumWorldRenderer.instanceNullable();
            if (renderer != null) {
                renderer.reload();
            }
        } catch (Throwable t) {
            // Never let a render-refresh failure take down the command itself -- worst case,
            // the toggle silently doesn't force a repaint and behaves like before this fix.
            XrayClient.LOGGER.warn("Failed to force a Sodium chunk refresh after X-ray toggle", t);
        }
    }
}
