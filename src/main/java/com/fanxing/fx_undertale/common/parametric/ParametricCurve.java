package com.fanxing.fx_undertale.common.parametric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface ParametricCurve {

    Map<String, StreamCodec<RegistryFriendlyByteBuf, ? extends ParametricCurve>> STREAM_CODEC_REGISTRY = new HashMap<>();
    Map<String, MapCodec<? extends ParametricCurve>> CODE_REGISTRY = new HashMap<>();
    // 多态 Codec：输出 { "type": "triangleWave", "amp":1, "spd":1 }
    Codec<ParametricCurve> CODEC = Codec.STRING.dispatch(
            "type",
            curve -> getClassLowerCase(curve.getClass()),
            ParametricCurve::get
    );
    // 多态 StreamCodec
    StreamCodec<RegistryFriendlyByteBuf, ParametricCurve> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, ParametricCurve curve) {
            String typeId = getClassLowerCase(curve.getClass());
            buf.writeUtf(typeId);                         // 先写类型 ID
            @SuppressWarnings("unchecked")
            StreamCodec<RegistryFriendlyByteBuf, ParametricCurve> codec =
                    (StreamCodec<RegistryFriendlyByteBuf, ParametricCurve>) STREAM_CODEC_REGISTRY.get(typeId);
            if (codec == null) throw new IllegalStateException("No stream codec for " + typeId);
            codec.encode(buf, curve);                     // 委托子类 codec 编码参数
        }
        @Override
        public @NotNull ParametricCurve decode(RegistryFriendlyByteBuf buf) {
            String typeId = buf.readUtf();
            StreamCodec<RegistryFriendlyByteBuf, ? extends ParametricCurve> codec = STREAM_CODEC_REGISTRY.get(typeId);
            if (codec == null) throw new IllegalStateException("No stream codec for " + typeId);
            return codec.decode(buf);
        }
    };

    static <T extends ParametricCurve> void registerStream(Class<T> clazz, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        STREAM_CODEC_REGISTRY.put(getClassLowerCase(clazz), codec);
    }

    // 注册：将类名首字母小写作为 ID（小驼峰）
    static void register(Class<? extends ParametricCurve> clazz, MapCodec<? extends ParametricCurve> codec) {
        CODE_REGISTRY.put(getClassLowerCase(clazz), codec);
    }


    static MapCodec<? extends ParametricCurve> get(String id) {
        MapCodec<? extends ParametricCurve> codec = CODE_REGISTRY.get(id);
        if (codec == null) throw new IllegalArgumentException("Unknown curve: " + id);
        return codec;
    }




    default void save(CompoundTag tag) {
        CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(error -> {})
                .ifPresent(nbt -> tag.put("curve", nbt));
    }
    // 静态加载方法

    static ParametricCurve read(CompoundTag tag) {return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> {})
                .orElseGet(() -> new TriangleWave(1f, 1f)); // 默认曲线
    }


    static String getClassLowerCase(Class<? extends ParametricCurve> clazz){
        String name = clazz.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    Function<Float, Vec3> function();
}
