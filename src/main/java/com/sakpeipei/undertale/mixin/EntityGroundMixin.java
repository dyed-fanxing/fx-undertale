package com.sakpeipei.undertale.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yujinbao
 * @since 2025/11/14 10:46
 * Entity重力相关方法
 */
@Mixin(Entity.class)
public abstract class EntityGroundMixin {
    protected abstract Vec3 collide(Vec3 movement);

    @Shadow public boolean horizontalCollision;
    @Shadow public boolean verticalCollision;
    @Shadow public boolean verticalCollisionBelow;

    /**
     * 修改碰撞标志计算 - 这是最关键的第一步！
     * 原版代码位置：在profiler.pop()之后，设置碰撞标志之前
     */
    @Inject(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;",
            ordinal = 1, shift = At.Shift.AFTER))
    private void onCalculateCollisionFlags(MoverType type, Vec3 movement, CallbackInfo ci,
                                           @Local(ordinal = 1) Vec3 vec3) { // vec3是collide返回的实际移动向量

        Entity self = (Entity)(Object)this;
        if (self.hasData(AttachmentTypeRegistry.GRAVITY)) {
            GravityData gravityData = self.getData(AttachmentTypeRegistry.GRAVITY);
            if (gravityData.isActive()) {
                Vec3 gravityDir = gravityData.getVec3();

                // 如果不是默认向下重力，需要修改碰撞判断
                if (!gravityDir.equals(new Vec3(0, -1, 0))) {
                    // 1. 计算在重力方向上的投影
                    double intendedInGravity = movement.dot(gravityDir);
                    double actualInGravity = vec3.dot(gravityDir);

                    // 2. 计算垂直于重力方向的分量
                    Vec3 intendedPerpendicular = movement.subtract(gravityDir.scale(intendedInGravity));
                    Vec3 actualPerpendicular = vec3.subtract(gravityDir.scale(actualInGravity));

                    // 3. 垂直碰撞：重力方向上的差异
                    this.verticalCollision = Math.abs(intendedInGravity - actualInGravity) > 1.0E-7;

                    // 4. 下方垂直碰撞：在重力方向上移动且被阻挡
                    this.verticalCollisionBelow = this.verticalCollision && intendedInGravity > 0.0;

                    // 5. 水平碰撞：垂直于重力方向上的差异
                    // 使用epsilon比较，避免浮点误差
                    boolean xDiff = Math.abs(intendedPerpendicular.x - actualPerpendicular.x) > 1.0E-7;
                    boolean yDiff = Math.abs(intendedPerpendicular.y - actualPerpendicular.y) > 1.0E-7;
                    boolean zDiff = Math.abs(intendedPerpendicular.z - actualPerpendicular.z) > 1.0E-7;
                    this.horizontalCollision = xDiff || yDiff || zDiff;

                    // 6. 轻微水平碰撞（暂时用原版逻辑，后面可以修改）
                    if (this.horizontalCollision) {
                        // 这里可以添加自定义的轻微碰撞判断

                    }
                    System.out.println("=== 自定义重力碰撞判断 ===");
                    System.out.println("重力方向: " + gravityDir);
                    System.out.println("预期移动: " + movement);
                    System.out.println("实际移动: " + vec3);
                    System.out.println("重力方向分量 - 预期: " + intendedInGravity + ", 实际: " + actualInGravity);
                    System.out.println("垂直碰撞: " + this.verticalCollision);
                    System.out.println("下方碰撞: " + this.verticalCollisionBelow);
                    System.out.println("水平碰撞: " + this.horizontalCollision);
                }
            }
        }
    }

    /**
     * 修改速度重置逻辑（水平碰撞时重置速度）
     */
    @Inject(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V",
            ordinal = 0), cancellable = true)
    private void onResetHorizontalVelocity(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (self.hasData(AttachmentTypeRegistry.GRAVITY) && this.horizontalCollision) {
            GravityData gravityData = self.getData(AttachmentTypeRegistry.GRAVITY);
            if (gravityData.isActive()) {
                Vec3 gravityDir = gravityData.getVec3().normalize();
                if (!gravityDir.equals(new Vec3(0, -1, 0))) {
                    Vec3 velocity = self.getDeltaMovement();
                    // 计算速度在重力方向上的分量
                    double parallelSpeed = velocity.dot(gravityDir);
                    // 只保留平行于重力方向的分量
                    Vec3 newVelocity = gravityDir.scale(parallelSpeed);
                    self.setDeltaMovement(newVelocity.x, newVelocity.y, newVelocity.z);
                    ci.cancel();
                    System.out.println("=== 速度重置 ===");
                    System.out.println("原速度: " + velocity);
                    System.out.println("新速度: " + newVelocity);
                    System.out.println("平行速度: " + parallelSpeed);
                }
            }
        }
    }
}
