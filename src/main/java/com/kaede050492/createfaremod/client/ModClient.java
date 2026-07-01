package com.kaede050492.createfaremod.client;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.client.screen.FareGateScreen;
import com.kaede050492.createfaremod.client.screen.IcCardScreen;
import com.kaede050492.createfaremod.client.screen.IcCardIssuerScreen;
import com.kaede050492.createfaremod.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreateFareMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    private ModClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FARE_GATE.get(), FareGateScreen::new);
        event.register(ModMenus.IC_CARD.get(), IcCardScreen::new);
        event.register(ModMenus.IC_CARD_ISSUER.get(), IcCardIssuerScreen::new);
    }
}
