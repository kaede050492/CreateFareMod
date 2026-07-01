package com.kaede050492.createfaremod.item;

import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.gate.GateConfiguration;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class FareGateConfigCardItem extends Item {
    private static final String CONFIGURATION_TAG = "createfaremod.gateConfiguration";

    public FareGateConfigCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof FareGateBlockEntity fareGate)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.createfaremod.config_card_creative_only"), true);
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            GateConfiguration configuration = readConfiguration(stack);
            if (configuration == null) {
                player.displayClientMessage(Component.translatable("message.createfaremod.config_card_empty"), true);
                return InteractionResult.FAIL;
            }
            fareGate.applyConfiguration(configuration);
            player.displayClientMessage(Component.translatable(
                    "message.createfaremod.config_pasted", configuration.stationName()
            ), true);
        } else {
            writeConfiguration(stack, fareGate.getConfiguration());
            player.displayClientMessage(Component.translatable(
                    "message.createfaremod.config_copied", fareGate.getConfiguration().stationName()
            ), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        if (!TooltipHelper.showDetails(tooltip)) {
            return;
        }
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.config_card.purpose");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.config_card.usage");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.config_card.configuration");
        GateConfiguration configuration = readConfiguration(stack);
        if (configuration != null && !configuration.stationName().isBlank()) {
            TooltipHelper.addStatus(
                    tooltip,
                    "tooltip.createfaremod.config_card.station",
                    configuration.stationName()
            );
        } else {
            TooltipHelper.addStatus(tooltip, "tooltip.createfaremod.config_card.empty");
        }
    }

    private static void writeConfiguration(ItemStack stack, GateConfiguration configuration) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(CONFIGURATION_TAG, configuration.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static GateConfiguration readConfiguration(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(CONFIGURATION_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return GateConfiguration.load(root.getCompound(CONFIGURATION_TAG));
    }
}
