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
public abstract class AbstractPhysicsMotionModel {
    /**
     * 每帧更新运动
     *
     * @param currentPos    实体当前位置
     * @param currentVel    实体当前速度
     * @param targetPos     目标当前位置
     * @param targetVel     目标当前速度
     * @param time          t时间
     * @return 更新后的速度向量（实体应调用 setDeltaMovement）
     */
    public abstract Vec3 update(Vec3 currentPos, Vec3 currentVel,@Nullable Vec3 targetPos, @Nullable Vec3 targetVel,double time);

    /**
     * 保存状态到NBT（用于实体存档）
     */
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("motionType", getType());
    }
    // NBT加载，需要子类实现根据Tag构造

    /**
     * 写入生成数据包（用于客户端同步）
     */
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(getType());
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


    protected abstract String getType();




    // 注册表：类型标识 -> 工厂（提供从 Tag 和 Buf 创建实例的方法）
    private static final Map<String, MotionModelFactory> REGISTRY = new HashMap<>();

    // 工厂接口
    private interface MotionModelFactory {
        AbstractPhysicsMotionModel fromTag(CompoundTag tag);
        AbstractPhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf);
    }

    // 注册方法，由子类在静态初始化块中调用
    protected static void register(String type,Function<CompoundTag, AbstractPhysicsMotionModel> tagFactory,Function<RegistryFriendlyByteBuf, AbstractPhysicsMotionModel> bufFactory) {
        REGISTRY.put(type, new MotionModelFactory() {
            @Override
            public AbstractPhysicsMotionModel fromTag(CompoundTag tag) {
                return tagFactory.apply(tag);
            }
            @Override
            public AbstractPhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf) {
                return bufFactory.apply(buf);
            }
        });
    }

    // 从 NBT 反序列化
    public static AbstractPhysicsMotionModel fromTag(CompoundTag tag) {
        if(tag.contains("motionType")){
            String type = tag.getString("motionType");
            MotionModelFactory factory = REGISTRY.get(type);
            if (factory == null) {
                throw new IllegalArgumentException("Unknown motion model type: " + type);
            }
            return factory.fromTag(tag);
        }
        return null;
    }

    // 从网络缓冲区反序列化
    public static AbstractPhysicsMotionModel fromBuf(RegistryFriendlyByteBuf buf) {
        String type = buf.readUtf();
        MotionModelFactory factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown motion model type: " + type);
        }
        return factory.fromBuf(buf);
    }



}