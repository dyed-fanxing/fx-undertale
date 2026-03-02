package com.sakpeipei.undertale.client;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.particle.BallGrowParticle;
import com.sakpeipei.undertale.client.particle.LightStreakParticle;
import com.sakpeipei.undertale.client.render.entity.*;
import com.sakpeipei.undertale.client.render.entity.boss.SansRender;
import com.sakpeipei.undertale.client.render.entity.summon.*;
import com.sakpeipei.undertale.client.screen.GravitySelectionScreen;
import com.sakpeipei.undertale.net.packet.*;
import com.sakpeipei.undertale.registry.EntityTypes;
import com.sakpeipei.undertale.registry.MenuTypes;
import com.sakpeipei.undertale.registry.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本地客户端注册绑定服务端相关的注册表标识
 */
@EventBusSubscriber(modid = Undertale.MOD_ID, value = Dist.CLIENT)
public class Setup {
    /**
     * 监听客户端实体渲染事件
     * 添加 将服务端注册的标识与客户端具体渲染实现绑定一起
     */
    @SubscribeEvent
    public static void registerRendererHandler(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.GASTER_BLASTER.get(), GasterBlasterRender::new);
//        event.registerEntityRenderer(EntityTypes.GASTER_BLASTER_PRO.get(), GasterBlasterProRender::new);
        event.registerEntityRenderer(EntityTypes.SANS.get(), SansRender::new);
        event.registerEntityRenderer(EntityTypes.GROUND_BONE.get(), GroundBoneRender::new);
        event.registerEntityRenderer(EntityTypes.MOVING_GROUND_BONE.get(), MovingGroundBoneRender::new);
        event.registerEntityRenderer(EntityTypes.LATERAL_BONE.get(), LateralBoneRender::new);
        event.registerEntityRenderer(EntityTypes.FLYING_BONE.get(), FlyingBoneRender::new);
    }

    /**
     * 监听客户端注册粒子提供者事件
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerParticleProviderHandler(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.BALL_GROW.get(), BallGrowParticle.Provider::new);
        event.registerSpriteSet(ParticleTypes.LIGHT_STREAK.get(), LightStreakParticle.Provider::new);
    }

    /**
     * 监听客户端服务端注册网络发包事件
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerPayloadHandler(final RegisterPayloadHandlersEvent event) {
        // 初始化注册器，设置网络版本为"1"
        final PayloadRegistrar registrar = event.registrar("1");
        // 仅客户端接收的Payload

        registrar.playToClient(AnimPacket.TYPE, AnimPacket.STREAM_CODEC, AnimPacket::handle);
        registrar.playToClient(SoulModePacket.TYPE, SoulModePacket.STREAM_CODEC, SoulModePacket::handle);

        registrar.playToClient(GasterBlasterProPacket.TYPE, GasterBlasterProPacket.STREAM_CODEC, GasterBlasterProPacket::handle);

        registrar.playToClient(WarningTipPacket.Cylinder.TYPE, WarningTipPacket.Cylinder.STREAM_CODEC, WarningTipPacket.Cylinder::handle);
        registrar.playToClient(WarningTipPacket.Cube.TYPE, WarningTipPacket.Cube.STREAM_CODEC, WarningTipPacket.Cube::handle);
//        registrar.playToClient(WarningTipAABBPacket.TYPE, WarningTipAABBPacket.STREAM_CODEC, WarningTipAABBPacket::handle);
        registrar.playToClient(TimeJumpTeleportPacket.TYPE,TimeJumpTeleportPacket.STREAM_CODEC, TimeJumpTeleportPacket::handle);
        registrar.playToClient(GravityPacket.TYPE,GravityPacket.STREAM_CODEC, GravityPacket::handle);
        registrar.playToClient(KaramPacket.TYPE,KaramPacket.STREAM_CODEC, KaramPacket::handle);
        registrar.playToClient(KaramTagPacket.TYPE, KaramTagPacket.STREAM_CODEC, KaramTagPacket::handle);
        registrar.playToServer(GravitySelectionPacket.TYPE,GravitySelectionPacket.STREAM_CODEC, GravitySelectionPacket::handle);
    }

    /**
     * 监听客户端菜单屏幕事件
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(MenuTypes.GRAVITY_SELECTION_MENU.get(), GravitySelectionScreen::new);
    }
}

