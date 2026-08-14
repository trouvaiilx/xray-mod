package io.github.trouvaiilx.xray.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Common payload registry for client and server networking.
 */
public final class XrayNetworkRegistry {

    private static final java.util.concurrent.atomic.AtomicBoolean REGISTERED = new java.util.concurrent.atomic.AtomicBoolean(false);

    private XrayNetworkRegistry() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            PayloadTypeRegistry.clientboundPlay().register(XrayOptInPayload.TYPE, XrayOptInPayload.CODEC);
            PayloadTypeRegistry.serverboundPlay().register(XrayOptInPayload.TYPE, XrayOptInPayload.CODEC);
        }
    }
}
