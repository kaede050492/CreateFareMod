package com.kaede050492.createfaremod.menu;

import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.gate.GateConfiguration;
import com.kaede050492.createfaremod.registry.ModBlocks;
import com.kaede050492.createfaremod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class FareGateMenu extends AbstractContainerMenu {
    private final BlockPos gatePos;
    private final GateConfiguration configuration;

    public FareGateMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), readConfiguration(buffer));
    }

    public FareGateMenu(
            int containerId,
            Inventory inventory,
            BlockPos gatePos,
            GateConfiguration configuration
    ) {
        super(ModMenus.FARE_GATE.get(), containerId);
        this.gatePos = gatePos.immutable();
        this.configuration = configuration;
    }

    public static void open(ServerPlayer player, FareGateBlockEntity fareGate) {
        GateConfiguration configuration = fareGate.getConfiguration();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.createfaremod.fare_gate.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                return new FareGateMenu(id, inventory, fareGate.getBlockPos(), configuration);
            }
        }, buffer -> {
            buffer.writeBlockPos(fareGate.getBlockPos());
            buffer.writeNbt(configuration.save());
        });
    }

    public BlockPos getGatePos() {
        return gatePos;
    }

    public GateConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                gatePos.getX() + 0.5D,
                gatePos.getY() + 0.5D,
                gatePos.getZ() + 0.5D
        ) <= 64.0D && player.level().getBlockState(gatePos).is(ModBlocks.FARE_GATE.get());
    }

    private static GateConfiguration readConfiguration(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? GateConfiguration.empty() : GateConfiguration.load(tag);
    }
}
