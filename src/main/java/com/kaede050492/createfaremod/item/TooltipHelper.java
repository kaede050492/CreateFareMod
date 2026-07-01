package com.kaede050492.createfaremod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static boolean showDetails(List<Component> tooltip) {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createfaremod.hold_shift")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        return true;
    }

    public static void addDetail(List<Component> tooltip, String translationKey, Object... values) {
        tooltip.add(Component.translatable(translationKey, values).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void addStatus(List<Component> tooltip, String translationKey, Object... values) {
        tooltip.add(Component.translatable(translationKey, values).withStyle(ChatFormatting.AQUA));
    }
}
