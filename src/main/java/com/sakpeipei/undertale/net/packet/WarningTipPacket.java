package com.sakpeipei.undertale.net.packet;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.render.effect.EffectRendererHandler;
import com.sakpeipei.undertale.client.render.effect.WarningTip;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakpeipei
 * @since 2025/11/18 14:10
 */
public record WarningTipPacket(float x, float y, float z, float r, float h, int lifetime, int color, Direction gravity) implements CustomPacketPayload {
    public static final Type<WarningTipPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "warning_tip_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipPacket> STREAM_CODEC = CustomPacketPayload.codec(WarningTipPacket::write, WarningTipPacket::new);
    public WarningTipPacket(float x, float y, float z, float r, float h, int lifetime, int color) {
        this(x,y,z,r,h,lifetime,color,Direction.DOWN);
    }

    public WarningTipPacket(FriendlyByteBuf buf) {
        this(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readInt(),buf.readEnum(Direction.class));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(this.r);
        buf.writeFloat(this.h);
        buf.writeVarInt(this.lifetime);
        buf.writeInt(this.color);
        buf.writeEnum(this.gravity);
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(WarningTipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip(packet.x, packet.y, packet.z, packet.r, packet.h, packet.lifetime, packet.color,packet.gravity)));
    }
}
