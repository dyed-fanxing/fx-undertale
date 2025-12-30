package com.sakpeipei.undertale.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
