package com.kaede050492.createfaremod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SaveGateConfigurationPayload.TYPE,
                SaveGateConfigurationPayload.STREAM_CODEC,
                SaveGateConfigurationPayload::handle
        );
        registrar.playToClient(
                PaymentFailurePayload.TYPE,
                PaymentFailurePayload.STREAM_CODEC,
                PaymentFailurePayload::handle
        );
    }
}
