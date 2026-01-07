package com.sakpeipei.undertale;

import com.mojang.logging.LogUtils;
import com.sakpeipei.undertale.registry.*;
import com.sakpeipei.undertale.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// 此处值应与META-INF/neoforge.mods.toml文件中的条目匹配
@Mod(Undertale.MODID)
public class Undertale {
    // 在公共位置定义mod id供所有内容引用
    public static final String MODID = "undertale";
    public static final Logger LOGGER = LogUtils.getLogger();



    // 模组类的构造函数是模组加载时运行的第一段代码
    public Undertale(IEventBus modEventBus, ModContainer modContainer) {
        // 注册commonSetup方法用于模组加载
        modEventBus.addListener(this::commonSetup);


        BlockRegistry.register(modEventBus);        // 方块注册
        ItemRegistry.register(modEventBus);         // 物品注册
        EntityTypeRegistry.register(modEventBus);   // 实体注册
        MobEffectRegistry.registry(modEventBus);    // buff注册
        SoundRegistry.register(modEventBus);        // 声音注册
        ParticleRegistry.register(modEventBus);     // 粒子注册

        //neo forge注册

        AttachmentTypeRegistry.register(modEventBus);


        // 注册当前类以响应游戏事件
        NeoForge.EVENT_BUS.register(this);
        // 注册将物品添加到创造标签页的方法
        modEventBus.addListener(this::addCreative);
        // 注册模组的配置规范，以便FML可以创建和加载配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // 通用设置方法
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("通用设置初始化");

        if (Config.logDirtBlock) {
            LOGGER.info("泥土方块 >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("物品 >> {}", item));
    }

    // 将示例方块物品添加到建筑方块标签页
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
        }
    }

    // 服务器启动时执行的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器正在启动");
    }

    // 客户端模组事件订阅类
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("客户端设置初始化");
            LOGGER.info("MINECRAFT用户名 >> {}", Minecraft.getInstance().getUser().getName());

        }
    }
}