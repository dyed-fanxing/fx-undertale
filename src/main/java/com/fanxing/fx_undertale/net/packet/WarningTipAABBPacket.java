package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.effect.EffectRendererHandler;
import com.fanxing.fx_undertale.client.effect.WarningTipAABB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author Sakpeipei
 * @since 2025/11/18 14:10
 */
public record WarningTipAABBPacket(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int lifetime, Color color) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WarningTipAABBPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_aabb_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipAABBPacket> STREAM_CODEC = CustomPacketPayload.codec(WarningTipAABBPacket::write, WarningTipAABBPacket::new);

    public WarningTipAABBPacket(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int lifetime, Color color) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.lifetime = lifetime;
        this.color = color;
    }
    public WarningTipAABBPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readVarInt(),new Color(buf.readInt()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.minX);
        buf.writeDouble(this.minY);
        buf.writeDouble(this.minZ);
        buf.writeDouble(this.maxX);
        buf.writeDouble(this.maxY);
        buf.writeDouble(this.maxZ);
        buf.writeVarInt(this.lifetime);
        buf.writeInt(this.color.getRGB());
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
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
