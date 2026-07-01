package com.kaede050492.createfaremod.item;

import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.data.CardLedgerSavedData;
import com.kaede050492.createfaremod.menu.IcCardMenu;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class IcCardItem extends Item {
    public IcCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FareGateBlockEntity fareGate)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            fareGate.queueCardUse(player, context.getHand());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            IcCardMenu.open(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<net.minecraft.network.chat.Component> tooltip,
            TooltipFlag flag
    ) {
        if (!TooltipHelper.showDetails(tooltip)) {
            return;
        }
        CardLedgerSavedData.DisplayData data = CardLedgerSavedData.readDisplayData(stack);
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card.purpose");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card.usage");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card.configuration");
        if (!data.issued()) {
            TooltipHelper.addStatus(tooltip, "tooltip.createfaremod.ic_card.unissued");
        } else if (data.entered()) {
            TooltipHelper.addStatus(
                    tooltip,
                    "tooltip.createfaremod.ic_card.entered",
                    data.entryStationName(),
                    data.lineId()
            );
        } else {
            TooltipHelper.addStatus(tooltip, "tooltip.createfaremod.ic_card.ready");
        }
    }
}
