package io.github.trouvaiilx.xray.server;

import io.github.trouvaiilx.xray.network.XrayOptInPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated server entrypoint for X-Ray Mod.
 * Handles server-side opt-in handshake packets and server commands.
 */
public final class XrayServer implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("xray-server");

    @Override
    public void onInitializeServer() {
        XrayServerConfig.load();
        XrayServerCommand.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            boolean allowed = XrayServerConfig.isXrayAllowed();
            ServerPlayNetworking.send(player, new XrayOptInPayload(allowed));
            LOGGER.info("[X-Ray Server] Sent opt-in handshake to player {} (allowed={})", player.getScoreboardName(), allowed);
        });

        LOGGER.info("[X-Ray Server] Initialized. Current X-ray permission setting: {}",
                XrayServerConfig.isXrayAllowed() ? "ALLOWED" : "DENIED");
    }

    public static void broadcastConsent(MinecraftServer server, boolean allowed) {
        XrayOptInPayload payload = new XrayOptInPayload(allowed);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
