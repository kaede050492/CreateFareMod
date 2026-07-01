package com.kaede050492.createfaremod.client.screen;

import com.kaede050492.createfaremod.menu.IcCardMenu;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class IcCardScreen extends AbstractContainerScreen<IcCardMenu> {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;

    public IcCardScreen(IcCardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xE0101820);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + PANEL_WIDTH - 4, topPos + PANEL_HEIGHT - 4, 0xF0EEF2F5);
        graphics.fill(leftPos + 10, topPos + 26, leftPos + PANEL_WIDTH - 10, topPos + 28, 0xFF2478B8);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        IcCardMenu.CardView view = menu.getCardView();
        graphics.drawString(font, title, 12, 10, 0x202830, false);
        drawValue(graphics, "screen.createfaremod.ic_card.balance", view.balanceText(), 36);
        drawValue(
                graphics,
                "screen.createfaremod.ic_card.status",
                Component.translatable(view.entered()
                        ? "screen.createfaremod.ic_card.entered"
                        : "screen.createfaremod.ic_card.ready").getString(),
                52
        );
        drawValue(
                graphics,
                "screen.createfaremod.ic_card.entry_station",
                view.entered() ? view.entryStation() : "-",
                68
        );
        drawValue(
                graphics,
                "screen.createfaremod.ic_card.entry_time",
                view.entryTimestamp() > 0L ? TIME_FORMAT.format(Instant.ofEpochMilli(view.entryTimestamp())) : "-",
                84
        );
        drawValue(graphics, "screen.createfaremod.ic_card.line", view.lineId().isBlank() ? "-" : view.lineId(), 100);
        graphics.drawString(
                font,
                Component.translatable("screen.createfaremod.ic_card.history"),
                12,
                122,
                0x202830,
                false
        );
        int y = 138;
        if (view.history().isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("screen.createfaremod.ic_card.no_history"),
                    16,
                    y,
                    0x666666,
                    false
            );
        } else {
            for (IcCardMenu.HistoryEntry entry : view.history()) {
                String route = entry.entryStation().isBlank()
                        ? entry.exitStation()
                        : entry.entryStation() + " \u2192 " + entry.exitStation();
                String text = route + "  " + entry.fare() + " LC  " + friendlyResult(entry.result());
                graphics.drawString(font, text, 16, y, 0x3A3A3A, false);
                y += 13;
            }
        }
    }

    private void drawValue(GuiGraphics graphics, String key, String value, int y) {
        graphics.drawString(font, Component.translatable(key), 12, y, 0x555555, false);
        graphics.drawString(font, value, 112, y, 0x202830, false);
    }

    private static String friendlyResult(String result) {
        return result.endsWith("SUCCESS") ? "Success" : result;
    }
}
