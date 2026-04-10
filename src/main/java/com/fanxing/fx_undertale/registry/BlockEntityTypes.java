package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityTypes {
    // 1. 在 Mod 主类或专门的注册类中，创建 DeferredRegister 实例
//    并让它指向内置的 BLOCK_ENTITY_TYPES 注册表
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FxUndertale.MOD_ID);

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
