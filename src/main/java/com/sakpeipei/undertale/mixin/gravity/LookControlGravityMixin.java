package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 将用于视线角度计算的世界位移差值转为局部位移
 */
@Mixin(LookControl.class)
public abstract class LookControlGravityMixin {

    @Shadow @Final protected Mob mob;
    @Shadow protected double wantedX;
    @Shadow protected double wantedY;
    @Shadow protected double wantedZ;


//    @Inject(method = "getXRotD", at = @At("HEAD"), cancellable = true)
//    private void getXRotD(CallbackInfoReturnable<Optional<Float>> cir) {
//        GravityData gravityData = this.mob.getData(AttachmentTypes.GRAVITY.get());
//        if (gravityData.getGravity() != Direction.DOWN){
//            cir.cancel();
//            double dx = this.wantedX - this.mob.getX();
//            double dy = this.wantedY - this.mob.getEyeY();
//            double dz = this.wantedZ - this.mob.getZ();
//            // 世界 -> 局部
//            Vec3 localDir = gravityData.worldToLocal(dx,dy,dz);
//            double horizontalDist = Math.sqrt(localDir.x * localDir.x + localDir.z * localDir.z);
//            if (Math.abs(localDir.y) <= 1.0E-5F && Math.abs(horizontalDist) <= 1.0E-5F) {
//                cir.setReturnValue(Optional.empty());
//            } else {
//                // 在局部空间中，y 是上下，x/z 是水平
//                float xRot = (float)(-(Mth.atan2(localDir.y, horizontalDist) * (180F / Math.PI)));
//                cir.setReturnValue(Optional.of(xRot));
//            }
//        }
//    }
//    @Inject(method = "getYRotD", at = @At("HEAD"), cancellable = true)
//    private void getYRotD(CallbackInfoReturnable<Optional<Float>> cir) {
//        GravityData gravityData = mob.getData(AttachmentTypes.GRAVITY.get());
//        if (gravityData.getGravity() != Direction.DOWN){
//            double dx = this.wantedX - this.mob.getX();
//            double dy = this.wantedY - this.mob.getY();
//            double dz = this.wantedZ - this.mob.getZ();
//            // 世界 -> 局部
//            Vec3 localDir = gravityData.worldToLocal(dx,dy,dz);
//            if (Math.abs(localDir.z) <= 1.0E-5F && Math.abs(localDir.x) <= 1.0E-5F) {
//                cir.setReturnValue(Optional.empty());
//            } else {
//                // atan2(z, x) 因为局部空间中 Z 是前，X 是右
//                float yRot = (float)(Mth.atan2(localDir.z, localDir.x) * (180F / Math.PI)) - 90.0F;
//                cir.setReturnValue(Optional.of(yRot));
//            }
//        }
//    }
}
