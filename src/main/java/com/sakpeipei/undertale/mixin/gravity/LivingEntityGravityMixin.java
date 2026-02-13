package com.sakpeipei.undertale.mixin.gravity;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {

    /**
     * 修改移动时控制身体旋转的计算用dx硬编码
     */
    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 0)
    private double d1_dx(double d1) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        return switch (gravity) {
            case DOWN, SOUTH, NORTH -> d1;                // 局部x -> 世界x
            case UP -> -(self.getX() - self.xo);    // 局部x -> 世界x 取反
            case EAST -> -(self.getZ() - self.zo);  // 局部x -> 世界z，取反
            case WEST -> self.getZ() - self.zo;     // 局部x -> 世界z
        };
    }

    /**
     * 修改移动时控制身体旋转的计算用dz硬编码
     */
    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 1)
    private double d0_dz(double d0) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        return switch (gravity) {
            case DOWN, UP -> d0;                         // 局部z -> 世界z
            case EAST, WEST -> self.getY() - self.yo;  // 局部z -> 世界z，且Y不变
            case SOUTH -> self.getY() - self.yo;    // 局部z -> 世界z，y不变
            case NORTH -> -(self.getY() - self.yo); // 局部z -> 世界z，y取反
        };
    }

    /**
     * 修改移动时实体动画计算用dx和dz硬编码
     */
    @ModifyVariable(method = "calculateEntityAnimation", at = @At(value = "STORE"), ordinal = 0)
    private float onCalculateEntityAnimation(float f, @Local(ordinal = 0, argsOnly = true) boolean flying) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        return switch (gravity) {
            case UP, DOWN -> f; // XZ平面
            case SOUTH, NORTH -> (float) Mth.length(self.getX() - self.xo, self.getY() - self.yo, flying ? self.getZ() - self.zo : 0);  // XY平面
            case EAST, WEST -> (float) Mth.length(flying ? self.getX() - self.xo : 0, self.getY() - self.yo, self.getZ() - self.zo);  // YZ平面
        };
    }
}