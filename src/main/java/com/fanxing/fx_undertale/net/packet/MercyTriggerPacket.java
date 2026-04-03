package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MercyTriggerPacket(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MercyTriggerPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "mercy_trigger_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MercyTriggerPacket> STREAM_CODEC = CustomPacketPayload.codec(MercyTriggerPacket::write, MercyTriggerPacket::new);

    public MercyTriggerPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
    }

    public static void handle(MercyTriggerPacket packet, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(packet.entityId);
        if (entity instanceof Sans a) {
            a.setMercyTriggered(true);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<MercyTriggerPacket> type() {
        return TYPE;
    }
}
