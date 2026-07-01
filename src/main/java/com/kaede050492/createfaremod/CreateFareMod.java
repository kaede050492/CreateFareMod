package com.kaede050492.createfaremod;

import com.kaede050492.createfaremod.network.ModNetworking;
import com.kaede050492.createfaremod.registry.ModBlockEntities;
import com.kaede050492.createfaremod.registry.ModBlocks;
import com.kaede050492.createfaremod.registry.ModCreativeTabs;
import com.kaede050492.createfaremod.registry.ModItems;
import com.kaede050492.createfaremod.registry.ModMenus;
import com.kaede050492.createfaremod.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CreateFareMod.MOD_ID)
public final class CreateFareMod {
    public static final String MOD_ID = "createfaremod";

    public CreateFareMod(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
    }
}
