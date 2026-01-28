package com.sakpeipei.undertale.network;

import com.sakpeipei.undertale.Undertale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakpeipei
 * @since 2025/12/16 15:45
 */
public record TimeJumpTeleportPacket(int entityId, int endTick) implements CustomPacketPayload {
    public static final String END_TICK = Undertale.MODID + ":teleport_time_jump_end_tick";

    public static final CustomPacketPayload.Type<TimeJumpTeleportPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "teleport_time_jump_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimeJumpTeleportPacket> STREAM_CODEC = CustomPacketPayload.codec(TimeJumpTeleportPacket::write, TimeJumpTeleportPacket::new);

    public TimeJumpTeleportPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readVarInt());
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.endTick);
    }

    public static void handle(TimeJumpTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity != null) {
                    entity.getPersistentData().putInt(END_TICK,packet.endTick);
                }
            }
        });
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
