package com.sakpeipei.undertale.net.packet;


import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.attachment.PersistentDataDict;
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
public record SoulPatternPacket(int entityId, byte state) implements CustomPacketPayload {
    public static final Type<SoulPatternPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "soul_state_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulPatternPacket> STREAM_CODEC = CustomPacketPayload.codec(SoulPatternPacket::write, SoulPatternPacket::new);

    public SoulPatternPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readByte());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(state);
    }

    public static void handle(SoulPatternPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity != null) {
                    entity.getPersistentData().putByte(PersistentDataDict.SOUL_PATTERN, packet.state);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<SoulPatternPacket> type() {
        return TYPE;
    }
}