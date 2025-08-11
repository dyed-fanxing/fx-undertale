package com.sakpeipei.mod.undertale.client;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.particle.BallGrowParticle;
import com.sakpeipei.mod.undertale.client.particle.LightStreakParticle;
import com.sakpeipei.mod.undertale.client.render.entity.GasterBlasterFixedRender;
import com.sakpeipei.mod.undertale.client.render.entity.GasterBlasterProRender;
import com.sakpeipei.mod.undertale.client.render.entity.SansRender;
import com.sakpeipei.mod.undertale.network.GasterBlasterProPacket;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.registry.ParticleRegistry;
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
     * 客户端注册渲染，将服务端注册的标识与客户端具体渲染实现绑定一起
     * @param event
     */
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypeRegistry.GASTER_BLASTER_FIXED.get(), GasterBlasterFixedRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), GasterBlasterProRender::new);
        event.registerEntityRenderer(EntityTypeRegistry.SANS.get(), SansRender::new);
    }

    /**
     * 客户端注册粒子，
     * @param event 三种注册方式
     */
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.BALL_GROW.get(), BallGrowParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.LIGHT_STREAK.get(), LightStreakParticle.Provider::new);
    }


    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // 初始化注册器，设置网络版本为"1"
        final PayloadRegistrar registrar = event.registrar("1");

        // 仅客户端接收的Payload
        registrar.playToClient(GasterBlasterProPacket.TYPE, GasterBlasterProPacket.STREAM_CODEC, GasterBlasterProPacket::handle);
    }
}

