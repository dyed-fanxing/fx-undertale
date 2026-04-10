package com.fanxing.fx_undertale.common.parametric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * 三角波曲线 - 沿 X 轴匀速往返运动
 * @param amplitude 振幅（运动范围 -amplitude 到 +amplitude）
 * @param speed     角速度（弧度/秒），周期 = 2π/speed
 */
public record TriangleWave(float amplitude, float speed) implements ParametricCurve {
    // 使用 mapCodec 构建 MapCodec<TriangleWave>
    public static final MapCodec<TriangleWave> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    Codec.FLOAT.fieldOf("amp").forGetter(TriangleWave::amplitude),
                    Codec.FLOAT.fieldOf("spd").forGetter(TriangleWave::speed)
            ).apply(inst, TriangleWave::new)
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, TriangleWave> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, TriangleWave curve) {
            buf.writeFloat(curve.amplitude());
            buf.writeFloat(curve.speed());
        }

        @Override
        public @NotNull TriangleWave decode(RegistryFriendlyByteBuf buf) {
            return new TriangleWave(buf.readFloat(), buf.readFloat());
        }
    };

    @Override
    public Function<Float, Vec3> function() {
        return t -> {
            // t 为归一化进度 [0,1)，自动循环
            double phase = t - Math.floor(t);
            double value01 = phase < 0.5 ? phase * 2 : 2 - phase * 2;
            double offset = (value01 * 2 - 1) * amplitude;
            return new Vec3(offset, 0, 0);
        };
    }
    static {
        ParametricCurve.register(TriangleWave.class, CODEC);           // NBT
        ParametricCurve.registerStream(TriangleWave.class, STREAM_CODEC); // 网络
    }
}