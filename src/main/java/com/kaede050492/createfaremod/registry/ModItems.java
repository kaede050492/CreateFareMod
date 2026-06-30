package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.item.IcCardItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateFareMod.MOD_ID);

    public static final DeferredItem<BlockItem> FARE_GATE = ITEMS.register(
            "fare_gate",
            () -> new BlockItem(ModBlocks.FARE_GATE.get(), new Item.Properties())
    );

    public static final DeferredItem<IcCardItem> IC_CARD = ITEMS.register(
            "ic_card",
            () -> new IcCardItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
