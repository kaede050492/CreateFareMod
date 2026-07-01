package com.kaede050492.createfaremod.client.screen;

import com.kaede050492.createfaremod.gate.GateConfiguration;
import com.kaede050492.createfaremod.gate.GateMode;
import com.kaede050492.createfaremod.menu.FareGateMenu;
import com.kaede050492.createfaremod.network.SaveGateConfigurationPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FareGateScreen extends AbstractContainerScreen<FareGateMenu> {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 270;

    private EditBox stationId;
    private EditBox stationName;
    private EditBox lineId;
    private EditBox accountId;
    private EditBox fareTableId;
    private Button modeButton;
    private Button maintenanceButton;
    private GateMode gateMode;
    private boolean maintenanceMode;
    private String validationMessage = "";

    public FareGateScreen(FareGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
        gateMode = menu.getConfiguration().gateMode();
        maintenanceMode = menu.getConfiguration().maintenanceMode();
    }

    @Override
    protected void init() {
        super.init();
        GateConfiguration configuration = menu.getConfiguration();
        int fieldX = leftPos + 96;
        int fieldWidth = 172;
        stationId = addField(fieldX, topPos + 28, fieldWidth, configuration.stationId(), 32);
        stationName = addField(fieldX, topPos + 52, fieldWidth, configuration.stationName(), 64);
        lineId = addField(fieldX, topPos + 76, fieldWidth, configuration.lineId(), 32);
        accountId = addField(fieldX, topPos + 100, fieldWidth, configuration.accountId(), 80);
        fareTableId = addField(fieldX, topPos + 172, fieldWidth, configuration.fareTableId(), 32);

        modeButton = addRenderableWidget(Button.builder(
                modeText(),
                button -> cycleMode()
        ).bounds(fieldX, topPos + 124, fieldWidth, 20).build());
        maintenanceButton = addRenderableWidget(Button.builder(
                maintenanceText(),
                button -> toggleMaintenance()
        ).bounds(fieldX, topPos + 148, fieldWidth, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.createfaremod.fare_gate.save"),
                button -> save()
        ).bounds(leftPos + PANEL_WIDTH - 78, topPos + PANEL_HEIGHT - 28, 66, 20).build());
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 18, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        return addRenderableWidget(field);
    }

    private void cycleMode() {
        GateMode[] values = GateMode.values();
        gateMode = values[(gateMode.ordinal() + 1) % values.length];
        modeButton.setMessage(modeText());
    }

    private Component modeText() {
        return Component.translatable("gate_mode.createfaremod." + gateMode.name().toLowerCase());
    }

    private void toggleMaintenance() {
        maintenanceMode = !maintenanceMode;
        maintenanceButton.setMessage(maintenanceText());
    }

    private Component maintenanceText() {
        return Component.translatable(maintenanceMode
                ? "screen.createfaremod.fare_gate.maintenance_on"
                : "screen.createfaremod.fare_gate.maintenance_off");
    }

    private void save() {
        GateConfiguration configuration = new GateConfiguration(
                stationId.getValue(),
                stationName.getValue(),
                lineId.getValue(),
                accountId.getValue(),
                menu.getConfiguration().accountName(),
                gateMode,
                fareTableId.getValue(),
                menu.getConfiguration().fareTable(),
                menu.getConfiguration().ownerUuid(),
                maintenanceMode
        );
        validationMessage = configuration.validate().orElse("");
        if (!validationMessage.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new SaveGateConfigurationPayload(menu.getGatePos(), configuration));
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xE0101010);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + PANEL_WIDTH - 4, topPos + PANEL_HEIGHT - 4, 0xE0E6E6E6);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0x202020, false);
        drawLabel(graphics, "screen.createfaremod.fare_gate.station_id", 30);
        drawLabel(graphics, "screen.createfaremod.fare_gate.station_name", 54);
        drawLabel(graphics, "screen.createfaremod.fare_gate.line", 78);
        drawLabel(graphics, "screen.createfaremod.fare_gate.account", 102);
        drawLabel(graphics, "screen.createfaremod.fare_gate.mode", 126);
        drawLabel(graphics, "screen.createfaremod.fare_gate.maintenance", 150);
        drawLabel(graphics, "screen.createfaremod.fare_gate.fare_table", 174);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.createfaremod.fare_gate.account_name",
                        menu.getConfiguration().accountName()
                ),
                96,
                202,
                0x555555,
                false
        );
        if (!validationMessage.isEmpty()) {
            graphics.drawString(font, validationMessage, 12, 244, 0xCC2222, false);
        }
    }

    private void drawLabel(GuiGraphics graphics, String key, int y) {
        graphics.drawString(font, Component.translatable(key), 12, y, 0x303030, false);
    }
}
