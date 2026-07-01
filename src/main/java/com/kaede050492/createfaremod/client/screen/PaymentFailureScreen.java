package com.kaede050492.createfaremod.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PaymentFailureScreen extends Screen {
    private final String reason;
    private final long fare;

    public PaymentFailureScreen(String reason, long fare) {
        super(Component.translatable("screen.createfaremod.payment_failure.title"));
        this.reason = reason;
        this.fare = fare;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(width / 2 - 50, height / 2 + 35, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 45, 0xFF5555);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.createfaremod.payment_failure.fare", fare),
                width / 2,
                height / 2 - 15,
                0xFFFFFF
        );
        graphics.drawCenteredString(font, reason, width / 2, height / 2 + 5, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
