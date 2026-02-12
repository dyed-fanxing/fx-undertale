package com.sakpeipei.undertale.mixin.roll;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.IRollable;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRollMixin  implements IRollable {
    @Shadow
    protected abstract ListTag newFloatList(float... p_20066_);


    // 添加另一个轴的旋转自由度
    @Unique
    private float undertale$roll;
    @Unique
    private float undertale$rollO;

    public float undertale$getRoll() {
        return undertale$roll;
    }
    public void undertale$setRoll(float roll) {
        this.undertale$roll = roll;
    }
    public float undertale$getRollO() {
        return undertale$rollO;
    }
    public void undertale$setRollO(float rollO) {
        this.undertale$rollO = rollO;
    }
    @Unique
    @Override
    public float undertale$getViewRoll(float partialTicks) {
        return partialTicks == 1.0F ? this.undertale$getRoll() : Mth.lerp(partialTicks, this.undertale$rollO, this.undertale$getRoll());
    }

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F",shift = At.Shift.AFTER))
    private void setRollInBaseTickAfterGetYRot(CallbackInfo ci){
        this.undertale$rollO = undertale$getRoll();
    }

    @Inject(method = "setRot", at = @At(value = "TAIL"))
    private void setRollInSetRot(CallbackInfo ci){
        this.undertale$rollO = undertale$getRoll();
    }

    @Inject(method = "setOldPosAndRot", at = @At(value = "TAIL"))
    private void setOldPosAndRotTail(CallbackInfo ci){
        this.undertale$rollO = this.undertale$getRoll();
    }



    @Redirect(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;newFloatList([F)Lnet/minecraft/nbt/ListTag;",ordinal = 0))
    private ListTag rotationInSaveWithoutId(Entity instance, float[] floats){
        IRollable rollable = (IRollable)instance;
        Direction gravity = instance.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
        return switch (gravity){
            case DOWN,UP -> this.newFloatList(instance.getYRot(), instance.getXRot());
            case EAST,WEST,SOUTH,NORTH -> this.newFloatList(instance.getYRot(), instance.getXRot(),rollable.undertale$getRoll());
        };
    }
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setRot(FF)V",shift = At.Shift.AFTER))
    private void setRoll(CallbackInfo ci){
        this.undertale$setRoll(undertale$getRoll() % 360.0F);
    }
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setXRot(F)V",shift = At.Shift.AFTER))
    private void setRollInLoadAfterSetXRot(CallbackInfo ci, @Local(ordinal = 2) ListTag listTag2){
        this.undertale$setRoll(listTag2.getFloat(2));
    }



    @Inject(method = "getForward", at = @At("HEAD"), cancellable = true)
    public void getForward(CallbackInfoReturnable<Vec3> cir) {
        // 待修改
//        return Vec3.directionFromRotation(this.getRotationVector());
    }
}
