package com.sakpeipei.mod.undertale.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.sakpeipei.mod.undertale.Undertale.MODID;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);// 方块注册器
    public static void register(IEventBus bus) {
        // 将方块延迟注册器注册到模组事件总线
        BLOCKS.register(bus);
    }



}
