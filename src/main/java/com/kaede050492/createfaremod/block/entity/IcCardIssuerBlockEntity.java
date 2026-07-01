package com.kaede050492.createfaremod.block.entity;

import com.kaede050492.createfaremod.currency.CurrencyAdapter;
import com.kaede050492.createfaremod.currency.LightmansCurrencyAdapter;
import com.kaede050492.createfaremod.data.CardLedgerSavedData;
import com.kaede050492.createfaremod.menu.IcCardIssuerMenu;
import com.kaede050492.createfaremod.network.PaymentFailurePayload;
import com.kaede050492.createfaremod.network.TransactionResultPayload;
import com.kaede050492.createfaremod.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IcCardIssuerBlockEntity extends BlockEntity {
    public static final long MAX_ISSUE_FEE = 1_000_000_000L;

    private UUID ownerUuid;
    private long issueFee;

    public IcCardIssuerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IC_CARD_ISSUER.get(), pos, state);
    }

    public void openMenu(ServerPlayer player) {
        IcCardIssuerMenu.open(player, this);
    }

    public void issueCard(ServerPlayer player) {
        if (!isUsableBy(player)) {
            return;
        }
        if (player.getInventory().getFreeSlot() < 0) {
            player.displayClientMessage(
                    Component.translatable("message.createfaremod.issuer_inventory_full"),
                    true
            );
            return;
        }
        CurrencyAdapter.PaymentResult payment =
                LightmansCurrencyAdapter.INSTANCE.debit(player, issueFee);
        if (!payment.success()) {
            PacketDistributor.sendToPlayer(player, new PaymentFailurePayload(payment.message(), issueFee));
            return;
        }
        ItemStack card = CardLedgerSavedData.get(player.getServer()).issueCard(player);
        if (!player.getInventory().add(card)) {
            player.drop(card, false);
        }
        PacketDistributor.sendToPlayer(
                player,
                new TransactionResultPayload(
                        true,
                        Component.translatable("message.createfaremod.card_issued").getString(),
                        issueFee
                )
        );
    }

    public void updateIssueFee(ServerPlayer player, long requestedFee) {
        if (!isUsableBy(player) || !canConfigure(player)) {
            player.displayClientMessage(
                    Component.translatable("message.createfaremod.not_issuer_owner"),
                    true
            );
            return;
        }
        if (requestedFee < 0L || requestedFee > MAX_ISSUE_FEE) {
            player.displayClientMessage(
                    Component.translatable("message.createfaremod.invalid_issue_fee"),
                    true
            );
            return;
        }
        issueFee = requestedFee;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
        player.displayClientMessage(
                Component.translatable("message.createfaremod.issue_fee_saved", issueFee),
                true
        );
    }

    public boolean canConfigure(ServerPlayer player) {
        return ownerUuid == null
                || ownerUuid.equals(player.getUUID())
                || player.hasPermissions(2);
    }

    public boolean isUsableBy(ServerPlayer player) {
        return level != null
                && !level.isClientSide
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D
                && player.level().getBlockEntity(worldPosition) == this;
    }

    public void setOwnerIfAbsent(UUID owner) {
        if (ownerUuid == null && owner != null) {
            ownerUuid = owner;
            setChanged();
        }
    }

    public long getIssueFee() {
        return issueFee;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerUuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        issueFee = Math.max(0L, Math.min(MAX_ISSUE_FEE, tag.getLong("issueFee")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUuid != null) {
            tag.putUUID("owner", ownerUuid);
        }
        tag.putLong("issueFee", issueFee);
    }
}
