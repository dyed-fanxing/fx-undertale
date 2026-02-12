package com.sakpeipei.undertale.mixin.roll;

import com.sakpeipei.undertale.entity.IRollable;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRollMixin {

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F",shift = At.Shift.AFTER))
    private void setRoll(CallbackInfo ci){
        IRollable rollable = (IRollable)this;
        rollable.undertale$setRollO(rollable.undertale$getRoll());
    }
}