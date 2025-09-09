package com.sakpeipei.mod.undertale.event.handler;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

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

    /**
     * 被sans第一次攻击时添加KR效果，时间无限
     */
    @SubscribeEvent
    public static void onEntityHurt(LivingDamageEvent.Post event){
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if(source.getEntity() instanceof Sans && entity.getEffect(MobEffectRegistry.KARMA) == null){
            entity.addEffect(new MobEffectInstance(MobEffectRegistry.KARMA,Integer.MAX_VALUE));
        }
    }
}
