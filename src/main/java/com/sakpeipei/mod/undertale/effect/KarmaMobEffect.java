package com.sakpeipei.mod.undertale.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 21:23
 * KR业报buff
 */
public class KarmaMobEffect extends MobEffect {
    private int value;

    protected KarmaMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int p_295391_, int p_294280_) {
        return true;
    }
}
