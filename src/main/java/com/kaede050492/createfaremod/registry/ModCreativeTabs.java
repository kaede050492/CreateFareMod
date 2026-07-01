package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateFareMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createfaremod.main"))
                    .icon(() -> ModItems.IC_CARD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FARE_GATE.get());
                        output.accept(ModItems.IC_CARD.get());
                        output.accept(ModItems.CONFIG_CARD.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
