package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.menu.FareGateMenu;
import com.kaede050492.createfaremod.menu.IcCardMenu;
import com.kaede050492.createfaremod.menu.IcCardIssuerMenu;
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
    public static final DeferredHolder<MenuType<?>, MenuType<IcCardMenu>> IC_CARD =
            MENUS.register("ic_card", () -> IMenuTypeExtension.create(IcCardMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<IcCardIssuerMenu>> IC_CARD_ISSUER =
            MENUS.register("ic_card_issuer", () -> IMenuTypeExtension.create(IcCardIssuerMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
