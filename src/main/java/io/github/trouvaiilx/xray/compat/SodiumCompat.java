package io.github.trouvaiilx.xray.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Facade and compatibility layer for Sodium integration.
 * Provides cached mod detection and safe lazy invocation of Sodium render rebuild tasks.
 */
public final class SodiumCompat {

    private static final boolean SODIUM_PRESENT = FabricLoader.getInstance().isModLoaded("sodium");

    private SodiumCompat() {
    }

    /**
     * @return true if Sodium is loaded in the current runtime environment.
     */
    public static boolean isAvailable() {
        return SODIUM_PRESENT;
    }

    /**
     * Triggers an asynchronous chunk re-mesh across the player's view distance if Sodium is loaded.
     */
    public static void refreshChunks() {
        if (SODIUM_PRESENT) {
            SodiumRenderRefresher.refreshAllChunks();
        }
    }
}
