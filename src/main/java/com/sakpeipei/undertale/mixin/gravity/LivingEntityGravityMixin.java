package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.IRollable;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin{
    private static final Logger log = LoggerFactory.getLogger(LivingEntityGravityMixin.class);

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F",shift = At.Shift.AFTER))
    private void setRoll(CallbackInfo ci){
        IRollable rollable = (IRollable)this;
        rollable.undertale$setRollO(rollable.undertale$getRoll());
    }


    /**
     * 如果正在冲刺，会对面向的方向进行加速，而速度需要局部角度算出来
     * 所以需要捕获f1返回局部角度yRot
     */
    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float getYRotInJumpFromGround(LivingEntity instance) {
        GravityData data = instance.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        return switch (gravity) {
            case DOWN -> instance.getYRot();
            case UP ->  -instance.getYRot();
            case NORTH -> 0.0F;
            case SOUTH -> ((IRollable)instance).undertale$getRoll();
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
}