package com.kaede050492.createfaremod.client;

import com.kaede050492.createfaremod.client.screen.PaymentFailureScreen;
import com.kaede050492.createfaremod.network.PaymentFailurePayload;
import com.kaede050492.createfaremod.network.TransactionResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handlePaymentFailure(PaymentFailurePayload payload) {
        Minecraft.getInstance().setScreen(new PaymentFailureScreen(payload.reason(), payload.fare()));
    }

    public static void handleTransactionResult(TransactionResultPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.success() && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(payload.message()), true);
        } else if (!payload.success()) {
            minecraft.setScreen(new PaymentFailureScreen(payload.message(), payload.fare()));
        }
    }
}
