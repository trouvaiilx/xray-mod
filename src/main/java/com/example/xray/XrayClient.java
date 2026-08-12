package com.example.xray;

import com.example.xray.command.XrayCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XrayClient implements ClientModInitializer {
    public static final String MOD_ID = "xray-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger("xray-mod");

    @Override
    public void onInitializeClient() {
        XrayCommand.register();

        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            LOGGER.info("Sodium detected — X-ray render hooks active.");
        } else {
            LOGGER.warn("Sodium not found — X-ray is installed but has no rendering backend. "
                    + "The /trigger xray command will run but nothing will visually change "
                    + "until a Sodium-compatible render path is added.");
        }
    }
}
