package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.entity.capability.Rollable;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class RollableEntityMixin {
    @Shadow
    public abstract Vec3 getViewVector(float partialTick);
    @Shadow protected abstract Vec3 calculateUpVector(float xRot, float yRot);
    @Shadow public abstract float getViewXRot(float partialTick);
    @Shadow public abstract float getViewYRot(float partialTick);


    @Shadow public abstract float getEyeHeight();

    @Shadow public double xo;

    @Shadow public double yo;

    @Shadow public double zo;

    @Shadow public abstract double getX();

    @Shadow public abstract double getY();

    @Shadow public abstract double getZ();

    @Shadow public abstract Vec3 getUpVector(float p_20290_);

    @Shadow public abstract Vec3 position();

    @Shadow public abstract float getXRot();

    @Shadow public abstract float getYRot();

    @Shadow private Vec3 position;

    @Inject(method = "getUpVector", at = @At("RETURN"), cancellable = true)
    private void onGetUpVector(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        // 获取原始返回值（不含横滚的上方向）
        Vec3 original = cir.getReturnValue();
        // 检查当前实体是否实现了 IRollable
        if (this instanceof Rollable rollable) {
            float roll = rollable.getRoll();
            if (Math.abs(roll) > 1e-7) {
                Vec3 forward = ((Entity)(Object)this).getViewVector(partialTick);
                Vec3 rotated = RotUtils.rotate(original, forward, roll* Mth.DEG_TO_RAD);
                cir.setReturnValue(rotated);
            }
        }
    }

    /**
     * 拦截无参数的 getEyePosition()，使其应用横滚
     */
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void onGetEyePosition(CallbackInfoReturnable<Vec3> cir) {
        if (this instanceof Rollable rollable) {
            float roll = rollable.getRoll();
            if (Math.abs(roll) > 1e-7) {
                cir.setReturnValue(RotUtils.getWorldVec3(0, this.getEyeHeight(), 0, roll, this.getXRot(), this.getYRot()).add(this.position));
            }
        }
    }

    /**
     * 拦截带参数的 getEyePosition(float partialTick)，同样处理横滚
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void onGetEyePositionPartial(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (this instanceof Rollable rollable) {
            float roll = rollable.getRoll();
            if (Math.abs(roll) > 1e-7) {
                cir.setReturnValue(RotUtils.getWorldVec3(0, this.getEyeHeight(), 0, roll, this.getXRot(), this.getYRot()).add(
                        Mth.lerp(partialTick, this.xo, this.getX()),
                        Mth.lerp(partialTick, this.yo, this.getY()),
                        Mth.lerp(partialTick, this.zo, this.getZ())
                ));
            }
        }
    }

}
