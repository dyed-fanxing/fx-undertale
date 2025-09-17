package com.sakpeipei.mod.undertale.event.handler;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.boss.Karma;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
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

import java.util.HashSet;
import java.util.UUID;

/**
 * @author yujinbao
 * @since 2025/9/9 13:17
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class KarmaHandler {
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

    /**
     * 当KR攻击物被销毁时，移除攻击过的且还处于KR状态下的实体的 来自自身KR攻击招式的判重key
     * @param event
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Karma owner) {
            Entity.RemovalReason removalReason = entity.getRemovalReason();
            if (removalReason != null && removalReason.shouldDestroy()) {
                if(entity.level() instanceof ServerLevel level){
                    // 拥有者UUID字符串，攻击过的实体UUID字符串列表
                    KaramAttackData data = entity.getData(AttachmentTypeRegistry.KARMA_ATTACK);
                    HashSet<String> attackedEntities = data.getAttackedEntities();
                    // 遍历
                    attackedEntities.forEach(attackedStringUUID -> {
                        //如果攻击的实体是活体
                        if(level.getEntity(UUID.fromString(attackedStringUUID)) instanceof LivingEntity attackedEntity){
                            //判断是否存在KR效果
                            if(attackedEntity.hasEffect(MobEffectRegistry.KARMA)){
                                attackedEntity.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT).getAttacks().remove(owner.getStringUUID() + ':' + data.getUUID());
                            }
                        }
                    });
                }
            }
        }
    }
}