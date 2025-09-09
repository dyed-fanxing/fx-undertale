package com.sakpeipei.mod.undertale.effect;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 21:23
 * KR业报buff
 */
public class KarmaMobEffect extends MobEffect {
    private static final short MAX_KR = 40;
    private Map<String, String> skills;
    private final byte[] DAMAGE_INTERVAL_FRAMES = {30, 15, 5, 2, 1};  //多少帧掉一点KR

    public KarmaMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if(entity.getHealth() > 1) {
            entity.hurt(entity.damageSources().source(DamageTypes.KARMA),1.0f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobHurt(@NotNull LivingEntity entity, int amplifier, DamageSource source, float damage) {
        if(source.getEntity() instanceof Sans) {
            MobEffectInstance effect = entity.getEffect(MobEffectRegistry.KARMA);
            if (effect != null) {
                effect.update(new MobEffectInstance(MobEffectRegistry.KARMA,Integer.MAX_VALUE,getAmplifier()));
            }

        }
    }
    // 设置KR值（0-40）
    public void setKarmaValue(short value) {
        this.value = (short) Mth.clamp(value, 0, MAX_KR);
    }
    // 增加KR值
    public void addKarmaValue(short amount) {
        setKarmaValue((short) (value + amount));
    }
    public int getAmplifier(){
        return value / 10;
    }

}
