package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.common.DamageTypes;
import com.fanxing.fx_undertale.entity.attachment.KaramJudge;
import com.fanxing.fx_undertale.entity.attachment.Karam;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Sakpeipei
 * @since 2025/9/9 13:17
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class KarmaHandler {
    private static final byte[] DAMAGE_INTERVAL_FRAMES = {30, 15, 5, 2, 1};

    /**
     * 存活实体受伤后
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event){
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return; // 只在服务端处理
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        // Sans触发，直接攻击实体不为null，盾牌阻挡的伤害 > 0且剩余伤害 <= 0
        // key 为什么不用markHurt，因为为了玩家不被GB炮黏住，
        // 由于markHurt标记后，会重置玩家速度，导致玩家在被帧伤等高频伤害攻击时候走不动
        // 所以对帧伤类型伤害使用no_impact标签，该标签会在hurt方法跳过markHurt，导致markHurt标记不上，所以判断不了
        // 而且这个事件的时机，markHurt还没有设置呢，所以更用不了了
        if (!(attacker instanceof Sans) || directEntity == null || (event.getBlockedDamage()>0&&event.getNewDamage() <=0)) return;
        // 获取攻击数据（投射物上的KaramsAttackData）
        KaramJudge attackData = directEntity.getData(AttachmentTypes.KARMA_ATTACK);
        String uuid = attackData.getUUID();

        // 构造唯一键：攻击者UUID + ":" + 投射物UUID
        String key = attacker.getStringUUID() + ":" + uuid;
        // 获取目标身上的KR数据
        Karam karam = entity.getData(AttachmentTypes.KARMA);
        Set<String> attacks = karam.getAttacks();
        // 根据是否已经记录过该攻击来增加KR值
        if (attacks.contains(key)) {
            karam.addValue(1);
        } else {
            karam.addValue(attackData.getValue());
            attacks.add(key);
        }
        entity.setData(AttachmentTypes.KARMA, karam);
        karam.sendPacket(entity, -1f);
    }

    @SubscribeEvent
    public static void onLivingTickPos(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if(entity instanceof LivingEntity livingEntity){
            Karam karam = livingEntity.getData(AttachmentTypes.KARMA);
            byte value = karam.getValue();
            if (value == 0) return; // 无KR值，不处理
            // 计算伤害间隔
            if (livingEntity.tickCount % DAMAGE_INTERVAL_FRAMES[value / 10] == 0) {
                float lastAbsorption = livingEntity.getAbsorptionAmount();
                if(livingEntity instanceof Player){
                    if(livingEntity.getHealth() > 1){
                        livingEntity.hurt(livingEntity.damageSources().source(DamageTypes.KARMA), 1.0f);
                    }
                }else{
                    livingEntity.hurt(livingEntity.damageSources().source(DamageTypes.KARMA), 1.0f);
                }
                karam.subValue(livingEntity);
                // 同步客户端数据
                float currentAbsorption = livingEntity.getAbsorptionAmount();
                if (currentAbsorption != lastAbsorption) {
                    karam.sendPacket(livingEntity, currentAbsorption);
                } else {
                    karam.sendPacket(livingEntity, -1f);
                }
                livingEntity.setData(AttachmentTypes.KARMA, karam);
            }
        }
    }

    @SubscribeEvent
    public static void onKRAttackerEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide) {
            if( entity instanceof LivingEntity livingEntity && livingEntity.hasData(AttachmentTypes.KARMA)){
                livingEntity.removeData(AttachmentTypes.KARMA);
            }
        }
        //当KR攻击物被销毁时，移除攻击过的且还处于KR状态下的实体的 来自自身KR攻击招式的判重key
        if (entity instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Sans owner) {
            Entity.RemovalReason removalReason = entity.getRemovalReason();
            if (removalReason != null && removalReason.shouldDestroy()) {
                if(entity.level() instanceof ServerLevel level){
                    // 拥有者UUID字符串，攻击过的实体UUID字符串列表
                    KaramJudge data = entity.getData(AttachmentTypes.KARMA_ATTACK);
                    HashSet<String> attackedEntities = data.getAttackedEntities();
                    if(attackedEntities != null && !attackedEntities.isEmpty()){
                        // 遍历
                        attackedEntities.forEach(attackedStringUUID -> {
                            //如果攻击的实体是活体
                            if(level.getEntity(UUID.fromString(attackedStringUUID)) instanceof LivingEntity attackedEntity){
                                //判断是否存在KR效果
                                if(attackedEntity.hasData(AttachmentTypes.KARMA)){
                                    attackedEntity.getData(AttachmentTypes.KARMA).getAttacks().remove(owner.getStringUUID() + ':' + data.getUUID());
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}