package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.attachment.KaramJudge;
import com.fanxing.fx_undertale.entity.attachment.Karam;
import com.fanxing.fx_undertale.entity.attachment.PlayerSoul;
import com.fanxing.fx_undertale.entity.persistentData.SoulMode;
import com.fanxing.fx_undertale.net.packet.GravityPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author FanXing
 * @since 2025-09-13 22:07
 */
@EventBusSubscriber
public class AttachmentTypes {
    private static final Logger log = LoggerFactory.getLogger(AttachmentTypes.class);
    public static DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FxUndertale.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSoul>> PLAYER_SOUL = ATTACHMENT_TYPES.register("player_soul",
                    () -> AttachmentType.builder(PlayerSoul::new).serialize(PlayerSoul.PLAYER_SOUL_DATA).copyOnDeath().build()
    );
    public static final Supplier<AttachmentType<Karam>> KARMA = ATTACHMENT_TYPES.register(
            "karma", () -> AttachmentType.builder(Karam::new).serialize(Karam.CODEC).build()
    );
    public static final Supplier<AttachmentType<KaramJudge>> KARMA_ATTACK = ATTACHMENT_TYPES.register(
            "karma_judge", () -> AttachmentType.builder(KaramJudge::new).serialize(KaramJudge.CODEC).build()
    );
    public static final Supplier<AttachmentType<Gravity>> GRAVITY = ATTACHMENT_TYPES.register(
            "gravity", () -> AttachmentType.builder(()->new Gravity()).serialize(Gravity.CODEC).build()
    );
    public static final Supplier<AttachmentType<Boolean>> KARMA_TAG = ATTACHMENT_TYPES.register(
            "karma_tag", () -> AttachmentType.builder(()->false).build()
    );
    public static final Supplier<AttachmentType<Byte>> SOUL_MODE = ATTACHMENT_TYPES.register(
            "soul_mode", () -> AttachmentType.builder(()-> SoulMode.DEFAULT).build()
    );
    public static final Supplier<AttachmentType<Boolean>> GRAVITY_CONTROL_TAG = ATTACHMENT_TYPES.register(
            "gravity_control_tag", () -> AttachmentType.builder(()->false).build()
    );
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }


    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Gravity data = target.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
            PacketDistributor.sendToPlayer(player,new GravityPacket(target.getId(), data.getGravity()));
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Gravity data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            PacketDistributor.sendToPlayer(player,new GravityPacket(player.getId(), data.getGravity()));
        }
    }
}
