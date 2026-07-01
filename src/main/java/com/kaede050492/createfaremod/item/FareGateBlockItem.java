package com.kaede050492.createfaremod.item;

import com.kaede050492.createfaremod.gate.GateConfiguration;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public final class FareGateBlockItem extends BlockItem {
    private static final String CONFIGURATION_TAG = "createfaremod.gateConfiguration";

    public FareGateBlockItem(Block block, Properties properties) {
        super(block, properties);
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
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.fare_gate.purpose");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.fare_gate.usage");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.fare_gate.configuration");
        GateConfiguration configuration = readConfiguration(stack);
        if (configuration == null) {
            TooltipHelper.addStatus(tooltip, "tooltip.createfaremod.fare_gate.unconfigured");
            return;
        }
        TooltipHelper.addStatus(
                tooltip,
                "tooltip.createfaremod.fare_gate.station_id",
                configuration.stationId()
        );
        TooltipHelper.addStatus(
                tooltip,
                "tooltip.createfaremod.fare_gate.mode",
                configuration.gateMode().name()
        );
        TooltipHelper.addStatus(
                tooltip,
                "tooltip.createfaremod.fare_gate.account",
                configuration.accountName().isBlank()
                        ? configuration.accountId()
                        : configuration.accountName()
        );
        TooltipHelper.addStatus(
                tooltip,
                configuration.maintenanceMode()
                        ? "tooltip.createfaremod.fare_gate.maintenance"
                        : "tooltip.createfaremod.fare_gate.normal"
        );
    }

    private static GateConfiguration readConfiguration(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(CONFIGURATION_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return GateConfiguration.load(root.getCompound(CONFIGURATION_TAG));
    }
}
