package com.sakpeipei.undertale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 配置类示例。虽然不是必须的，但建议使用配置类来保持配置的组织性。
 * 演示如何使用NeoForge的配置API
 */
@EventBusSubscriber(modid = Undertale.MOD_ID) // 自动注册到模组事件总线
public class Config {
    // 配置构建器
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 是否在通用设置时记录泥土方块
    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("是否在通用设置时记录泥土方块")
            .define("logDirtBlock", true);

    // 魔法数字配置
    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("一个魔法数字")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    // 魔法数字的介绍文本
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("魔法数字的介绍文本")
            .define("magicNumberIntroduction", "魔法数字是... ");

    // 物品资源定位符列表
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("在通用设置时要记录的物品列表")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"),null, Config::validateItemName);

    // 最终构建的配置规范
    static final ModConfigSpec SPEC = BUILDER.build();

    // 配置值的运行时缓存
    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    /**
     * 验证物品名称是否有效
     * @param obj 要验证的对象
     * @return 如果是有效的物品名称则返回true
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName &&
                BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    /**
     * 当配置加载时调用的方法
     * @param event 配置事件
     */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 从配置中获取值并缓存
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // 将字符串列表转换为物品集合
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
    }
}