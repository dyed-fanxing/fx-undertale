package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 弹簧模型（加入质量）
 * 公式：
 *   displacement = targetPos - currentPos
 *   springForce = k * displacement
 *   acceleration = springForce / mass
 *   newVel = (currentVel + acceleration * dt) * damping
 *   (dt 隐含在每次 update 中，由调用者传入 deltaTime)
 * 能量：
 *   kinetic = 0.5 * mass * v^2
 *   potential = 0.5 * k * |displacement|^2
 *   total = kinetic + potential
 */
public class SpringMotionModel extends PhysicsMotionModel {
    private static final Logger log = LoggerFactory.getLogger(SpringMotionModel.class);
    private float frequency ;      // 弹簧刚度 k
    private float damping = 1.0f;         // 每 tick 速度缩放因子 (1.0 无阻尼, <1 衰减)
    private float mass;            // 质量，默认为 1.0

    private double springForce; // 最近一次计算的弹簧力大小
    private double potentialEnergy; // 最近一次计算的势能
    private double kineticEnergy;   // 最近一次计算的动能
    private double totalEnergy;     // 总能量

    // 构造函数（默认质量=1）
    public SpringMotionModel(float frequency) {
        this(frequency, 1f, 1.0f);
    }

    public SpringMotionModel(float frequency, float damping) {
        this(frequency, damping, 1.0f);
    }

    public SpringMotionModel(float frequency, float damping, float mass) {
        this.frequency = frequency;
        this.damping = damping;
        this.mass = mass > 0 ? mass : 1.0f; // 确保质量为正
    }

    public SpringMotionModel(CompoundTag tag) {
        this.frequency = tag.getFloat("frequency");
        this.damping = tag.getFloat("damping");
        this.mass = tag.contains("mass") ? tag.getFloat("mass") : 1.0f;
    }

    public SpringMotionModel(RegistryFriendlyByteBuf buf) {
        this.frequency = buf.readFloat();
        this.damping = buf.readFloat();
        this.mass = buf.readFloat();
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel,@Nullable Vec3 targetPos, @Nullable Vec3 targetVel,double deltaTime) {
        if (targetPos == null) {
            // 无目标时，仅按阻尼衰减速度
            return currentVel.scale(damping);
        }
        // 位移向量，将 Y 分量置零
        Vec3 displacement = targetPos.subtract(currentPos);
        // 弹簧力（大小与位移成正比）
        Vec3 springForce = displacement.scale(frequency);
        this.springForce = springForce.length();
        // 加速度 = 力 / 质量
        Vec3 acceleration = springForce.scale(1.0f / mass);
        // 新速度 = (当前速度 + 加速度 * dt) * 阻尼
        Vec3 newVel = currentVel.add(acceleration).scale(damping);
        // 计算能量（动能包含质量，势能不变）
        this.kineticEnergy = 0.5 * mass * newVel.lengthSqr();
        this.potentialEnergy = 0.5 * frequency * displacement.lengthSqr();
        this.totalEnergy = kineticEnergy + potentialEnergy;

        return newVel;
    }

    /**
     * 获取最近一次弹簧力的大小（绝对值）
     */
    public double getSpringForce() {
        return springForce;
    }

    /**
     * 获取最近一次计算的总机械能（动能 + 势能）
     */
    @Override
    public float getTotalEnergy() {
        return (float) totalEnergy;
    }

    /**
     * 获取动能
     */
    public double getKineticEnergy() {
        return kineticEnergy;
    }

    /**
     * 获取势能
     */
    public double getPotentialEnergy() {
        return potentialEnergy;
    }

    /**
     * 获取质量
     */
    public float getMass() {
        return mass;
    }

    /**
     * 设置质量（确保大于0）
     */
    public void setMass(float mass) {
        if (mass > 0) this.mass = mass;
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("frequency", frequency);
        tag.putFloat("damping", damping);
        tag.putFloat("mass", mass);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(frequency);
        buf.writeFloat(damping);
        buf.writeFloat(mass);
    }

    @Override
    protected String getType() {
        return "spring";
    }

    static {
        register("spring", SpringMotionModel::new, SpringMotionModel::new);
    }
}