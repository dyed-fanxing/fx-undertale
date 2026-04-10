package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.block.PlatformBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.fanxing.fx_undertale.FxUndertale.MOD_ID;

public class BlockTypes {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);// 方块注册器

    public static final DeferredBlock<Block> PLATFORM_BLOCK = BLOCKS.register("platform",() -> new PlatformBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)                           // 地图颜色，可选
            .strength(1.5f, 6.0f)              // 硬度和抗性
            .noOcclusion()                                      // 如果模型不是完整方块则必须
            .dynamicShape()                                     // 如果形状会变则必须
            .pushReaction(PushReaction.BLOCK)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

}
