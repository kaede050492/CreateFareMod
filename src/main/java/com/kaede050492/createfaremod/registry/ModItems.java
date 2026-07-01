package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.item.FareGateConfigCardItem;
import com.kaede050492.createfaremod.item.FareGateBlockItem;
import com.kaede050492.createfaremod.item.IcCardItem;
import com.kaede050492.createfaremod.item.IcCardIssuerBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateFareMod.MOD_ID);

    public static final DeferredItem<FareGateBlockItem> FARE_GATE = ITEMS.register(
            "fare_gate",
            () -> new FareGateBlockItem(ModBlocks.FARE_GATE.get(), new Item.Properties())
    );

    public static final DeferredItem<IcCardIssuerBlockItem> IC_CARD_ISSUER = ITEMS.register(
            "ic_card_issuer",
            () -> new IcCardIssuerBlockItem(ModBlocks.IC_CARD_ISSUER.get(), new Item.Properties())
    );

    public static final DeferredItem<IcCardItem> IC_CARD = ITEMS.register(
            "ic_card",
            () -> new IcCardItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<FareGateConfigCardItem> CONFIG_CARD = ITEMS.register(
            "config_card",
            () -> new FareGateConfigCardItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
