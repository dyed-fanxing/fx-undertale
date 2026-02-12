package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import com.sakpeipei.undertale.registry.MobEffectTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashSet;
import java.util.UUID;

/**
 * @author Sakpeipei
 * @since 2025/9/9 13:17
 */
@EventBusSubscriber(modid = Undertale.MOD_ID)
public class KarmaAndFrameHandler {
    /**
     * 被sans第一次攻击之后添加KR效果(只是一个表面,不做具体数据处理,数据处理在KarmaMobEffect里处理)，时间无限，
     * 因为LivingEntity hurt方法中的buff的onMobHurt方法是在实体收拾事件Post之后触发的,
     * 所以仍可以触发buff效果的onMobHurt方法
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event){
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if(source.getEntity() instanceof Sans && entity.getEffect(MobEffectTypes.KARMA) == null){
            // 添加KR效果
            entity.addEffect(new MobEffectInstance(MobEffectTypes.KARMA,-1));
        }
    }

    /**
     * 拦截活体进入伤害，设置无敌帧
     * 伤害hurt事件顺序，LivingIncomingDamageEvent -> LivingDamageEvent.Pre -> LivingDamageEvent.Post
     * 其中LivingDamageEvent.Pre -> LivingDamageEvent.Post是actuallyHurt方法内的，在hurt中被调用
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event){
        DamageSource source = event.getSource();
        if(source.is(DamageTypes.KARMA) || source.is(DamageTypes.FRAME)){
            int tick = 0;
            Iterable<ItemStack> armorSlots = event.getEntity().getArmorSlots();
            // 遍历装备槽位，检测有无延长无敌时间的装备或道具
            event.getContainer().setPostAttackInvulnerabilityTicks(tick);
        }
    }
    /**
     * 当KR攻击物被销毁时，移除攻击过的且还处于KR状态下的实体的 来自自身KR攻击招式的判重key
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Sans owner) {
            Entity.RemovalReason removalReason = entity.getRemovalReason();
            if (removalReason != null && removalReason.shouldDestroy()) {
                if(entity.level() instanceof ServerLevel level){
                    // 拥有者UUID字符串，攻击过的实体UUID字符串列表
                    KaramAttackData data = entity.getData(AttachmentTypes.KARMA_ATTACK);
                    HashSet<String> attackedEntities = data.getAttackedEntities();
                    if(attackedEntities != null && !attackedEntities.isEmpty()){
                        // 遍历
                        attackedEntities.forEach(attackedStringUUID -> {
                            //如果攻击的实体是活体
                            if(level.getEntity(UUID.fromString(attackedStringUUID)) instanceof LivingEntity attackedEntity){
                                //判断是否存在KR效果
                                if(attackedEntity.hasEffect(MobEffectTypes.KARMA)){
                                    attackedEntity.getData(AttachmentTypes.KARMA_MOB_EFFECT).getAttacks().remove(owner.getStringUUID() + ':' + data.getUUID());
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}