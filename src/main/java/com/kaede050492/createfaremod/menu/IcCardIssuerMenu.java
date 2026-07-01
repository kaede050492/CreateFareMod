package com.kaede050492.createfaremod.menu;

import com.kaede050492.createfaremod.block.entity.IcCardIssuerBlockEntity;
import com.kaede050492.createfaremod.registry.ModBlocks;
import com.kaede050492.createfaremod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class IcCardIssuerMenu extends AbstractContainerMenu {
    private final BlockPos issuerPos;
    private final long issueFee;
    private final boolean canConfigure;

    public IcCardIssuerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                buffer.readBlockPos(),
                buffer.readVarLong(),
                buffer.readBoolean()
        );
    }

    private IcCardIssuerMenu(
            int containerId,
            BlockPos issuerPos,
            long issueFee,
            boolean canConfigure
    ) {
        super(ModMenus.IC_CARD_ISSUER.get(), containerId);
        this.issuerPos = issuerPos.immutable();
        this.issueFee = issueFee;
        this.canConfigure = canConfigure;
    }

    public static void open(ServerPlayer player, IcCardIssuerBlockEntity issuer) {
        long fee = issuer.getIssueFee();
        boolean operator = issuer.canConfigure(player);
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.createfaremod.ic_card_issuer.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                return new IcCardIssuerMenu(id, issuer.getBlockPos(), fee, operator);
            }
        }, buffer -> {
            buffer.writeBlockPos(issuer.getBlockPos());
            buffer.writeVarLong(fee);
            buffer.writeBoolean(operator);
        });
    }

    public BlockPos getIssuerPos() {
        return issuerPos;
    }

    public long getIssueFee() {
        return issueFee;
    }

    public boolean canConfigure() {
        return canConfigure;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                issuerPos.getX() + 0.5D,
                issuerPos.getY() + 0.5D,
                issuerPos.getZ() + 0.5D
        ) <= 64.0D && player.level().getBlockState(issuerPos).is(ModBlocks.IC_CARD_ISSUER.get());
    }
}
