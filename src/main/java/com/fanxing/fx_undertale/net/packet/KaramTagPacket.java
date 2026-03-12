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

public record KaramTagPacket(int entityId, boolean exist) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaramTagPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "karam_exist_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KaramTagPacket> STREAM_CODEC = CustomPacketPayload.codec(KaramTagPacket::write, KaramTagPacket::new);

    public KaramTagPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(exist);
    }

    public static void handle(KaramTagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof LivingEntity entity) {
                entity.setData(AttachmentTypes.KARMA_TAG, packet.exist);
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<KaramTagPacket> type() {
        return TYPE;
    }
}
