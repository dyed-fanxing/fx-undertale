package com.sakpeipei.undertale.mixin;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
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
    protected abstract Vec3 collide(Vec3 movement);

    @Shadow public boolean verticalCollision;
    @Shadow public boolean verticalCollisionBelow;

    /**
     * 修改垂直碰撞判断，支持局部重力
     */
    @Inject(method = "move", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;verticalCollisionBelow:Z",
            ordinal = 0, shift = At.Shift.AFTER))
    private void onCalculateVerticalCollisionBelow(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (self instanceof LivingEntity living) {
            if (living.hasData(AttachmentTypeRegistry.GRAVITY)) {
                GravityData gravityData = living.getData(AttachmentTypeRegistry.GRAVITY);

                if (gravityData.isActive()) {
                    // 修改判断逻辑
                    Vec3 gravityVec = gravityData.getVec3().normalize();
                    double moveInGravity = movement.dot(gravityVec);
                    double resultInGravity = result.dot(gravityVec);

                    // 替换原版的 flag1 和 flag3
                    this.verticalCollision = Math.abs(moveInGravity - resultInGravity) > 1.0E-7;
                    this.verticalCollisionBelow = verticalCollision && moveInGravity > 0.0;
                }
            }
        }
    }

}
