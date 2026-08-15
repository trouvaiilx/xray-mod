package io.github.trouvaiilx.xray;

import io.github.trouvaiilx.xray.compat.SodiumCompat;
import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the runtime X-ray toggle state and delegates block whitelist queries to XrayConfig.
 */
public final class XrayState {
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private XrayState() {
    }

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

    public static boolean isWhitelisted(Block block) {
        return XrayConfig.isWhitelisted(block);
    }
}
