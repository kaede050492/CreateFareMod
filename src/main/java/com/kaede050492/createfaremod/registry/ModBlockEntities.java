package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.block.entity.IcCardIssuerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateFareMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FareGateBlockEntity>> FARE_GATE =
            BLOCK_ENTITY_TYPES.register(
                    "fare_gate",
                    () -> BlockEntityType.Builder.of(FareGateBlockEntity::new, ModBlocks.FARE_GATE.get()).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IcCardIssuerBlockEntity>> IC_CARD_ISSUER =
            BLOCK_ENTITY_TYPES.register(
                    "ic_card_issuer",
                    () -> BlockEntityType.Builder.of(
                            IcCardIssuerBlockEntity::new,
                            ModBlocks.IC_CARD_ISSUER.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
