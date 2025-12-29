package com.sakpeipei.mod.undertale.network;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.event.handler.DecorationRendererHandler;
import com.sakpeipei.mod.undertale.client.render.decoration.WarningTip;
import com.sakpeipei.mod.undertale.client.render.decoration.WarningTipAABB;
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

/**
 * @author yujinbao
 * @since 2025/11/18 14:10
 */
public record WarningTipPacket(float x,float y,float z,float r, float h,int lifetime, int color) implements CustomPacketPayload {
    public static final Type<WarningTipPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "warning_tip_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipPacket> STREAM_CODEC = CustomPacketPayload.codec(WarningTipPacket::write, WarningTipPacket::new);
    private static final Logger log = LogManager.getLogger(WarningTipPacket.class);

    public WarningTipPacket(float x,float y,float z,float r, float h,int lifetime, int color) {
        this.x = x;this.y = y;this.z = z;
        this.r = r;this.h = h;
        this.lifetime = lifetime;
        this.color = color;
    }
    public WarningTipPacket(FriendlyByteBuf buf) {
        this(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(this.r);
        buf.writeFloat(this.h);
        buf.writeVarInt(this.lifetime);
        buf.writeInt(this.color);
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(WarningTipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            DecorationRendererHandler.addDecoration(new WarningTip(packet.r,packet.h, packet.lifetime,packet.color));
        });
    }
}
