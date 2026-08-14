package io.github.trouvaiilx.xray;

import io.github.trouvaiilx.xray.command.XrayCommand;
import io.github.trouvaiilx.xray.compat.SodiumCompat;
import io.github.trouvaiilx.xray.config.XrayConfig;
import io.github.trouvaiilx.xray.core.state.XrayDistanceChecker;
import io.github.trouvaiilx.xray.core.state.XrayState;
import io.github.trouvaiilx.xray.keybind.XrayKeybinds;
import io.github.trouvaiilx.xray.render.XrayPeekRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client entrypoint for X-Ray Mod.
 */
public final class XrayClient implements ClientModInitializer {
    public static final String MOD_ID = "xray-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger("xray-mod");

    private static int lastChunkX = Integer.MIN_VALUE;
    private static int lastChunkZ = Integer.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        io.github.trouvaiilx.xray.core.state.XrayServerConsent.register();
        XrayConfig.load();
        XrayCommand.register();
        XrayKeybinds.register();
        XrayPeekRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            XrayConfig.tick();

            if (client.player != null && client.level != null) {
                int cx = client.player.getBlockX() >> 4;
                int cz = client.player.getBlockZ() >> 4;
                XrayDistanceChecker.updatePlayerChunkPos(cx, cz);

                if (XrayState.isEnabled()) {
                    if (cx != lastChunkX || cz != lastChunkZ) {
                        lastChunkX = cx;
                        lastChunkZ = cz;

                        int effectiveDistance = client.options.getEffectiveRenderDistance();
                        if (XrayState.getRenderDistance() < effectiveDistance) {
                            refreshRender();
                        }
                    }
                }
            }
        });

        if (SodiumCompat.isAvailable()) {
            LOGGER.info("Sodium detected — X-ray render hooks active.");
        } else {
            LOGGER.warn("Sodium not found — X-ray is installed but rendering backend is unavailable.");
        }
    }

    public static void refreshRender() {
        SodiumCompat.refreshChunks();
    }
}
