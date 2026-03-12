package com.fanxing.fx_undertale.particle.options;

//public record LifeTimeOption implements ParticleOptions {
//    public static final MapCodec<GrowOption> MAP_CODEC = RecordCodecBuilder.mapCodec(
//            instance -> instance.group(
//                    Codec.FLOAT.fieldOf("size").forGetter(GrowOption::size),
//                    Codec.SHORT.fieldOf("lifetime").forGetter(GrowOption::lifetime)
//            ).apply(instance, GrowOption::new)
//    );
//
//    // 网络流 解码/编码，用于服务端 客户端传输数据，开销小，网络传输用，服务端传输用
//    public static final StreamCodec<RegistryFriendlyByteBuf, GrowOption> STREAM_CODEC =
//            StreamCodec.of(
//                    (buf, option) -> {
//                        buf.writeFloat(option.size);
//                        buf.writeShort(option.lifetime);
//                    },
//                    buf -> new GrowOption(buf.readFloat(), buf.readShort())
//            );
//
//
//    @Override
//    public @NotNull ParticleType<GrowOption> getType() {
//        return ParticleRegistry.HALO_SCALE.get();
//    }
//}
