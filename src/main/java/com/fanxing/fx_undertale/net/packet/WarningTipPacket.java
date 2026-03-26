package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.effect.EffectRendererHandler;
import com.fanxing.fx_undertale.client.effect.WarningTip;
import com.fanxing.fx_undertale.utils.ByteBufUtils;
import com.fanxing.fx_undertale.utils.ParametricCurveType;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

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

    public WarningTipPacket(float x, float y, float z, int lifetime, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lifetime = lifetime;
        this.color = color;
    }

    public WarningTipPacket(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeVarInt(this.lifetime);
        buf.writeInt(this.color);
    }

    public static class Cylinder extends WarningTipPacket {
        public static final Type<Cylinder> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_cylinder_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cylinder> STREAM_CODEC = CustomPacketPayload.codec(Cylinder::write, Cylinder::new);

        private final float h;
        private final float r;
        private final Direction gravity;

        public Cylinder(float x, float y, float z, float r, float h, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.h = h;
            this.r = r;
            this.gravity = Direction.DOWN;
        }

        public Cylinder(float x, float y, float z, float r, float h, int lifetime, int color, Direction gravity) {
            super(x, y, z, lifetime, color);
            this.h = h;
            this.r = r;
            this.gravity = gravity;
        }

        public Cylinder(FriendlyByteBuf buf) {
            super(buf);
            this.h = buf.readFloat();
            this.r = buf.readFloat();
            this.gravity = buf.readEnum(Direction.class);
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeFloat(this.h);
            buf.writeFloat(this.r);
            buf.writeEnum(this.gravity);
        }

        public static void handle(Cylinder packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Cylinder(packet.x, packet.y, packet.z, packet.r, packet.h, packet.lifetime, packet.color, packet.gravity)));
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

        public Cube(float x, float y, float z, float length, float width, float height, float yaw, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.height = height;
            this.yaw = yaw;
        }

        public Cube(FriendlyByteBuf buf) {
            super(buf);
            this.length = buf.readFloat();
            this.width = buf.readFloat();
            this.height = buf.readFloat();
            this.yaw = buf.readFloat();
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeFloat(length);
            buf.writeFloat(width);
            buf.writeFloat(height);
            buf.writeFloat(yaw);
        }

        public static void handle(Cube packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Cube(packet.x, packet.y, packet.z, packet.length, packet.width, packet.height, packet.yaw, packet.lifetime, packet.color)));
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

        public Quad(float x, float y, float z, float length, float width, float yaw, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.yaw = yaw;
        }

        public Quad(FriendlyByteBuf buf) {
            super(buf);
            this.length = buf.readFloat();
            this.width = buf.readFloat();
            this.yaw = buf.readFloat();
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeFloat(length);
            buf.writeFloat(width);
            buf.writeFloat(yaw);
        }

        public static void handle(Quad packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Quad(packet.x, packet.y, packet.z, packet.length, packet.width, packet.yaw, packet.lifetime, packet.color)));
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

        public Circle(float x, float y, float z, float radius, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
        }

        public Circle(FriendlyByteBuf buf) {
            super(buf);
            this.radius = buf.readFloat();
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeFloat(radius);
        }

        public static void handle(Circle packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.Circle(packet.x, packet.y, packet.z, packet.radius, packet.lifetime, packet.color)));
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class CurveStrip extends WarningTipPacket {
        public static final Type<CurveStrip> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_curve_strip_packet"));
        public static final StreamCodec<FriendlyByteBuf, CurveStrip> STREAM_CODEC = CustomPacketPayload.codec(CurveStrip::write, CurveStrip::new);
        private final float radius;
        private final float width;
        private final float yaw;
        private final int segments;
        private final ParametricCurveType curveType;
        private final float[] params;

        public CurveStrip(float x, float y, float z, int lifetime, int color, float radius, float width, float yaw, int segments, ParametricCurveType curveType, float... params) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
            this.width = width;
            this.yaw = yaw;
            this.segments = segments;
            this.curveType = curveType;
            this.params = params;
        }

        public CurveStrip(FriendlyByteBuf buf) {
            super(buf);
            this.radius = buf.readFloat();
            this.width = buf.readFloat();
            this.yaw = buf.readFloat();
            this.segments = buf.readInt();
            this.curveType = buf.readEnum(ParametricCurveType.class);
            this.params = new float[buf.readInt()];
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeFloat(radius);
            buf.writeFloat(width);
            buf.writeFloat(yaw);
            buf.writeInt(segments);
            buf.writeEnum(curveType);
            ByteBufUtils.writeFloatArray(buf, params);
        }

        public static void handle(CurveStrip packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTip.CurveStripPrecession(packet.x, packet.y, packet.z, packet.lifetime, packet.color, packet.radius, packet.width, packet.yaw, packet.segments, packet.curveType.create(packet.params)) {
            }));
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * 径向均匀分布的多个曲线条带包
     * 服务端发送一次，客户端自动生成 curveCount 个均匀旋转的条带
     */
    public static class RadialPrecessionCurveStripsPacket extends WarningTipPacket {
        public static final Type<RadialPrecessionCurveStripsPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "radial_curve_strips_packet")
        );
        public static final StreamCodec<FriendlyByteBuf, RadialPrecessionCurveStripsPacket> STREAM_CODEC = CustomPacketPayload.codec(RadialPrecessionCurveStripsPacket::write, RadialPrecessionCurveStripsPacket::new);
        private final int count;                 // 条带数量
        private final float radius;
        private final float width;
        private final int segments;
        private final ParametricCurveType curveType;  // 已包含正向/反向
        private final float[] params;

        public RadialPrecessionCurveStripsPacket(float x, float y, float z, int lifetime, int color, int count, float radius, float width, int segments, ParametricCurveType curveType, float... params) {
            super(x, y, z, lifetime, color);
            this.count = count;
            this.radius = radius;
            this.width = width;
            this.segments = segments;
            this.curveType = curveType;
            this.params = params;
        }

        public RadialPrecessionCurveStripsPacket(FriendlyByteBuf buf) {
            super(buf);
            this.count = buf.readVarInt();
            this.radius = buf.readFloat();
            this.width = buf.readFloat();
            this.segments = buf.readVarInt();
            this.curveType = buf.readEnum(ParametricCurveType.class);
            this.params = ByteBufUtils.readFloatArray(buf);
        }

        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeVarInt(count);
            buf.writeFloat(radius);
            buf.writeFloat(width);
            buf.writeVarInt(segments);
            buf.writeEnum(curveType);
            ByteBufUtils.writeFloatArray(buf, params);
        }

        public static void handle(RadialPrecessionCurveStripsPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                Function<Float, Vec3> baseCurve = packet.curveType.create(packet.params);
                for (int s = 0; s < packet.count; s++) {
                    float yaw = s * 360f / packet.count;   // 均匀分布角度
                    EffectRendererHandler.addDecoration(new WarningTip.CurveStripPrecession(
                            packet.x, packet.y, packet.z,
                            packet.lifetime, packet.color,
                            packet.radius, packet.width, yaw,
                            packet.segments, baseCurve
                    ));
                }
            });
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
