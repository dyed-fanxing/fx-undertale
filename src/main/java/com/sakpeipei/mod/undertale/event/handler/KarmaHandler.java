package com.sakpeipei.mod.undertale.event.handler;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.effect.KarmaMobEffect;
import com.sakpeipei.mod.undertale.entity.boss.Karma;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;
import java.util.UUID;

/**
 * @author yujinbao
 * @since 2025/9/9 13:17
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class KarmaHandler {
    @SubscribeEvent
    public static void onLivingEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 死亡时重置所有 Karma 数据
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Karma owner) {
            Entity.RemovalReason removalReason = entity.getRemovalReason();
            if (removalReason != null && removalReason.shouldDestroy()) {
                Map<String, String> karmaAttackTypeMap = owner.getKarmaAttackType();
                // 先获取攻击类型，再移除
                String entityUUID = entity.getStringUUID();
                String attackType = karmaAttackTypeMap.get(entityUUID);
                karmaAttackTypeMap.remove(entityUUID);
                //遍历KR攻击物攻击过的实体列表
                ListTag list = entity.getPersistentData().getList(KarmaMobEffect.KR_ATTACKED_ENTITIES, Tag.TAG_STRING);
                list.forEach(targetUUID -> {
                    if(entity.level() instanceof ServerLevel level){
                        if(level.getEntity(UUID.fromString(targetUUID.getAsString())) instanceof LivingEntity livingEntity){
                            MobEffectInstance effect = livingEntity.getEffect(MobEffectRegistry.KARMA);
                            if (effect != null) {
                                KarmaMobEffect value =  (KarmaMobEffect) effect.getEffect().value();
                                value.getActiveAttacks().remove(attackType + ":" + entityUUID );
                            }
                        }
                    }
                });
                // 清理攻击实体的NBT数据
                entity.getPersistentData().remove(KarmaMobEffect.KR_ATTACKED_ENTITIES);
            }
        }
    }

    /**
     * 被sans第一次攻击之后添加KR效果(只是一个表面,不做具体数据处理,数据处理在KarmaMobEffect里处理)，时间无限，
     * 因为LivingEntity hurt方法中的buff的onMobHurt方法是在实体收拾事件Post之后触发的,
     * 所以仍可以触发buff效果的onMobHurt方法
     */
    @SubscribeEvent
    public static void onEntityHurt(LivingDamageEvent.Post event){
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if(source.getEntity() instanceof Karma && entity.getEffect(MobEffectRegistry.KARMA) == null){
            // 添加KR效果
            entity.addEffect(new MobEffectInstance(MobEffectRegistry.KARMA,-1));
        }
    }
}