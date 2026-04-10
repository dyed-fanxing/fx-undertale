package com.fanxing.fx_undertale.common.phys.motion;

import com.fanxing.fx_undertale.net.packet.SyncMotionPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 物理运动模型
 * 实体通过组合此模型来实现不同的运动行为
 * 由于物理运动每Tick都要计算，因为是运动连续的，所以不需要判脏数据，如果需要同步则每Tick都得同步
 */
public class PhysicsMotionModel {
    public PhysicsMotionModel() {
    }
    public PhysicsMotionModel(CompoundTag tag) {
    }
    public PhysicsMotionModel(RegistryFriendlyByteBuf buf) {
    }

    /**
     * 每帧更新运动
     *
     * @param currentPos    实体当前位置
     * @param currentVel    实体当前速度
     * @param targetPos     目标当前位置
     * @param ticks         持续的tick数
     * @return 更新后的速度向量（实体应调用 setDeltaMovement）
     */
    public Vec3 update(Vec3 currentPos, Vec3 currentVel,@Nullable Vec3 targetPos,int ticks){
        return currentVel;
    }

    /**
     * 保存状态到NBT（用于实体存档）
     */
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("motionType", getClassKey(this.getClass()));
    }
    // NBT加载，需要子类实现根据Tag构造

    /**
     * 写入生成数据包（用于客户端同步）
     */
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(getClassKey(this.getClass()));
    }

    // 读取生成数据包，需要子类实现根据Buf去写构造方法

    protected void writeSyncData(FriendlyByteBuf buf) {
    }

    protected void readSyncData(FriendlyByteBuf buf) {
    }


    public void sync(Entity entity) {
        if (!entity.level().isClientSide) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            writeSyncData(buf);
            if(buf.writerIndex() > 0){
                byte[] data = new byte[buf.writerIndex()];
                buf.readBytes(data);
                PacketDistributor.sendToPlayersTrackingEntity(entity, new SyncMotionPayload(entity.getId(), data));
            }
        }
    }

    public void readSyncData(byte[] bytes) {
        readSyncData(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
    }


    public float getTotalEnergy(){
        return 0f;
    }


    // 注册表：类型标识 -> 工厂（提供从 Tag 和 Buf 创建实例的方法）
    private static final Map<String, MotionModelFactory> REGISTRY = new HashMap<>();





    // 工厂接口
    private interface MotionModelFactory {
        PhysicsMotionModel fromTag(CompoundTag tag);
        PhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf);
    }

    // 注册方法，由子类在静态初始化块中调用
    protected static<T extends PhysicsMotionModel>  void register(Class<T> clazz) {
        String key = getClassKey(clazz);
        try {
            java.lang.reflect.Constructor<T> tagCtor = clazz.getDeclaredConstructor(CompoundTag.class);
            java.lang.reflect.Constructor<T> bufCtor = clazz.getDeclaredConstructor(RegistryFriendlyByteBuf.class);
            REGISTRY.put(key, new MotionModelFactory() {
                @Override
                public PhysicsMotionModel fromTag(CompoundTag tag) {
                    try {
                        return tagCtor.newInstance(tag);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create " + clazz + " from tag", e);
                    }
                }
                @Override
                public PhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf) {
                    try {
                        return bufCtor.newInstance(buf);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create " + clazz + " from buf", e);
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(clazz + " must have constructors: (CompoundTag) and (RegistryFriendlyByteBuf)", e);
        }
    }

    // 从 NBT 反序列化
    public static PhysicsMotionModel fromTag(CompoundTag tag) {
        if(tag.contains("motionType")){
            String type = tag.getString("motionType");
            MotionModelFactory factory = REGISTRY.get(type);
            if (factory == null) {
                throw new IllegalArgumentException("Unknown motion model type: " + type);
            }
            return factory.fromTag(tag);
        }
        return new PhysicsMotionModel();
    }

    // 从网络缓冲区反序列化
    public static PhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf) {
        String type = buf.readUtf();
        MotionModelFactory factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown motion model type: " + type);
        }
        return factory.fromBuf(buf);
    }


    private static String getClassKey(Class<?> clazz) {
        String name = clazz.getSimpleName();
        int idx = name.indexOf("Motion");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        // 转小驼峰：首字母小写
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }



    public static void registry() {
        register(PhysicsMotionModel.class);
        register(SpringMotionModel.class);
        register(RoseSpiralMotionModel.class);
        register(CircularMotionModel.class);
        register(ProportionalNavigationModel.class);
        register(GravityMotionModel.class);
    }
}