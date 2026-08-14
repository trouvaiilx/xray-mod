package io.github.trouvaiilx.xray.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C / C2S CustomPacketPayload indicating whether X-ray features are allowed by the server.
 */
public record XrayOptInPayload(boolean allowed) implements CustomPacketPayload {

    public static final String MOD_ID = "xray-mod";

    public static final CustomPacketPayload.Type<XrayOptInPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "opt_in"));

    public static final StreamCodec<FriendlyByteBuf, XrayOptInPayload> CODEC =
            ByteBufCodecs.BOOL.map(XrayOptInPayload::new, XrayOptInPayload::allowed).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
