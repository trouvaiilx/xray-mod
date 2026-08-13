package com.example.xray;

import com.example.xray.command.XrayCommand;
import com.example.xray.config.XrayConfig;
import com.example.xray.keybind.XrayKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XrayClient implements ClientModInitializer {
    public static final String MOD_ID = "xray-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger("xray-mod");

    @Override
    public void onInitializeClient() {
        XrayConfig.load();
        XrayCommand.register();
        XrayKeybinds.register();

        // Drains XrayConfig's dirty flag at most once per tick (20/sec), so dragging the
        // render-distance slider doesn't hammer disk every frame -- see XrayConfig#tick().
        ClientTickEvents.END_CLIENT_TICK.register(client -> XrayConfig.tick());

        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            LOGGER.info("Sodium detected — X-ray render hooks active.");
        } else {
            LOGGER.warn("Sodium not found — X-ray is installed but has no rendering backend. "
                    + "The /trigger xray command will run but nothing will visually change "
                    + "until a Sodium-compatible render path is added.");
        }
    }
}

