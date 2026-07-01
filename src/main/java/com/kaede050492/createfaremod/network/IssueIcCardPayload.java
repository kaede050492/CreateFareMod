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

public record IssueIcCardPayload(BlockPos issuerPos) implements CustomPacketPayload {
    public static final Type<IssueIcCardPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, "issue_ic_card")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, IssueIcCardPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeBlockPos(payload.issuerPos),
                    buffer -> new IssueIcCardPayload(buffer.readBlockPos())
            );

    public static void handle(IssueIcCardPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof IcCardIssuerMenu menu
                && menu.getIssuerPos().equals(payload.issuerPos)
                && player.level().getBlockEntity(payload.issuerPos) instanceof IcCardIssuerBlockEntity issuer) {
            issuer.issueCard(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
