package com.sakpeipei.mod.undertale.network;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterPro;
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

public class GasterBlasterProPacket implements CustomPacketPayload {
    private final short timer;
    private final int entityId;
    public static final CustomPacketPayload.Type<GasterBlasterProPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "gaster_blaster_pro"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasterBlasterProPacket> STREAM_CODEC = CustomPacketPayload.codec(GasterBlasterProPacket::write, GasterBlasterProPacket::new);

    public GasterBlasterProPacket(short timer, int entityId) {
        this.timer = timer;
        this.entityId = entityId;
    }

    public GasterBlasterProPacket(FriendlyByteBuf buf) {
        this.timer = buf.readShort();
        this.entityId = buf.readVarInt();
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.timer);
        buf.writeVarInt(this.entityId);
    }

    public static void handle(GasterBlasterProPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);
                if (entity instanceof GasterBlasterPro entity1) {
                    entity1.timer = packet.timer;
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

