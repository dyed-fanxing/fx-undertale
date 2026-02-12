package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;
    @Shadow protected abstract void setPosition(double p_90585_, double p_90586_, double p_90587_);

    /**
     * 根据重力不同方向调整相机的位置
     */
    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void setPositionInSetUp(BlockGetter blockGetter, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            switch (data.getGravity()) {
                case UP -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                case SOUTH -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()) - (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight));
                case NORTH -> this.setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()) + (double)Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight));
            }
        }
    }

}
