package com.kaede050492.createfaremod.network;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.client.ClientPayloadHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransactionResultPayload(
        boolean success,
        String message,
        long fare
) implements CustomPacketPayload {
    public static final Type<TransactionResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, "transaction_result")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TransactionResultPayload> STREAM_CODEC =
            StreamCodec.of(TransactionResultPayload::encode, TransactionResultPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, TransactionResultPayload payload) {
        buffer.writeBoolean(payload.success);
        buffer.writeUtf(payload.message, 256);
        buffer.writeVarLong(payload.fare);
    }

    private static TransactionResultPayload decode(RegistryFriendlyByteBuf buffer) {
        return new TransactionResultPayload(
                buffer.readBoolean(),
                buffer.readUtf(256),
                buffer.readVarLong()
        );
    }

    public static void handle(TransactionResultPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleTransactionResult(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
