package com.fanxing.fx_undertale.net.packet;


import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GravityAccPacket(int entityId, float acc) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GravityAccPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "gravity_control_tag_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravityAccPacket> STREAM_CODEC = CustomPacketPayload.codec(GravityAccPacket::write, GravityAccPacket::new);

    public GravityAccPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(acc);
    }

    public static void handle(GravityAccPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof LivingEntity entity) {
                entity.setData(AttachmentTypes.GRAVITY_ACC, packet.acc);
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<GravityAccPacket> type() {
        return TYPE;
    }
}
