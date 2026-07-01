package com.kaede050492.createfaremod.client;

import com.kaede050492.createfaremod.client.screen.PaymentFailureScreen;
import com.kaede050492.createfaremod.network.PaymentFailurePayload;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handlePaymentFailure(PaymentFailurePayload payload) {
        Minecraft.getInstance().setScreen(new PaymentFailureScreen(payload.reason(), payload.fare()));
    }
}
