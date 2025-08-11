package com.sakpeipei.mod.undertale.registry;

import com.sakpeipei.mod.undertale.item.GasterBlasterFixedItem;
import com.sakpeipei.mod.undertale.item.GasterBlasterProItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.sakpeipei.mod.undertale.Undertale.MODID;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID); // 物品注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);// 创造标签页注册器
    public static void register(IEventBus bus) {
        // 将物品延迟注册器注册到模组事件总线
        ITEMS.register(bus);
        // 将创造创造标签页延迟注册器注册到模组事件总线
        CREATIVE_MODE_TABS.register(bus);
    }

    public static final DeferredItem<Item> GASTER_BLASTER = ITEMS.registerItem("gaster_blaster", GasterBlasterFixedItem::new);//使用注册器注册物品，并返回注册的物品
    public static final DeferredItem<Item> GASTER_BLASTER_PRO = ITEMS.registerItem("gaster_blaster_pro", GasterBlasterProItem::new);//使用注册器注册物品，并返回注册的物品

    // 创建创造标签页，并添加物品，放置在战斗标签页之后
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("undertale_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.undertale")) // 标签页标题
                    .withTabsImage(ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/determination.png"))
                    .withTabsBefore(CreativeModeTabs.COMBAT) // 定位在战斗标签页前
                    .displayItems((parameters, output) -> {
                        output.accept(GASTER_BLASTER.get());
                        output.accept(GASTER_BLASTER_PRO.get());
                    }).build());
}
