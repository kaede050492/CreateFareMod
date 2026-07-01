package com.kaede050492.createfaremod.network;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PaymentFailurePayload(String reason, long fare) implements CustomPacketPayload {
    public static final Type<PaymentFailurePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, "payment_failure")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PaymentFailurePayload> STREAM_CODEC =
            StreamCodec.of(PaymentFailurePayload::encode, PaymentFailurePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PaymentFailurePayload payload) {
        buffer.writeUtf(payload.reason, 256);
        buffer.writeVarLong(payload.fare);
    }

    private static PaymentFailurePayload decode(RegistryFriendlyByteBuf buffer) {
        return new PaymentFailurePayload(buffer.readUtf(256), buffer.readVarLong());
    }

    public static void handle(PaymentFailurePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handlePaymentFailure(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
