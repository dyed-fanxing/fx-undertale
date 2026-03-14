package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.effect.EffectRendererHandler;
import com.fanxing.fx_undertale.client.effect.WarningTip;
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
public abstract class WarningTipPacket implements CustomPacketPayload {
    protected float x;
    protected float y;
    protected float z;
    protected int lifetime;
    protected int color;

    public WarningTipPacket(float x, float y, float z,int lifetime, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lifetime = lifetime;
        this.color = color;
    }

    public static class Cylinder extends WarningTipPacket {
        public static final Type<Cylinder> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_cylinder_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cylinder> STREAM_CODEC = CustomPacketPayload.codec(Cylinder::write, Cylinder::new);

        private final float h;
        private final float r;
        private final Direction gravity;

        public Cylinder(float x, float y, float z,float r, float h,int lifetime, int color) {
            super(x, y, z,lifetime, color);
            this.h = h;
            this.r = r;
            this.gravity = Direction.DOWN;
        }

        public Cylinder(float x, float y, float z,float r, float h,int lifetime, int color,Direction gravity) {
            super(x, y, z,lifetime, color);
            this.h = h;
            this.r = r;
            this.gravity = gravity;
        }
        public Cylinder(FriendlyByteBuf buf) {
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

        public static void handle(Cylinder packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Cylinder(packet.x, packet.y, packet.z, packet.r, packet.h, packet.lifetime, packet.color,packet.gravity)));
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class Cube extends WarningTipPacket {
        public static final Type<Cube> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_cube_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cube> STREAM_CODEC = CustomPacketPayload.codec(Cube::write, Cube::new);


        private final float length;
        private final float width;
        private final float height;
        private final float yaw;
        public Cube(float x, float y, float z,float length,float width,float height,float yaw, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.height = height;
            this.yaw = yaw;
        }
        public Cube(FriendlyByteBuf buf) {
            this(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(x);
            buf.writeFloat(y);
            buf.writeFloat(z);
            buf.writeFloat(length);
            buf.writeFloat(width);
            buf.writeFloat(height);
            buf.writeFloat(yaw);
            buf.writeVarInt(this.lifetime);
            buf.writeInt(this.color);
        }
        public static void handle(Cube packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Cube(packet.x, packet.y, packet.z, packet.length, packet.width,packet.height,packet.yaw, packet.lifetime, packet.color)));
        }
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class Quad extends WarningTipPacket {
        public static final Type<Quad> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_quad_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Quad> STREAM_CODEC = CustomPacketPayload.codec(Quad::write, Quad::new);


        private final float length;
        private final float width;
        private final float yaw;
        public Quad(float x, float y, float z,float length,float width,float yaw, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.yaw = yaw;
        }
        public Quad(FriendlyByteBuf buf) {
            this(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(x);
            buf.writeFloat(y);
            buf.writeFloat(z);
            buf.writeFloat(length);
            buf.writeFloat(width);
            buf.writeFloat(yaw);
            buf.writeVarInt(this.lifetime);
            buf.writeInt(this.color);
        }
        public static void handle(Quad packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Quad(packet.x, packet.y, packet.z, packet.length, packet.width,packet.yaw, packet.lifetime, packet.color)));
        }
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    public static class Circle extends WarningTipPacket {
        public static final Type<Circle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_circle_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Circle> STREAM_CODEC = CustomPacketPayload.codec(Circle::write, Circle::new);

        private final float radius;
        public Circle(float x, float y, float z,float radius,int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
        }
        public Circle(FriendlyByteBuf buf) {
            this(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readInt());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(x);
            buf.writeFloat(y);
            buf.writeFloat(z);
            buf.writeFloat(radius);
            buf.writeVarInt(this.lifetime);
            buf.writeInt(this.color);
        }
        public static void handle(Circle packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Circle(packet.x, packet.y, packet.z, packet.radius,packet.lifetime, packet.color)));
        }
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
