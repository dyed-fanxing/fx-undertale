package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.render.effect.EffectRendererHandler;
import com.fanxing.fx_undertale.client.render.effect.WarningTipAABB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public record WarningTipAABBPacket(double minX,double minY,double minZ,double maxX,double maxY,double maxZ,int lifetime,int color) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<WarningTipAABBPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_aabb_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipAABBPacket> STREAM_CODEC = CustomPacketPayload.codec(WarningTipAABBPacket::write, WarningTipAABBPacket::new);
    private static final Logger log = LogManager.getLogger(WarningTipAABBPacket.class);

    public WarningTipAABBPacket(int lifetime,int color,AABB aabb) {
        this(aabb.minX,aabb.minY,aabb.minZ, aabb.maxX,aabb.maxY,aabb.maxZ,lifetime,color);
    }
    public WarningTipAABBPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readVarInt(),buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.minX);
        buf.writeDouble(this.minY);
        buf.writeDouble(this.minZ);
        buf.writeDouble(this.maxX);
        buf.writeDouble(this.maxY);
        buf.writeDouble(this.maxZ);
        buf.writeVarInt(this.lifetime);
        buf.writeInt(this.color);
    }
    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(WarningTipAABBPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            EffectRendererHandler.addDecoration(new WarningTipAABB(
                    new AABB(packet.minX, packet.minY,packet.minZ, packet.maxX, packet.maxY, packet.maxZ),
                    packet.lifetime,packet.color
            ));
        });
    }
}
