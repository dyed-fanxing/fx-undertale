package com.sakpeipei.undertale.client;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.particle.BallGrowParticle;
import com.sakpeipei.undertale.client.particle.LightStreakParticle;
import com.sakpeipei.undertale.client.render.entity.*;
import com.sakpeipei.undertale.network.*;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.registry.ParticleRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本地客户端注册绑定服务端相关的注册表标识
 */
@EventBusSubscriber(modid = Undertale.MODID, value = Dist.CLIENT)
public class Setup {
    /**
     * 监听客户端实体渲染事件
     * 添加 将服务端注册的标识与客户端具体渲染实现绑定一起
     * @param event
     */
    @SubscribeEvent
    public static void registerRendererHandler(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypeRegistry.GASTER_BLASTER.get(), GasterBlasterRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), GasterBlasterProRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.SANS.get(), SansRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.FLYING_BONE.get(), FlyingBoneRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.GROUND_BONE.get(), GroundBoneRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.MOVING_GROUND_BONE.get(), MovingGroundBoneRender::new);
    }

    /**
     * 监听客户端注册粒子提供者事件
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerParticleProviderHandler(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.BALL_GROW.get(), BallGrowParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.LIGHT_STREAK.get(), LightStreakParticle.Provider::new);
    }

    /**
     * 监听客户端注册网络发包事件
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerPayloadHandler(final RegisterPayloadHandlersEvent event) {
        // 初始化注册器，设置网络版本为"1"
        final PayloadRegistrar registrar = event.registrar("1");
        // 仅客户端接收的Payload
        registrar.playToClient(GasterBlasterProPacket.TYPE, GasterBlasterProPacket.STREAM_CODEC, GasterBlasterProPacket::handle);
        registrar.playToClient(KaramPacket.TYPE,KaramPacket.STREAM_CODEC, KaramPacket::handle);
        registrar.playToClient(WarningTipPacket.TYPE, WarningTipPacket.STREAM_CODEC, WarningTipPacket::handle);
//        registrar.playToClient(WarningTipAABBPacket.TYPE, WarningTipAABBPacket.STREAM_CODEC, WarningTipAABBPacket::handle);
        registrar.playToClient(AnimPacket.TYPE, AnimPacket.STREAM_CODEC, AnimPacket::handle);
//        registrar.playToClient(GasterBlasterBeamEndPacket.TYPE,GasterBlasterBeamEndPacket.STREAM_CODEC, GasterBlasterBeamEndPacket::handle);
        registrar.playToClient(TimeJumpTeleportPacket.TYPE,TimeJumpTeleportPacket.STREAM_CODEC, TimeJumpTeleportPacket::handle);
    }
}

