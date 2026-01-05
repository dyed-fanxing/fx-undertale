package com.sakpeipei.undertale.mixin;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {

    @Shadow public float xxa;
    @Shadow public float zza;
    @Shadow public float yya;

    /**
     * 完全接管有自定义重力实体的运动逻辑 - 全部写在一个方法里
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelWithCustomGravity(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity.hasData(AttachmentTypeRegistry.GRAVITY)) {
            GravityData gravityData = entity.getData(AttachmentTypeRegistry.GRAVITY);

            if (gravityData.isActive()) {
                // 获取重力数据
                Vec3 g = gravityData.getVec3().normalize();
                double g0 = entity.getGravity();

                double dy = entity.getDeltaMovement().dot(g);

                if (dy < 0 && entity.hasEffect(MobEffects.SLOW_FALLING)) {
                    g0 = Math.min(g0, 0.01);
                }

                // === 1. 检测局部"地面" ===
                AABB groundBox = entity.getBoundingBox()
                        .expandTowards(g.scale(0.1))
                        .inflate(-0.05, -0.05, -0.05);
                boolean onLocalGround = !entity.level().noCollision(entity, groundBox);

                // === 2. 计算局部摩擦 ===
                float friction = 0.91F;
                if (onLocalGround) {
                    Vec3 checkPos = entity.position().add(g.scale(0.1));
                    BlockPos blockPos = BlockPos.containing(checkPos);
                    friction = entity.level().getBlockState(blockPos)
                            .getFriction(entity.level(), blockPos, entity) * 0.91F;
                }

                // === 3. 计算局部移动输入 ===
                Vec3 movement = Vec3.ZERO;
                Vec3 input = new Vec3(xxa, yya, zza);
                if (input.lengthSqr() > 0.0001) {
                    // 计算局部坐标系
                    Vec3 up = g.reverse();
                    Vec3 lookVec = entity.getLookAngle();
                    double dot = lookVec.dot(g);
                    Vec3 forward = lookVec.subtract(g.scale(dot));
                    if (forward.lengthSqr() < 0.001) {
                        forward = new Vec3(0, 0, 1).subtract(g.scale(g.z));
                    }
                    forward = forward.normalize();
                    Vec3 right = new Vec3(
                            up.y * forward.z - up.z * forward.y,
                            up.z * forward.x - up.x * forward.z,
                            up.x * forward.y - up.y * forward.x
                    ).normalize();

                    // 转换输入到局部坐标系
                    movement = Vec3.ZERO
                            .add(right.scale(input.x))
                            .add(forward.scale(input.z))
                            .add(up.scale(input.y))
                            .normalize()
                            .scale(entity.getSpeed() * 0.15);
                }

                // === 4. 处理不同状态 ===
                Vec3 velocity = entity.getDeltaMovement();

                if (entity.isInWater() || entity.isInLava()) {
                    // 流体中：减弱重力
                    double fluidGravity = g0 * 0.02;
                    velocity = velocity.add(g.scale(fluidGravity)).add(movement).scale(0.8);
                } else if (entity.isFallFlying()) {
                    // 鞘翅飞行：修改重力方向
                    velocity = velocity.add(g.scale(g0 * 0.5));
                    Vec3 lookVec = entity.getLookAngle();
                    float pitch = entity.getXRot() * Mth.DEG_TO_RAD;
                    double cosPitch = Math.cos(pitch);
                    double lift = cosPitch * cosPitch * Math.min(1.0, lookVec.length() / 0.4);
                    velocity = velocity.add(g.scale(-1.0 + lift * 0.75));
                } else if (entity.onClimbable()) {
                    // 爬梯子：忽略局部重力
                    if (velocity.y > 0) {
                        velocity = velocity.add(0, -g0 * 0.5, 0);
                    }
                } else {
                    // === 5. 正常移动（核心逻辑） ===
                    // 应用重力和移动
                    velocity = velocity.add(g.scale(g0)).add(movement);

                    // 应用摩擦
                    if (onLocalGround) {
                        velocity = new Vec3(
                                velocity.x * friction,
                                velocity.y,
                                velocity.z * friction
                        );
                        // 抵消向下的速度
                        double gravityDot = velocity.dot(g);
                        if (gravityDot < 0) {
                            velocity = velocity.add(g.scale(-gravityDot * 0.5));
                        }
                    } else {
                        velocity = velocity.multiply(1.0, 0.98, 1.0);
                    }

                    // 移动实体
                    entity.move(MoverType.SELF, velocity);

                    // 防止卡地
                    if (onLocalGround) {
                        AABB pushBox = entity.getBoundingBox()
                                .expandTowards(g.scale(0.05))
                                .inflate(-0.01, -0.01, -0.01);
                        if (!entity.level().noCollision(entity, pushBox)) {
                            entity.setDeltaMovement(entity.getDeltaMovement().add(g.reverse().scale(0.05)));
                            entity.hurtMarked = true;
                        }
                    }
                }

                // === 6. 设置最终速度 ===
                entity.setDeltaMovement(velocity);
                entity.hurtMarked = true;

                // === 7. 更新动画 ===
                entity.calculateEntityAnimation(false);

                // 取消原版逻辑
                ci.cancel();
            }
        }
    }

    /**
     * 修改跳跃方向
     */
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void onJumpWithCustomGravity(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity.hasData(AttachmentTypeRegistry.GRAVITY)) {
            GravityData gravityData = entity.getData(AttachmentTypeRegistry.GRAVITY);

            if (gravityData.isActive()) {
                // 在重力反方向跳跃
                Vec3 jumpDirection = gravityData.getVec3().reverse();
                double jumpStrength = 0.42;

                if (entity.hasEffect(MobEffects.JUMP)) {
                    jumpStrength += 0.1 * (entity.getEffect(MobEffects.JUMP).getAmplifier() + 1);
                }

                Vec3 jumpForce = jumpDirection.scale(jumpStrength);
                entity.setDeltaMovement(entity.getDeltaMovement().add(jumpForce));
                entity.hurtMarked = true;

                ci.cancel();
            }
        }
    }
}