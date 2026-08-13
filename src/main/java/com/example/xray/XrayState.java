package com.example.xray;

import com.example.xray.config.XrayConfig;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the runtime X-ray on/off flag. Read from the render thread (mixins) and written from
 * the command thread / config screen -- both of these only ever happen on the client, and the
 * flag is a plain volatile/atomic, so no extra locking is needed for a simple on/off switch.
 *
 * The whitelist, fullbright toggle, and X-ray render distance used to live here too, back when
 * the whitelist was a hardcoded Set<Block> you edited in source. They've since moved to
 * XrayConfig (persisted, GUI-editable, backed by the live block registry) -- this class now
 * just forwards isWhitelisted() there so the mixins didn't all need to change their imports.
 */
public final class XrayState {
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private XrayState() {
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
    }

    public static boolean toggle() {
        // A CAS retry loop here would be unnecessary work, not just unnecessary caution: per
        // this class's own doc comment, ENABLED is only ever WRITTEN from the client command/
        // GUI thread, and both Brigadier and the Screen event loop run on that single thread --
        // so there is never a second writer for a CAS to lose a race against. AtomicBoolean is
        // still the right type (its get()/set() are volatile reads/writes, which is what
        // actually matters: making the new value visible to the render threads that call
        // isEnabled()), just without a pointless retry loop.
        boolean newValue = !ENABLED.get();
        ENABLED.set(newValue);
        return newValue;
    }

    /**
     * True for blocks that should keep rendering (and render on every face) while X-ray is on.
     * Delegates to the persisted, GUI-editable whitelist in XrayConfig.
     */
    public static boolean isWhitelisted(Block block) {
        return XrayConfig.isWhitelisted(block);
    }
}
