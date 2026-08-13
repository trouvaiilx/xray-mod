package com.example.xray;

import com.example.xray.command.XrayCommand;
import com.example.xray.config.XrayConfig;
import com.example.xray.keybind.XrayKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.xray.compat.SodiumRenderRefresher;
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

        // Drains XrayConfig's dirty flag at most once per tick (20/sec), and tracks player movement
        // across chunk section boundaries to update X-ray render distance dynamically as the player moves.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            XrayConfig.tick();

            if (XrayState.isEnabled() && client.player != null && client.level != null) {
                int cx = client.player.blockPosition().getX() >> 4;
                int cz = client.player.blockPosition().getZ() >> 4;
                if (cx != lastChunkX || cz != lastChunkZ) {
                    lastChunkX = cx;
                    lastChunkZ = cz;
                    refreshRender();
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
        if (!XrayState.isEnabled()) {
            return;
        }
        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            SodiumRenderRefresher.refreshAllChunks();
        } else {
            var client = Minecraft.getInstance();
            if (client.levelRenderer != null) {
                client.levelRenderer.resetLevelRenderData();
            }
        }
    }
}

