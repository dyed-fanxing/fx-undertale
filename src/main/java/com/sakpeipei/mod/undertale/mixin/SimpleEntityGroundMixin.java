package com.sakpeipei.mod.undertale.mixin;

import com.sakpeipei.mod.undertale.entity.attachment.GravityData;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author yujinbao
 * @since 2025/11/14 10:46
 * Entity重力相关方法
 */
@Mixin(Entity.class)
public abstract class SimpleEntityGroundMixin {
    @Shadow
    protected abstract Vec3 collide(Vec3 movement);
//    /**
//     * @author sakpeipei
//     * @reason 根据实体的局部重力方向判断是否在地面上
//     */
//    @Inject(method = "onGround", at = @At("HEAD"))
//    private void onGroundWithCustomGravity(CallbackInfoReturnable<Boolean> cir) {
//        Entity entity = (Entity)(Object)this;
//
//        // 只要有 Attachment 数据就使用自定义地面检测
//        if (entity.hasData(AttachmentTypeRegistry.GRAVITY)) {
//            GravityData gravityData = entity.getData(AttachmentTypeRegistry.GRAVITY);
//            Vec3 gravityDir = gravityData.getDownDirection();
//            // 使用原版的 collide 方法进行碰撞检测
//            Vec3 actualMovement = this.collide(movement);
//
//            // 计算移动在重力方向上的分量
//            double intendedMovementInGravityDir = movement.dot(gravityDir);
//            double actualMovementInGravityDir = actualMovement.dot(gravityDir);
//
//            // 原版逻辑：在重力方向上发生碰撞且向重力方向移动
//            boolean hasCollisionInGravityDir = !Mth.equal(intendedMovementInGravityDir, actualMovementInGravityDir);
//            boolean isMovingTowardsGravity = intendedMovementInGravityDir < 0.0;
//
//            boolean customOnGround = hasCollisionInGravityDir && isMovingTowardsGravity;
//
//            entity.onGround = customOnGround;
//            entity.checkSupportingBlock(customOnGround, movement);
//            ci.cancel();
//        }
//    }

//    /**
//     * 修改整个地面检测逻辑
//     */
//    @Inject(
//            method = "move",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V"
//            ),
//            cancellable = true)
//    private void onSetOnGroundWithMovement(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity entity = (Entity)(Object)this;
//
//        if (entity.hasData(AttachmentTypeRegistry.GRAVITY)) {
//            GravityData gravityData = entity.getData(AttachmentTypeRegistry.GRAVITY);
//            Vec3 gravityDir = gravityData.getDownDirection();
//
//            // 使用原版碰撞检测
//            Vec3 actualMovement = this.collide(movement);
//
//            // 计算在重力方向上的碰撞
//            double intendedMovementInGravityDir = movement.dot(gravityDir);
//            double actualMovementInGravityDir = actualMovement.dot(gravityDir);
//            boolean hasGravityCollision = !Mth.equal(intendedMovementInGravityDir, actualMovementInGravityDir);
//            boolean isMovingTowardsGravity = intendedMovementInGravityDir < 0.0;
//
//            // 设置地面状态
//            boolean customOnGround = hasGravityCollision && isMovingTowardsGravity;
//            entity.setOnGroundWithMovement(customOnGround, actualMovement);
//
//            // 取消原版的设置
//            ci.cancel();
//        }
//    }
}
