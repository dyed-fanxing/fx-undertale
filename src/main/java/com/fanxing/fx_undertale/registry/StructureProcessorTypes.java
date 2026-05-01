package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class StructureProcessorTypes {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, FxUndertale.MOD_ID);
    public static void register(IEventBus bus) {
        PROCESSORS.register(bus);
    }
}
