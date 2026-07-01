package com.kaede050492.createfaremod.client.screen;

import com.kaede050492.createfaremod.block.entity.IcCardIssuerBlockEntity;
import com.kaede050492.createfaremod.menu.IcCardIssuerMenu;
import com.kaede050492.createfaremod.network.IssueIcCardPayload;
import com.kaede050492.createfaremod.network.SaveIssuerFeePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IcCardIssuerScreen extends AbstractContainerScreen<IcCardIssuerMenu> {
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 132;

    private EditBox feeField;
    private String validationMessage = "";

    public IcCardIssuerScreen(IcCardIssuerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        feeField = new EditBox(
                font,
                leftPos + 92,
                topPos + 42,
                122,
                18,
                Component.translatable("screen.createfaremod.ic_card_issuer.issue_fee")
        );
        feeField.setMaxLength(10);
        feeField.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        feeField.setValue(Long.toString(menu.getIssueFee()));
        feeField.setEditable(menu.canConfigure());
        addRenderableWidget(feeField);
        addRenderableWidget(Button.builder(
                Component.translatable("screen.createfaremod.ic_card_issuer.issue"),
                button -> PacketDistributor.sendToServer(new IssueIcCardPayload(menu.getIssuerPos()))
        ).bounds(leftPos + 12, topPos + 76, 96, 20).build());
        if (menu.canConfigure()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.createfaremod.ic_card_issuer.save_fee"),
                    button -> saveFee()
            ).bounds(leftPos + 122, topPos + 76, 96, 20).build());
        }
    }

    private void saveFee() {
        try {
            long fee = Long.parseLong(feeField.getValue());
            if (fee < 0L || fee > IcCardIssuerBlockEntity.MAX_ISSUE_FEE) {
                throw new NumberFormatException();
            }
            PacketDistributor.sendToServer(new SaveIssuerFeePayload(menu.getIssuerPos(), fee));
            validationMessage = "";
        } catch (NumberFormatException exception) {
            validationMessage = Component.translatable(
                    "message.createfaremod.invalid_issue_fee"
            ).getString();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xE0121820);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + PANEL_WIDTH - 4, topPos + PANEL_HEIGHT - 4, 0xF0ECEFF2);
        graphics.fill(leftPos + 8, topPos + 28, leftPos + PANEL_WIDTH - 8, topPos + 30, 0xFF2478B8);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0x202830, false);
        graphics.drawString(
                font,
                Component.translatable("screen.createfaremod.ic_card_issuer.issue_fee"),
                12,
                46,
                0x404850,
                false
        );
        if (!validationMessage.isEmpty()) {
            graphics.drawString(font, validationMessage, 12, 108, 0xCC2222, false);
        }
    }
}
