package com.kaede050492.createfaremod.network;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.gate.GateConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveGateConfigurationPayload(
        BlockPos gatePos,
        GateConfiguration configuration
) implements CustomPacketPayload {
    public static final Type<SaveGateConfigurationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, "save_gate_configuration")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveGateConfigurationPayload> STREAM_CODEC =
            StreamCodec.of(SaveGateConfigurationPayload::encode, SaveGateConfigurationPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, SaveGateConfigurationPayload payload) {
        buffer.writeBlockPos(payload.gatePos);
        buffer.writeNbt(payload.configuration.save());
    }

    private static SaveGateConfigurationPayload decode(RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag tag = buffer.readNbt();
        return new SaveGateConfigurationPayload(
                pos, tag == null ? GateConfiguration.empty() : GateConfiguration.load(tag)
        );
    }

    public static void handle(SaveGateConfigurationPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().getBlockEntity(payload.gatePos) instanceof FareGateBlockEntity fareGate) {
            fareGate.updateConfiguration(player, payload.configuration);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
