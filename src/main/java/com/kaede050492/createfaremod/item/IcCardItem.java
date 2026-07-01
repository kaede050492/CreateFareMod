package com.kaede050492.createfaremod.item;

import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

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
}
