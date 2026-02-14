package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.undertale.entity.attachment.KaramMobEffectData;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.net.packet.GravityPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:07
 */
@EventBusSubscriber
public class AttachmentTypes {
    private static final Logger log = LoggerFactory.getLogger(AttachmentTypes.class);
    public static DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Undertale.MOD_ID);

    public static final Supplier<AttachmentType<KaramMobEffectData>> KARMA_MOB_EFFECT = ATTACHMENT_TYPES.register(
            "karma_mob_effect", () -> AttachmentType.builder(KaramMobEffectData::new).serialize(KaramMobEffectData.CODEC).build()
    );
    public static final Supplier<AttachmentType<KaramAttackData>> KARMA_ATTACK = ATTACHMENT_TYPES.register(
            "karma_attack", () -> AttachmentType.builder(KaramAttackData::new).serialize(KaramAttackData.CODEC).build()
    );
    public static final Supplier<AttachmentType<GravityData>> GRAVITY = ATTACHMENT_TYPES.register(
            "gravity", () -> AttachmentType.builder(()->new GravityData()).serialize(GravityData.CODEC).build()
    );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }


    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        ServerPlayer player = (ServerPlayer) event.getEntity();
        GravityData data = target.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
            PacketDistributor.sendToPlayer(player,new GravityPacket(target.getId(), data.getGravity(), target.getDeltaMovement()));
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        GravityData data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            PacketDistributor.sendToPlayer(player,new GravityPacket(player.getId(), data.getGravity(), player.getDeltaMovement()));
        }
    }
}
