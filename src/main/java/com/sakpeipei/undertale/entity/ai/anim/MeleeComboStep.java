package com.sakpeipei.undertale.entity.ai.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;


public record MeleeComboStep(Byte id, int[] hitTicks, BooleanSupplier canNext, Predicate<LivingEntity> onHit, int duration, int cooldown, int timeout) {


}
