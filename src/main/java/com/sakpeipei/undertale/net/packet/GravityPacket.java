package com.sakpeipei.undertale.net.packet;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:52
 */
public record GravityPacket(int entityId, Direction gravity,Vec3 deltaMovement) implements CustomPacketPayload {
    public static final Type<GravityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "gravity_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravityPacket> STREAM_CODEC = CustomPacketPayload.codec(GravityPacket::write, GravityPacket::new);

    public GravityPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readEnum(Direction.class),buf.readVec3());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeEnum(gravity);
        buf.writeVec3(deltaMovement);
    }

    public static void handle(GravityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity != null) {
                    entity.setDeltaMovement(packet.deltaMovement);
                    GravityData oldGravity = entity.getData(AttachmentTypes.GRAVITY);
                    GravityData gravityData = new GravityData(packet.gravity);
                    entity.setData(AttachmentTypes.GRAVITY,new GravityData(packet.gravity));
                    gravityData.applyGravity(entity,oldGravity);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<GravityPacket> type() {
        return TYPE;
    }
}
