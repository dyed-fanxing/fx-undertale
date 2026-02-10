package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {
    private static final Logger log = LoggerFactory.getLogger(LivingEntityGravityMixin.class);

    /**
     * 如果正在冲刺，会对面向的方向进行加速，而速度需要局部角度算出来
     * 所以需要捕获f1返回局部角度yRot
     */
    @ModifyVariable(method = "jumpFromGround", at = @At(value = "STORE"),ordinal = 1)
    private float jumpFromGround(float f1) {
        LivingEntity self = (LivingEntity) (Object) this;
        return switch (self.getData(AttachmentTypeRegistry.GRAVITY).getGravity()) {
            case DOWN -> f1;
            case UP ->  -f1;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
}