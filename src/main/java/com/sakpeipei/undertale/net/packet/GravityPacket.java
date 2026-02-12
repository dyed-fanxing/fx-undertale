package com.sakpeipei.undertale.net.packet;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:52
 */
public record GravityPacket(int attackId, int targetId, Direction gravity,Vec3 deltaMovement) implements CustomPacketPayload {
    public static final Type<GravityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "gravity_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravityPacket> STREAM_CODEC = CustomPacketPayload.codec(GravityPacket::write, GravityPacket::new);
    private static final Logger log = LoggerFactory.getLogger(GravityPacket.class);

    public GravityPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(),buf.readEnum(Direction.class),buf.readVec3());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(attackId);
        buf.writeVarInt(targetId);
        buf.writeEnum(gravity);
        buf.writeVec3(deltaMovement);
    }

    public static void handle(GravityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity attacker = level.getEntity(packet.attackId);
                Entity target = level.getEntity(packet.targetId);
                if(attacker instanceof Sans sans && target instanceof LivingEntity livingTarget){
                    target.setDeltaMovement(packet.deltaMovement);

                    GravityData oldGravity = target.getData(AttachmentTypeRegistry.GRAVITY);
                    GravityData gravityData = new GravityData(packet.gravity);
                    target.setData(AttachmentTypeRegistry.GRAVITY,new GravityData(packet.gravity));
                    gravityData.applyGravity(target,oldGravity);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<GravityPacket> type() {
        return TYPE;
    }
}
