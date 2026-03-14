package com.fanxing.fx_undertale.common.phys.motion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引力模型（固定强度）：加速度 = strength / (距离² + 软化半径²) * 方向指向目标
 * 当角动量为零时，物体沿直线振荡，振幅恒定（能量守恒）。
 */
public class GravityMotion extends AbstractPhysicsMotionModel {
    private static final Logger log = LoggerFactory.getLogger(GravityMotion.class);
    private float strength;   // 实际使用的引力强度（固定）
    private float damping = 1.0F;
    private float softening;

    /**
     * @param strength 强度
     * @param softening    软化半径
     */
    public GravityMotion(float strength,float softening) {
        this.strength = strength; // 线性比例，也可改为平方
        this.softening = softening;
    }

    /**
     * 根据实际距离进行缩放强度，使轨迹符合参考距离下调整的轨迹
     * @param baseStrength 基础强度
     * @param baseDis 距离
     * @param actualDis 真实距离
     */
    public GravityMotion(float baseStrength,float baseDis,float actualDis,float softening) {
        this.strength = baseStrength * (actualDis / baseDis); // 线性比例，也可改为平方
        this.softening = softening;
    }

    public GravityMotion(CompoundTag tag) {
        this.strength = tag.getFloat("strength");
        this.damping = tag.getFloat("damping");
        this.softening = tag.getFloat("softening");
    }

    public GravityMotion(RegistryFriendlyByteBuf buf) {
        this.strength = buf.readFloat();
        this.damping = buf.readFloat();
        this.softening = buf.readFloat();
    }

    @Override
    public Vec3 update(Vec3 currentPos, Vec3 currentVel, @Nullable Vec3 targetPos, @Nullable Vec3 targetVel, double deltaTime) {
        if (targetPos == null) {
            return currentVel;
        }

        Vec3 toTarget = targetPos.subtract(currentPos);
        double distSq = toTarget.lengthSqr();
        double softenedDistSq = distSq + softening * softening;
        double accelMag = strength / softenedDistSq;
        Vec3 acceleration = toTarget.normalize().scale(accelMag*0.05f);
        log.info("distSq：{}，softenedDistSq：{}，accelMag：{}，acceleration：{}",distSq,softenedDistSq,accelMag,acceleration);
        return currentVel.add(acceleration).scale(damping);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("strength", strength);
        tag.putFloat("damping", damping);
        tag.putFloat("softening", softening);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(strength);
        buf.writeFloat(damping);
        buf.writeFloat(softening);
    }

    public float getEffectiveStrength() { return strength; }
    public void setEffectiveStrength(float strength) { this.strength = strength; }
    public float getDamping() { return damping; }
    public void setDamping(float damping) { this.damping = damping; }
    public float getSoftening() { return softening; }
    public void setSoftening(float softening) { this.softening = softening; }

    @Override
    protected String getType() {
        return "gravity";
    }

    static {
        register("gravity",GravityMotion::new,GravityMotion::new);
    }
}