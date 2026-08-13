package io.github.trouvaiilx.xray;

import io.github.trouvaiilx.xray.command.XrayCommand;
import io.github.trouvaiilx.xray.config.XrayConfig;
import io.github.trouvaiilx.xray.keybind.XrayKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.trouvaiilx.xray.compat.SodiumRenderRefresher;
import net.minecraft.client.Minecraft;

public final class XrayClient implements ClientModInitializer {
    public static final String MOD_ID = "xray-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger("xray-mod");

    private static int lastChunkX = Integer.MIN_VALUE;
    private static int lastChunkZ = Integer.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        XrayConfig.load();
        XrayCommand.register();
        XrayKeybinds.register();
        io.github.trouvaiilx.xray.render.XrayPeekRenderer.register();

        // Drains XrayConfig's dirty flag at most once per tick (20/sec), and tracks player movement
        // across chunk section boundaries to update X-ray render distance dynamically as the player moves.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            XrayConfig.tick();

            if (client.player != null && client.level != null) {
                int cx = client.player.getBlockX() >> 4;
                int cz = client.player.getBlockZ() >> 4;
                XrayConfig.updatePlayerChunkPos(cx, cz);

                if (XrayState.isEnabled()) {
                    if (cx != lastChunkX || cz != lastChunkZ) {
                        lastChunkX = cx;
                        lastChunkZ = cz;

                        // Only force-refresh chunk meshes when moving across chunk boundaries IF
                        // X-ray distance is smaller than full effective render distance. If X-ray distance
                        // covers the whole view range, newly loaded chunks are meshed by Sodium automatically.
                        int effectiveDistance = client.options.getEffectiveRenderDistance();
                        if (XrayConfig.getRenderDistance() < effectiveDistance) {
                            refreshRender();
                        }
                    }
                }
            }
        });

        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            LOGGER.info("Sodium detected — X-ray render hooks active.");
        } else {
            LOGGER.warn("Sodium not found — X-ray is installed but has no rendering backend. "
                    + "The /trigger xray command will run but nothing will visually change "
                    + "until a Sodium-compatible render path is added.");
        }
    }

    public static void refreshRender() {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            return;
        }
        SodiumRenderRefresher.refreshAllChunks();
    }
}

