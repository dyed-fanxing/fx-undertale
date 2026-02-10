package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final Logger log = LoggerFactory.getLogger(CameraMixin.class);
    @Shadow @Final private Quaternionf rotation;

    @Shadow @Final private Vector3f forwards;

    @Shadow @Final private Vector3f up;

    @Shadow @Final private Vector3f left;

    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);

    @Shadow private float xRot;

    @Shadow private float yRot;

    @Shadow private float roll;

    @Shadow @Final private static Vector3f FORWARDS;
    @Shadow @Final private static Vector3f UP;
    @Shadow @Final private static Vector3f LEFT;

    @Shadow protected abstract void setPosition(double p_90585_, double p_90586_, double p_90587_);

    @Shadow private Vec3 position;

    @Shadow protected abstract void setPosition(Vec3 p_90582_);

    @Shadow private float eyeHeightOld;

    @Shadow private float eyeHeight;

    @Inject(method = "setRotation(FFF)V", at = @At("HEAD"), cancellable = true)
    protected void onSetRotationHead(float yaw, float pitch, float roll, CallbackInfo ci) {
        Camera camera = (Camera)(Object)this;
        GravityData data = camera.getEntity().getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if (data.getGravity() != Direction.DOWN) {
            switch (gravity) {
                case UP -> {

                    roll += 180f;
                    yaw += 180f;
                    this.yRot = yaw;
                    this.xRot = pitch;
                    this.roll = roll;
                    // 计算旋转四元数
                    this.rotation.rotationYXZ((float)Math.PI - yaw * ((float)Math.PI / 180F), -pitch * ((float)Math.PI / 180F), -roll * ((float)Math.PI / 180F));
                    FORWARDS.rotate(this.rotation, this.forwards);
                    UP.rotate(this.rotation, this.up);
                    LEFT.rotate(this.rotation, this.left);
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void setPositionInSetUp(BlockGetter p_90576_, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            switch (data.getGravity()) {
                case UP -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                case EAST -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.zo, entity.getZ()));
            }
        }
    }

}
