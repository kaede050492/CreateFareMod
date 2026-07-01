package com.kaede050492.createfaremod.registry;

import com.kaede050492.createfaremod.CreateFareMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateFareMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_SUCCESS = register("gate_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_FAILURE = register("gate_failure");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_OPEN = register("gate_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_CLOSE = register("gate_close");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateFareMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
