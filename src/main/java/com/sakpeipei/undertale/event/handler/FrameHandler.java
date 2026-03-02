package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;


@EventBusSubscriber(modid = Undertale.MOD_ID)
public class FrameHandler {
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
}
