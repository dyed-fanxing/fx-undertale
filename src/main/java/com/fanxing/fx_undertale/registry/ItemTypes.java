package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.boss.sans.SansAi;
import com.fanxing.fx_undertale.item.GasterBlasterItem;
import com.fanxing.fx_undertale.item.GravityDebugStick;
import com.fanxing.fx_undertale.item.MagicBone;
import com.fanxing.lib.item.compoent.ColorPalette;
import com.fanxing.lib.registry.DataComponentsFxLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.fanxing.fx_undertale.FxUndertale.MOD_ID;
import static com.fanxing.fx_undertale.registry.BlockTypes.PLATFORM_BLOCK;

public class ItemTypes {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID); // 物品注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);// 创造标签页注册器

    public static void register(IEventBus bus) {
        // 将物品延迟注册器注册到模组事件总线
        ITEMS.register(bus);
        // 将创造创造标签页延迟注册器注册到模组事件总线
        CREATIVE_MODE_TABS.register(bus);
    }

    public static final Supplier<GasterBlasterItem> GASTER_BLASTER = ITEMS.registerItem("gaster_blaster",
            properties -> new GasterBlasterItem(properties.stacksTo(1)
                    .component(DataComponentsFxLib.COLOR_SCHEME, SansAi.ENERGY_AQUA)
                    .component(DataComponentsFxLib.COLOR_PALETTES, new ArrayList<>(List.of(
//                            new ColorPalette(Component.translatable("gui.fx_lib.color_preset.rainbow_gradient"), List.of()),
                            new ColorPalette(Component.translatable("options.gamma.default"), SansAi.ENERGY_AQUA))
                    )))
    );
    public static final Supplier<GravityDebugStick> GRAVITY_DEBUG_STICK = ITEMS.registerItem("gravity_debug_stick", GravityDebugStick::new);//使用注册器注册物品，并返回注册的物品
    public static final Supplier<MagicBone> MAGIC_BONE = ITEMS.registerItem("bone", MagicBone::new);//使用注册器注册物品，并返回注册的物品


    public static final DeferredItem<BlockItem> PLATFORM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(PLATFORM_BLOCK);


    // 创建创造标签页，并添加物品，放置在战斗标签页之后
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register(MOD_ID + "_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID)) // 标签页标题
                    .icon(() -> new ItemStack(GASTER_BLASTER.get()))
                    .withTabsBefore(CreativeModeTabs.COMBAT) // 定位在战斗标签页前
                    .displayItems((parameters, output) -> {
                        output.accept(GASTER_BLASTER.get());
                        output.accept(GRAVITY_DEBUG_STICK.get());
                        output.accept(MAGIC_BONE.get());
                        output.accept(PLATFORM_BLOCK_ITEM.get());
                    }).build());
}
