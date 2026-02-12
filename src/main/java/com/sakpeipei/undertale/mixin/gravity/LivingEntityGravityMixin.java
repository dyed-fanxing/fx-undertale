package com.sakpeipei.undertale.mixin.gravity;

import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin{
    private static final Logger log = LoggerFactory.getLogger(LivingEntityGravityMixin.class);

}