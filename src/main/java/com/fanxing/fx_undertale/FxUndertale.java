package com.fanxing.fx_undertale;

import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// 此处值应与META-INF/neoforge.mods.toml文件中的条目匹配
@Mod(FxUndertale.MOD_ID)
public class FxUndertale {
    // 在公共位置定义mod id供所有内容引用
    public static final String MOD_ID = "fx_undertale";
    public static final Logger LOGGER = LogUtils.getLogger();


    // 模组类的构造函数是模组加载时运行的第一段代码
    public FxUndertale(IEventBus modEventBus, ModContainer modContainer) {
        // 注册commonSetup方法用于模组加载
        modEventBus.addListener(this::commonSetup);


        BlockTypes.register(modEventBus);               // 方块注册
        ItemTypes.register(modEventBus);                // 物品注册

        EntityTypes.register(modEventBus);              // 实体注册
        MobEffectTypes.registry(modEventBus);           // buff注册

        MemoryModuleTypes.register(modEventBus);        // 记忆注册
        SoundEvnets.register(modEventBus);              // 声音注册
        ParticleTypes.register(modEventBus);            // 粒子注册
        AttachmentTypes.register(modEventBus);          // 附件注册
        MenuTypes.register(modEventBus);                // 菜单注册


        StructureProcessorTypes.register(modEventBus);  // 结构回调注册

        // 注册当前类以响应游戏事件
        NeoForge.EVENT_BUS.register(this);

        // 注册将物品添加到创造标签页的方法
        modEventBus.addListener(this::addCreative);

        // 注册模组的配置规范，以便FML可以创建和加载配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        // SERVER端配置，自动同步
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        // 客户端注册原生配置界面
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }




        PhysicsMotionModel.registry();
    }

    // 通用设置方法
    private void commonSetup(final FMLCommonSetupEvent event) {
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
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("客户端设置初始化");
            LOGGER.info("MINECRAFT用户名 >> {}", Minecraft.getInstance().getUser().getName());

        }
    }
}