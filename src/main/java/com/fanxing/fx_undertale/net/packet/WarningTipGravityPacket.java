package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.render.effect.WarningTipGravity;
import com.fanxing.lib.client.render.effect.EffectRendererHandler;
import com.fanxing.lib.net.packet.WarningTipPacket;
import com.fanxing.lib.util.ParametricCurveType;
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
public class WarningTipGravityPacket {
    public static class Cylinder extends WarningTipPacket.Cylinder {
        public static final Type<WarningTipGravityPacket.Cylinder> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "warning_tip_cylinder_gravity_packet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WarningTipGravityPacket.Cylinder> STREAM_CODEC = CustomPacketPayload.codec(WarningTipGravityPacket.Cylinder::write, WarningTipGravityPacket.Cylinder::new);
        public final Direction gravity;

        public Cylinder(float x, float y, float z, float r, float h, int lifetime, int color) {
            super(x, y, z, r, h, lifetime, color);
            this.gravity = Direction.DOWN;
        }

        public Cylinder(float x, float y, float z, float r, float h, int lifetime, int color, Direction gravity) {
            super(x, y, z, r, h, lifetime, color);
            this.gravity = gravity;
        }

        public Cylinder(FriendlyByteBuf buf) {
            super(buf);
            this.gravity = buf.readEnum(Direction.class);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeEnum(this.gravity);
        }


        public static void handle(WarningTipGravityPacket.Cylinder packet, IPayloadContext context) {
            context.enqueueWork(() -> EffectRendererHandler.addDecoration(new WarningTipGravity.Cylinder(packet.x, packet.y, packet.z, packet.r, packet.h, packet.lifetime, packet.color, packet.gravity)));
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class RadialPrecessionCurveStripsGravityPacket extends WarningTipPacket.RadialPrecessionCurveStripsPacket {
        public static final Type<RadialPrecessionCurveStripsGravityPacket> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "radial_precession_curve_strips_gravity_packet")
        );
        public static final StreamCodec<FriendlyByteBuf, RadialPrecessionCurveStripsGravityPacket> STREAM_CODEC = CustomPacketPayload.codec(RadialPrecessionCurveStripsGravityPacket::write, RadialPrecessionCurveStripsGravityPacket::new);

        protected final Direction gravity;

        public RadialPrecessionCurveStripsGravityPacket(float x, float y, float z, int lifetime, int color, int count, float radius, float width, int segments, Direction gravity, ParametricCurveType curveType, float... params) {
            super(x, y, z, lifetime, color, count, radius, width, segments, curveType, params);
            this.gravity = gravity;
        }

        public RadialPrecessionCurveStripsGravityPacket(FriendlyByteBuf buf) {
            super(buf);
            this.gravity = buf.readEnum(Direction.class);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            super.write(buf);
            buf.writeEnum(gravity);
        }

        public static void handle(RadialPrecessionCurveStripsGravityPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                Function<Float, Vec3> baseCurve = packet.curveType.create(packet.params);
                for (int s = 0; s < packet.count; s++) {
                    float yaw = s * 360f / packet.count;   // 均匀分布角度
                    EffectRendererHandler.addDecoration(new WarningTipGravity.CurveStripPrecessionGravity(
                            packet.x, packet.y, packet.z,
                            packet.lifetime, packet.color,
                            packet.radius, packet.width, yaw,
                            packet.segments, baseCurve, packet.gravity
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
