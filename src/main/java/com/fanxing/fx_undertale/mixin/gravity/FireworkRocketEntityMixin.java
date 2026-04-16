package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    private static final Logger log = LoggerFactory.getLogger(FireworkRocketEntityMixin.class);
    @Shadow @Nullable private LivingEntity attachedToEntity;

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 redirectGetLookAngle(LivingEntity entity) {
        Vec3 original = entity.getLookAngle();
        Direction gravity = attachedToEntity.getData(AttachmentTypes.GRAVITY);
        log.info("original{}", original);
        if(gravity == Direction.DOWN) return original;
        else return GravityUtils.worldToLocal(gravity, original);
    }
}
