package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.capability.Animatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author FanXing
 * @since 2025-11-19 20:02
 */
public record AnimPacket(int entityId, int id,float speed) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<AnimPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "anim_id_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnimPacket> STREAM_CODEC = CustomPacketPayload.codec(AnimPacket::write, AnimPacket::new);
    private static final Logger log = LoggerFactory.getLogger(AnimPacket.class);

    public AnimPacket(int entityId, int id) {
        this(entityId, id, 1.0f);
    }
    public AnimPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readByte(),buf.readFloat());
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.id);
        buf.writeFloat(this.speed);
    }

    public static void handle(AnimPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof Animatable entity) {
                entity.setAnimID(packet.id);
            }
        });
    }
    @Override
    public @NotNull Type<AnimPacket> type() {
        return TYPE;
    }
}