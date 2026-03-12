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
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;


/**
 * 灵魂状态
 */
public record SoulModePacket(int entityId, byte mode) implements CustomPacketPayload {
    public static final Type<SoulModePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "soul_state_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulModePacket> STREAM_CODEC = CustomPacketPayload.codec(SoulModePacket::write, SoulModePacket::new);

    public SoulModePacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readByte());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(mode);
    }

    public static void handle(SoulModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity != null) {
                    entity.setData(AttachmentTypes.SOUL_MODE, packet.mode);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<SoulModePacket> type() {
        return TYPE;
    }
}