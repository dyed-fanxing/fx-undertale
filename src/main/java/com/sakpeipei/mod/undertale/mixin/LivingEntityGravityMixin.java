package com.sakpeipei.mod.undertale.mixin;

import com.sakpeipei.mod.undertale.entity.attachment.GravityData;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author yujinbao
 * @since 2025/11/14 10:46
 * LivingEntity重力Mixin - 精确修改重力计算
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {

    @Shadow
    protected boolean jumping;

    @Shadow
    public float xxa;

    @Shadow
    public float zza;

    @Shadow
    public float yya;

//    /**
//     * 完全接管有自定义重力实体的运动逻辑
//     */
//    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
//    private void onTravelWithCustomGravity(Vec3 travelVector, CallbackInfo ci) {
//        LivingEntity entity = (LivingEntity)(Object)this;
//
//        if (entity.hasData(AttachmentTypeRegistry.GRAVITY)) {
//            GravityData gravityData = entity.getData(AttachmentTypeRegistry.GRAVITY);
//
//            ci.cancel(); // 取消原版逻辑
//        }
//    }

}
