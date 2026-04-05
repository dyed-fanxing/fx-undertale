package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraGravityMixin {

    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;
    @Shadow protected abstract void setPosition(double p_90585_, double p_90586_, double p_90587_);

    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;

    @Shadow @Final private static Vector3f UP;
    @Shadow @Final private static Vector3f FORWARDS;
    @Shadow @Final private static Vector3f LEFT;

    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private float roll;

    @Shadow private Entity entity;

    /**
     * 根据重力不同方向调整相机的位置
     */
    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void setPositionInSetUp(BlockGetter blockGetter, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
            switch (gravity) {
                case UP -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                case EAST -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                case WEST -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()) + (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                case SOUTH -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight));
                case NORTH -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()) + (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight));
            }
        }
    }

    @Inject(method = "setRotation(FFF)V", at = @At(value = "HEAD"), cancellable = true)
    public void setRotation(float yRot, float xRot, float roll, CallbackInfo ci) {
        Direction gravity = this.entity.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN) {
            ci.cancel();
            this.xRot = xRot;
            this.yRot = yRot;
            this.roll = roll;
            this.rotation.set(GravityUtils.getLocalToWorldF(gravity)).rotateYXZ((float)Math.PI - yRot * ((float)Math.PI / 180F), -xRot * ((float)Math.PI / 180F), -roll * ((float)Math.PI / 180F));
            FORWARDS.rotate(this.rotation, this.forwards);
            UP.rotate(this.rotation, this.up);
            LEFT.rotate(this.rotation, this.left);
        }
    }
}
