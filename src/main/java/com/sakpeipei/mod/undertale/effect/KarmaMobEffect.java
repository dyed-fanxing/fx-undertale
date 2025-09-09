package com.sakpeipei.mod.undertale.effect;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Karma;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 21:23
 * KR业报buff
 */
public class KarmaMobEffect extends MobEffect {

    public static final String KR_ATTACKED_ENTITIES = "kr_attacked_entities";
    public static final short MAX_KR = 40;
    public static final byte[] DAMAGE_INTERVAL_FRAMES = {30, 15, 5, 2, 1};  //多少帧掉一点KR

    private byte value;
    private final Set<String> activeAttacks = new HashSet<>();


    public KarmaMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1) {
            entity.hurt(entity.damageSources().source(DamageTypes.KARMA), 1.0f);
        }else if(value > 1){
            setValue(value - 2);
        }
        setValue(value - 1 );
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
//        return value != 0 && calculateDuration(value) % DAMAGE_INTERVAL_FRAMES[calculateAmplifier(value)] == 0;
        return value != 0 && duration % DAMAGE_INTERVAL_FRAMES[calculateAmplifier(value)] == 0;
    }

    @Override
    public void onMobHurt(@NotNull LivingEntity entity, int amplifier, DamageSource source, float damage) {
        if (source.getEntity() instanceof Karma karmaEntity) {
            MobEffectInstance effect = entity.getEffect(MobEffectRegistry.KARMA);
            if (effect != null) {
                Entity attackEntity = source.getDirectEntity();
                if (attackEntity != null) {
                    Map<String, String> karmaAttackTypeMap = karmaEntity.getKarmaAttackType();
                    Map<String, Byte> karmaValueMap = karmaEntity.getKarmaAttackTypeValue();
                    if (karmaAttackTypeMap.containsKey(attackEntity.getUUID().toString())) {
                        String attackType = karmaAttackTypeMap.get(attackEntity.getUUID().toString());
                        // 使用攻击类型 + 攻击实体UUID 组合成判定是否为不同攻击类型的不同攻击实体的第一次攻击
                        String key =  attackType + ":" + attackEntity.getStringUUID();
                        if(activeAttacks.contains(key)){
                            setValue(value + 1 );
                        }else{
                            setValue(value + karmaValueMap.getOrDefault(attackType,(byte)0) );
                            activeAttacks.add(key);
                        }
                    } else {
                        return;
                    }
                    CompoundTag persistentData = attackEntity.getPersistentData();
                    ListTag attackedEntities;
                    if (persistentData.contains(KarmaMobEffect.KR_ATTACKED_ENTITIES, Tag.TAG_LIST)) {
                        attackedEntities = persistentData.getList(KarmaMobEffect.KR_ATTACKED_ENTITIES, Tag.TAG_STRING);
                    } else {
                        attackedEntities = new ListTag();
                        persistentData.put(KarmaMobEffect.KR_ATTACKED_ENTITIES, attackedEntities);
                    }
                    // 添加目标实体UUID到列表
                    if (!attackedEntities.contains(StringTag.valueOf(entity.getStringUUID()))) {
                        attackedEntities.add(StringTag.valueOf(entity.getStringUUID()));
                    }
                }
            }
        }
    }

    private void setValue(int value){
        this.value = (byte) Mth.clamp(value,0,MAX_KR);
    }

    public int calculateAmplifier(int kr) {
        return kr / 10;
    }

    public Set<String> getActiveAttacks() {
        return activeAttacks;
    }
}
