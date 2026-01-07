package com.sakpeipei.undertale.entity.attachment;

import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sakpeipei.undertale.common.LocalDirection;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * @author yujinbao
 * @since 2025/11/14 10:17
 * 重力方向数据 - 存储实体的重力状态
 */
public class GravityData {
    // 控制方向
    private Vec3 vec3 = new Vec3(0, -1, 0);
    private Vec3 previousGravityDirection = new Vec3(0, -1, 0);
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 10;
    /**
     * 是否激活重力，标记控制是否为重力控制
     * 若为true，则会改变目标姿态和局部重力
     * 若为false，则只是给予目标冲量
     */
    private boolean active;
    // Codec用于序列化，支持网络同步
    public static final Codec<GravityData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Vec3.CODEC.fieldOf("vec3").forGetter(data -> data.vec3),
                    Vec3.CODEC.fieldOf("previousGravityDirection").forGetter(data -> data.previousGravityDirection),
                    Codec.INT.fieldOf("transitionTicks").forGetter(data -> data.transitionTicks)
            ).apply(instance, GravityData::new)
    );

    // 构造方法
    public GravityData() {}

    public GravityData(Vec3 vec3, Vec3 previousGravityDirection, int transitionTicks) {
        this.vec3 = vec3;
        this.previousGravityDirection = previousGravityDirection;
        this.transitionTicks = transitionTicks;
    }

    // Getter 和 Setter 方法
    public Vec3 getVec3() {
        return vec3;
    }
    public boolean isActive() {
        return active;
    }

    public void setGravityDirection(Vec3 direction) {
        this.previousGravityDirection = this.vec3;
        this.vec3 = direction.normalize();
        this.transitionTicks = TRANSITION_DURATION;
    }

    public Vec3 getPreviousGravityDirection() {
        return previousGravityDirection;
    }

    /**
     * 基于攻击者朝向设置相对重力方向
     */
    public void setRelativeGravity(Entity attacker, LocalDirection localDirection) {
        switch (localDirection) {
            case FRONT -> setGravityDirection(attacker.getLookAngle().normalize());
            case BACK -> setGravityDirection(attacker.getLookAngle().reverse().normalize());
            case LEFT -> {
                Vec3 lookVec = attacker.getLookAngle();
                setGravityDirection(new Vec3(-lookVec.z, lookVec.y, lookVec.x).normalize());
            }
            case RIGHT -> {
                Vec3 lookVec = attacker.getLookAngle();
                setGravityDirection(new Vec3(lookVec.z, lookVec.y, -lookVec.x).normalize());
            }
            case UP -> setGravityDirection(new Vec3(0, 1, 0));
            case DOWN -> setGravityDirection(new Vec3(0, -1, 0));
        }
    }

    /**
     * 对目标实体应用攻击者的相对重力
     */
    public static void applyRelativeGravity(Entity attacker, Entity target, LocalDirection direction) {
        target.getData(AttachmentTypeRegistry.GRAVITY).setRelativeGravity(attacker, direction);
    }

    /**
     * 获取从默认重力到当前重力的旋转四元数
     */
    public Quaternionf getRotationQuaternion() {
        if (transitionTicks > 0) {
            float progress = 1.0f - (transitionTicks / (float) TRANSITION_DURATION);
            Vec3 interpolated = lerp(previousGravityDirection, vec3, progress);
            return getRotationFromTo(new Vec3(0, -1, 0), interpolated);
        }
        return getRotationFromTo(new Vec3(0, -1, 0), vec3);
    }

    public void tick() {
        if (transitionTicks > 0) {
            transitionTicks--;
        }
    }

    /**
     * 计算从方向A旋转到方向B所需的四元数
     */
    private Quaternionf getRotationFromTo(Vec3 from, Vec3 to) {
        from = from.normalize();
        to = to.normalize();

        if (from.equals(to)) {
            return new Quaternionf(0, 0, 0, 1);
        }

        if (from.equals(to.reverse())) {
            return Axis.ZP.rotationDegrees(180);
        }

        Vec3 axis = from.cross(to).normalize();
        double dot = Math.max(-1, Math.min(1, from.dot(to)));
        double angle = Math.acos(dot);

        Quaternionf rotation = new Quaternionf();
        rotation.setAngleAxis((float)angle, (float)axis.x, (float)axis.y, (float)axis.z);
        return rotation;
    }

    /**
     * 线性插值计算
     */
    private Vec3 lerp(Vec3 start, Vec3 end, float progress) {
        double x = start.x + (end.x - start.x) * progress;
        double y = start.y + (end.y - start.y) * progress;
        double z = start.z + (end.z - start.z) * progress;
        return new Vec3(x, y, z);
    }

    /**
     * 重置为默认重力（向下）
     */
    public void reset() {
        setGravityDirection(new Vec3(0, -1, 0));
    }

    /**
     * 获取过渡进度（0.0到1.0）
     */
    public float getTransitionProgress() {
        if (transitionTicks <= 0) return 1.0f;
        return 1.0f - (transitionTicks / (float) TRANSITION_DURATION);
    }

    /**
     * 检查是否正在过渡中
     */
    public boolean isTransitioning() {
        return transitionTicks > 0;
    }
}