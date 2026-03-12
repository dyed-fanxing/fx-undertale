package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.IGasterBlaster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakpeipei
 * @since 2026/1/6 14:35
 * GB炮光束终点位置
 */
public record GasterBlasterBeamEndPacket(int entityId, double x, double y, double z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GasterBlasterBeamEndPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "gaster_blaster_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasterBlasterBeamEndPacket> STREAM_CODEC = CustomPacketPayload.codec(GasterBlasterBeamEndPacket::write, GasterBlasterBeamEndPacket::new);

    public GasterBlasterBeamEndPacket(int entityId, Vec3 vec3) {
        this(entityId, vec3.x, vec3.y, vec3.z);
    }
    public GasterBlasterBeamEndPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readDouble(),buf.readDouble(),buf.readDouble());
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    public static void handle(GasterBlasterBeamEndPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof IGasterBlaster entity) {
//                entity.setEnd(new Vec3(packet.x, packet.y, packet.z));
            }
        });
    }

    @Override
    public @NotNull Type<GasterBlasterBeamEndPacket> type() {
        return TYPE;
    }
}
