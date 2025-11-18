package com.sakpeipei.mod.undertale.network;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.event.handler.DecorationRendererHandler;
import com.sakpeipei.mod.undertale.client.render.decoration.WarningTip;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author yujinbao
 * @since 2025/11/18 14:10
 */
public record WarningTipPacket(double minX,double minY,double minZ,double maxX,double maxY,double maxZ,int lifetime,int color) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WarningTipPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "warning_tip_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipPacket> STREAM_CODEC = CustomPacketPayload.codec(WarningTipPacket::write, WarningTipPacket::new);

    public WarningTipPacket(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int lifetime,int color) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.lifetime = lifetime;
        this.color = color;
    }
    public WarningTipPacket(FriendlyByteBuf buf) {
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
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(WarningTipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            DecorationRendererHandler.addDecoration(new WarningTip(
                    new AABB(packet.minX, packet.minY,packet.minZ, packet.maxX, packet.maxY, packet.maxZ),
                    packet.lifetime,packet.color
            ));
        });
    }
}
