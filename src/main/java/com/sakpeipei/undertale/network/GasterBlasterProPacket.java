package com.sakpeipei.undertale.network;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GasterBlasterProPacket(int entityId,short timer) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GasterBlasterProPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "gaster_blaster_pro"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasterBlasterProPacket> STREAM_CODEC = CustomPacketPayload.codec(GasterBlasterProPacket::write, GasterBlasterProPacket::new);


    public GasterBlasterProPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readShort());
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeShort(this.timer);
    }

    public static void handle(GasterBlasterProPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof GasterBlasterPro entity) {
                entity.timer = packet.timer;
            }
        });
    }

    @Override
    public @NotNull Type<GasterBlasterProPacket> type() {
        return TYPE;
    }
}

