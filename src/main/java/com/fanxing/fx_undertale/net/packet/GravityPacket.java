package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
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

/**
 * @author FanXing
 * @since 2025-09-13 22:52
 * @param a 局部重力方向上的初始速度
 */
public record GravityPacket(int entityId, Direction gravity,float acceleration) implements CustomPacketPayload {
    public static final Type<GravityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "gravity_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravityPacket> STREAM_CODEC = CustomPacketPayload.codec(GravityPacket::write, GravityPacket::new);

    public GravityPacket(int entityId, Direction gravity) {
        this(entityId, gravity, 0f);
    }

    public GravityPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readEnum(Direction.class),buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeEnum(gravity);
        buf.writeFloat(acceleration);
    }

    public static void handle(GravityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity != null) {
                    Gravity.applyGravity(entity, packet.gravity);
                    entity.addDeltaMovement(new Vec3(0,-packet.acceleration,0));
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<GravityPacket> type() {
        return TYPE;
    }

}
