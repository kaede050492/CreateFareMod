package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.menu.FareGateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateFareMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FareGateMenu>> FARE_GATE =
            MENUS.register("fare_gate", () -> IMenuTypeExtension.create(FareGateMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
