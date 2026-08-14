package io.github.trouvaiilx.xray.core.state;

import io.github.trouvaiilx.xray.XrayClient;
import io.github.trouvaiilx.xray.network.XrayOptInPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages server consent and opt-in status in compliance with Modrinth Content Rules (Rule 3.3.a).
 *
 * In Singleplayer: Always permitted (the player is local host).
 * In Multiplayer: Strictly gated behind server-side opt-in (XrayOptInPayload handshake).
 */
public final class XrayServerConsent {

    private static final AtomicBoolean SERVER_OPTED_IN = new AtomicBoolean(false);

    private XrayServerConsent() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(XrayOptInPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                boolean allowed = payload.allowed();
                setServerOptedIn(allowed);
                if (allowed) {
                    XrayClient.LOGGER.info("[X-Ray] Server opt-in verified: X-ray features permitted by server.");
                } else {
                    XrayClient.LOGGER.warn("[X-Ray] Server opt-in denied or revoked: X-ray features disabled.");
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isLocalServer()) {
                SERVER_OPTED_IN.set(true);
            } else {
                SERVER_OPTED_IN.set(false);
                if (io.github.trouvaiilx.xray.XrayState.isEnabled()) {
                    io.github.trouvaiilx.xray.XrayState.setEnabled(false);
                    XrayClient.refreshRender();
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SERVER_OPTED_IN.set(false);
            if (io.github.trouvaiilx.xray.XrayState.isEnabled()) {
                io.github.trouvaiilx.xray.XrayState.setEnabled(false);
            }
        });
    }

    public static boolean isServerOptedIn() {
        return SERVER_OPTED_IN.get();
    }

    public static void setServerOptedIn(boolean value) {
        boolean previous = SERVER_OPTED_IN.getAndSet(value);
        if (previous && !value) {
            if (io.github.trouvaiilx.xray.XrayState.isEnabled()) {
                io.github.trouvaiilx.xray.XrayState.setEnabled(false);
                XrayClient.refreshRender();
            }
        }
    }

    /**
     * Evaluates if X-ray operations are currently permitted.
     *
     * @return true if in singleplayer or if the connected server has sent opt-in authorization.
     */
    public static boolean isAllowed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        if (mc.isLocalServer()) {
            return true;
        }
        if (mc.getConnection() == null) {
            return false;
        }
        return SERVER_OPTED_IN.get();
    }

    /**
     * Checks if currently in singleplayer / local game.
     */
    public static boolean isSingleplayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.isLocalServer();
    }
}
