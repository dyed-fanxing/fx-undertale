package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.entity.capability.QuaternionRotatable;
import com.fanxing.fx_undertale.entity.capability.Rollable;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * key 以下涉及到需要根据角度插值计算的，都必须用Rotlerp角度插值去计算，不然会导致插值的角度在标准化的时候反向插值
 *  在 180 -> -180转换时，由于旧指还是原来的180 而新值变成了-180，如果是lerp插值就会在这一Tick内，从180反向插值到-180，反向绕了整整一圈
 *  由于欧拉角旋转会导致轴方向变反，导致OBB CCD旋转判定的扫掠方向滞后，而不是向着旋转方向的未来扫掠，所以只要涉及需要随意自由旋转的，必须使用四元数，而不能使用欧拉角
 */
@Mixin(Entity.class)
public abstract class OBBEntityMixin {
    private static final Logger log = LoggerFactory.getLogger(OBBEntityMixin.class);

    @Shadow
    public abstract Vec3 getViewVector(float partialTick);

    @Shadow
    protected abstract Vec3 calculateUpVector(float xRot, float yRot);

    @Shadow
    public abstract float getViewXRot(float partialTick);

    @Shadow
    public abstract float getViewYRot(float partialTick);


    @Shadow
    public abstract float getEyeHeight();

    @Shadow
    public double xo;

    @Shadow
    public double yo;

    @Shadow
    public double zo;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract Vec3 getUpVector(float p_20290_);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Shadow
    private Vec3 position;

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;


    @Shadow
    public float yRotO;

    @Shadow
    public float xRotO;

    @Shadow
    public abstract Vec3 getEyePosition();

    @Inject(method = "getViewXRot", at = @At("RETURN"), cancellable = true)
    public void getViewXRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (this instanceof OBBHolder) {
            cir.setReturnValue(partialTick == 1.0F ? this.getXRot() : Mth.rotLerp(partialTick, this.xRotO, this.getXRot()));
        }
    }

    @Inject(method = "getViewYRot", at = @At("RETURN"), cancellable = true)
    public void getViewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (this instanceof OBBHolder) {
            cir.setReturnValue(partialTick == 1.0F ? this.getYRot() : Mth.rotLerp(partialTick, this.yRotO, this.getYRot()));
        }
    }

    /**
     * 重新计算up向量
     * QuaternionRotatable 实体使用四元数计算，避免万向节死锁
     */
    @Inject(method = "getUpVector", at = @At("RETURN"), cancellable = true)
    private void onGetUpVector(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (this instanceof OBBHolder) {
            if (this instanceof QuaternionRotatable quaternionRotatable) {
                cir.setReturnValue(RotUtils.rotate(0f, 1, 0f, quaternionRotatable.getLerpOrientation(partialTick)));
                return;
            }

            if (this instanceof Rollable rollable) {
                cir.setReturnValue(RotUtils.rotateYXZ(0, 1, 0, getViewYRot(partialTick), getViewXRot(partialTick), Mth.rotLerp(partialTick, rollable.getRollO(), rollable.getRoll())));
            } else {
                cir.setReturnValue(RotUtils.rotateYX(0, 1, 0, getViewYRot(partialTick), getViewXRot(partialTick)));
            }
        }
    }

    /**
     * 拦截无参数的 getEyePosition()，使其应用横滚
     * QuaternionRotatable 实体使用四元数计算，避免万向节死锁
     */
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void onGetEyePosition(CallbackInfoReturnable<Vec3> cir) {
        if (this instanceof OBBHolder) {
            if (this instanceof QuaternionRotatable quaternionRotatable) {
                cir.setReturnValue(this.position().add(RotUtils.rotate(0f, getEyeHeight(), 0f, quaternionRotatable.getOrientation())));
                return;
            }

            if (this instanceof Rollable rollable) {
                cir.setReturnValue(RotUtils.rotateYXZ(0, this.getEyeHeight(), 0, this.getYRot(), this.getXRot(), rollable.getRoll()).add(this.position));
            } else {
                cir.setReturnValue(RotUtils.rotateYX(0, this.getEyeHeight(), 0, this.getYRot(), this.getXRot()).add(this.position));
            }
        }
    }

    /**
     * 拦截带参数的 getEyePosition(float partialTick)，同时处理可以横滚的
     * QuaternionRotatable 实体使用四元数计算，避免万向节死锁
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void onGetEyePositionPartial(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (this instanceof OBBHolder) {
            if (this instanceof QuaternionRotatable quaternionRotatable) {
                cir.setReturnValue(RotUtils.rotate(0f, getEyeHeight(), 0f, quaternionRotatable.getLerpOrientation(partialTick)).add(
                        Mth.lerp(partialTick, xo, getX()),
                        Mth.lerp(partialTick, yo, getY()),
                        Mth.lerp(partialTick, zo, getZ())
                ));
                return;
            }

            if (this instanceof Rollable rollable) {
                cir.setReturnValue(RotUtils.rotateYXZ(0, this.getEyeHeight(), 0,
                        this.getViewYRot(partialTick),
                        this.getViewXRot(partialTick),
                        Mth.rotLerp(partialTick, rollable.getRollO(), rollable.getRoll())).add(
                        Mth.lerp(partialTick, this.xo, this.getX()),
                        Mth.lerp(partialTick, this.yo, this.getY()),
                        Mth.lerp(partialTick, this.zo, this.getZ())
                ));
            } else {
                cir.setReturnValue(RotUtils.rotateYX(0, this.getEyeHeight(), 0,
                        this.getViewYRot(partialTick),
                        this.getViewXRot(partialTick)).add(
                        Mth.lerp(partialTick, this.xo, this.getX()),
                        Mth.lerp(partialTick, this.yo, this.getY()),
                        Mth.lerp(partialTick, this.zo, this.getZ())
                ));
            }
        }
    }
}
