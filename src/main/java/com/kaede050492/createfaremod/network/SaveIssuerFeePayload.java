package com.kaede050492.createfaremod.network;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.block.entity.IcCardIssuerBlockEntity;
import com.kaede050492.createfaremod.menu.IcCardIssuerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveIssuerFeePayload(BlockPos issuerPos, long issueFee) implements CustomPacketPayload {
    public static final Type<SaveIssuerFeePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, "save_issuer_fee")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveIssuerFeePayload> STREAM_CODEC =
            StreamCodec.of(SaveIssuerFeePayload::encode, SaveIssuerFeePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SaveIssuerFeePayload payload) {
        buffer.writeBlockPos(payload.issuerPos);
        buffer.writeVarLong(payload.issueFee);
    }

    private static SaveIssuerFeePayload decode(RegistryFriendlyByteBuf buffer) {
        return new SaveIssuerFeePayload(buffer.readBlockPos(), buffer.readVarLong());
    }

    public static void handle(SaveIssuerFeePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof IcCardIssuerMenu menu
                && menu.getIssuerPos().equals(payload.issuerPos)
                && player.level().getBlockEntity(payload.issuerPos) instanceof IcCardIssuerBlockEntity issuer) {
            issuer.updateIssueFee(player, payload.issueFee);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
