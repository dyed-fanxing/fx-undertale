package com.sakpeipei.undertale.effect;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.undertale.entity.attachment.KaramMobEffectData;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.registry.MobEffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 21:23
 * KR业报buff
 */
public class KarmaMobEffect extends MobEffect {
    public static final byte[] DAMAGE_INTERVAL_FRAMES = {30, 15, 5, 2, 1};  //多少帧掉一点KR

    public KarmaMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if(!entity.level().isClientSide){
            KaramMobEffectData karamData = entity.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT);
            byte value = karamData.getValue();
            if(value == 0){
                entity.removeEffect(MobEffectRegistry.KARMA);
            }else if(entity.tickCount % DAMAGE_INTERVAL_FRAMES[value/10] == 0){
                float last = entity.getAbsorptionAmount();
                if (entity.getHealth() > 1) {
//                    if(entity instanceof Player)                LogUtils.getLogger().info("KR之前{}", karamData.getValue());
                    if(entity.hurt(entity.damageSources().source(DamageTypes.KARMA), 1.0f)){
                        karamData.subValue(entity);
                    }
                }else{
                    karamData.setValue((byte) 0);
                }
                float current = entity.getAbsorptionAmount();
                if(current != last){
                    karamData.sendPacket(entity,current);
                }else{
                    karamData.sendPacket(entity,-1f);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onMobHurt(@NotNull LivingEntity entity, int amplifier, @NotNull DamageSource source, float damage) {
        if(entity.level().isClientSide){
            return;
        }
        Entity sourceEntity = source.getEntity();
        if (source.getEntity() instanceof Sans) {
            KaramMobEffectData karamData = entity.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT);
            Set<String> attacks = karamData.getAttacks();
            Entity attackEntity = source.getDirectEntity();
            if (attackEntity != null) {
                KaramAttackData data = attackEntity.getData(AttachmentTypeRegistry.KARMA_ATTACK);
                String uuid = data.getUUID();
                String key = null;
                if (sourceEntity != null) {
                    key = sourceEntity.getStringUUID() + ':' + uuid;
                }
                if(attacks.contains(key)){
                    karamData.addValue(entity,1);
                    karamData.sendPacket(entity,-1);
                }else{
                    karamData.addValue(entity,data.getValue());
                    karamData.sendPacket(entity,-1);
                    attacks.add(key);
                }
            }
        }
    }

    @Override
    public void onMobRemoved(@NotNull LivingEntity entity, int amplifier, Entity.RemovalReason removalReason) {
        if(removalReason.shouldDestroy()){
            entity.removeData(AttachmentTypeRegistry.KARMA_MOB_EFFECT);
        }
    }
}






