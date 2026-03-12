package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.item.GasterBlasterItem;
import com.fanxing.fx_undertale.item.GravityTestItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.fanxing.fx_undertale.FxUndertale.MOD_ID;

public class ItemTypes {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID); // 物品注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);// 创造标签页注册器

    public static void register(IEventBus bus) {
        // 将物品延迟注册器注册到模组事件总线
        ITEMS.register(bus);
        // 将创造创造标签页延迟注册器注册到模组事件总线
        CREATIVE_MODE_TABS.register(bus);
    }

    public static final Supplier<GasterBlasterItem> GASTER_BLASTER = ITEMS.registerItem("gaster_blaster", GasterBlasterItem::new);//使用注册器注册物品，并返回注册的物品
    public static final Supplier<GravityTestItem> GRAVITY_DEBUG_STICK = ITEMS.registerItem("gravity_debug_stick", GravityTestItem::new);//使用注册器注册物品，并返回注册的物品

    // 创建创造标签页，并添加物品，放置在战斗标签页之后
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("undertale_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fx_undertale")) // 标签页标题
                    .icon(() -> new ItemStack(GASTER_BLASTER.get()))
                    .withTabsBefore(CreativeModeTabs.COMBAT) // 定位在战斗标签页前
                    .displayItems((parameters, output) -> {
                        output.accept(GASTER_BLASTER.get());
                        output.accept(GRAVITY_DEBUG_STICK.get());
                    }).build());
}
