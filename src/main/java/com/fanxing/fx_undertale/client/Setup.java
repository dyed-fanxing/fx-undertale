package com.fanxing.fx_undertale.client;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.render.summon.RotationBoneRenderer;
import com.fanxing.fx_undertale.client.render.summon.GasterBlasterRender;
import com.fanxing.fx_undertale.client.render.summon.GroundBoneOBBRender;
import com.fanxing.fx_undertale.client.render.summon.GroundBoneRender;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.net.packet.*;
import com.fanxing.fx_undertale.client.particle.BallGrowParticle;
import com.fanxing.fx_undertale.client.particle.CustomWhiteAshNoGravityParticle;
import com.fanxing.fx_undertale.client.particle.CustomWhiteAshParticle;
import com.fanxing.fx_undertale.client.particle.LightStreakParticle;
import com.fanxing.fx_undertale.client.render.projectile.FlyingBoneRenderer;
import com.fanxing.fx_undertale.client.render.boss.SansRender;
import com.fanxing.fx_undertale.client.screen.GravitySelectionScreen;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.registry.MenuTypes;
import com.fanxing.fx_undertale.registry.ParticleTypes;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本地客户端注册绑定服务端相关的注册表标识
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID, value = Dist.CLIENT)
public class Setup {
    /**
     * 监听客户端实体渲染事件
     * 添加 将服务端注册的标识与客户端具体渲染实现绑定一起
     */
    @SubscribeEvent
    public static void registerRendererHandler(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.GASTER_BLASTER.get(), GasterBlasterRender::new);
        event.registerEntityRenderer(EntityTypes.SANS.get(), SansRender::new);
        event.registerEntityRenderer(EntityTypes.GROUND_BONE_OBB.get(), GroundBoneOBBRender::new);
        event.registerEntityRenderer(EntityTypes.GROUND_BONE.get(), GroundBoneRender::new);
        event.registerEntityRenderer(EntityTypes.FLYING_BONE.get(), FlyingBoneRenderer::new);
        event.registerEntityRenderer(EntityTypes.ROTATION_BONE.get(), RotationBoneRenderer::new);
    }

    /**
     * 注册需要属性的实体，即继承自LivingEntity
     */
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityTypes.SANS.get(), Sans.createAttributes().build());
        event.put(EntityTypes.GASTER_BLASTER.get(), GasterBlaster.createAttributes().build());
    }


    /**
     * 监听客户端注册粒子提供者事件
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerParticleProviderHandler(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.BALL_GROW.get(), BallGrowParticle.Provider::new);
        event.registerSpriteSet(ParticleTypes.LIGHT_STREAK.get(), LightStreakParticle.Provider::new);
        event.registerSpriteSet(ParticleTypes.CUSTOM_WHITE_ASH.get(), CustomWhiteAshParticle.Provider::new);
        event.registerSpriteSet(ParticleTypes.CUSTOM_NO_GRAVITY_WHITE_ASH.get(), CustomWhiteAshNoGravityParticle.Provider::new);
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

        registrar.playToClient(WarningTipPacket.Cylinder.TYPE, WarningTipPacket.Cylinder.STREAM_CODEC, WarningTipPacket.Cylinder::handle);
        registrar.playToClient(WarningTipPacket.Cube.TYPE, WarningTipPacket.Cube.STREAM_CODEC, WarningTipPacket.Cube::handle);
        registrar.playToClient(WarningTipPacket.Quad.TYPE, WarningTipPacket.Quad.STREAM_CODEC, WarningTipPacket.Quad::handle);
        registrar.playToClient(WarningTipPacket.QuadPrecession.TYPE, WarningTipPacket.QuadPrecession.STREAM_CODEC, WarningTipPacket.QuadPrecession::handle);
        registrar.playToClient(WarningTipPacket.QuadCirclePrecession.TYPE, WarningTipPacket.QuadCirclePrecession.STREAM_CODEC, WarningTipPacket.QuadCirclePrecession::handle);
        registrar.playToClient(WarningTipPacket.Circle.TYPE, WarningTipPacket.Circle.STREAM_CODEC, WarningTipPacket.Circle::handle);
        registrar.playToClient(WarningTipPacket.CurveStrip.TYPE, WarningTipPacket.CurveStrip.STREAM_CODEC, WarningTipPacket.CurveStrip::handle);
        registrar.playToClient(WarningTipPacket.RadialPrecessionCurveStripsPacket.TYPE, WarningTipPacket.RadialPrecessionCurveStripsPacket.STREAM_CODEC, WarningTipPacket.RadialPrecessionCurveStripsPacket::handle);
        registrar.playToClient(WarningTipPacket.RadialPrecessionCurveStripsGravityPacket.TYPE, WarningTipPacket.RadialPrecessionCurveStripsGravityPacket.STREAM_CODEC, WarningTipPacket.RadialPrecessionCurveStripsGravityPacket::handle);


        registrar.playToClient(GravityPacket.TYPE,GravityPacket.STREAM_CODEC, GravityPacket::handle);
        registrar.playToClient(GravityControlTagPacket.TYPE,GravityControlTagPacket.STREAM_CODEC, GravityControlTagPacket::handle);


        registrar.playToClient(KaramPacket.TYPE,KaramPacket.STREAM_CODEC, KaramPacket::handle);
        registrar.playToClient(KaramTagPacket.TYPE, KaramTagPacket.STREAM_CODEC, KaramTagPacket::handle);
        registrar.playToClient(TimeJumpTeleportPacket.TYPE,TimeJumpTeleportPacket.STREAM_CODEC, TimeJumpTeleportPacket::handle);


        registrar.playToClient(SyncMotionPayload.TYPE,SyncMotionPayload.STREAM_CODEC, SyncMotionPayload::handle);
        registrar.playToClient(QuaternionSyncPacket.TYPE,QuaternionSyncPacket.STREAM_CODEC, QuaternionSyncPacket::handle);





        registrar.playToServer(GravitySelectionPacket.TYPE,GravitySelectionPacket.STREAM_CODEC, GravitySelectionPacket::handle);
        registrar.playToServer(MercyTriggerPacket.TYPE,MercyTriggerPacket.STREAM_CODEC, MercyTriggerPacket::handle);
    }

    /**
     * 监听客户端菜单屏幕事件
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(MenuTypes.GRAVITY_SELECTION_MENU.get(), GravitySelectionScreen::new);
    }


    /**
     * PLA API的注册玩家动画控制器，类似gecklib的动画控制器
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册动画层。1000 是优先级，你可以根据需要调整（文档建议重要动画用 1500+）
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(PlayerAnimations.ATTACK, 1500,
                    player -> new PlayerAnimationController(player,(controller, state, animSetter) -> PlayState.STOP)
            );
        });
    }
}

