package com.kaede050492.createfaremod.item;

import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class IcCardIssuerBlockItem extends BlockItem {
    public IcCardIssuerBlockItem(Block block, Properties properties) {
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
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card_issuer.purpose");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card_issuer.usage");
        TooltipHelper.addDetail(tooltip, "tooltip.createfaremod.ic_card_issuer.configuration");
        TooltipHelper.addStatus(tooltip, "tooltip.createfaremod.ic_card_issuer.status");
    }
}
