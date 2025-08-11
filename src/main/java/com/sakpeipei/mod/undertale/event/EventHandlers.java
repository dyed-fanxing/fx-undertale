package com.sakpeipei.mod.undertale.event;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.GasterBlasterPro;
import com.sakpeipei.mod.undertale.entity.Sans;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.utils.Tags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Undertale.MODID)
public class EventHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * 注册需要属性的实体，即继承自LivingEntity
     */
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), GasterBlasterPro.createAttributes().build());
        event.put(EntityTypeRegistry.SANS.get(), Sans.createAttributes().build());
    }

    /**
     * 存活实体受伤事件之前
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event){
        DamageContainer container = event.getContainer();
        DamageSource source = event.getSource();
        if (source.is(Tags.GASTER_BLASTER_BYPASS)) {
            container.setPostAttackInvulnerabilityTicks(0);
        }
    }
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LOGGER.info("{},{},{},{},{}",
                event.getEntity(),
                event.getOriginalDamage(),
                event.getSource(),
                event.getNewDamage(),
                event.getReduction(DamageContainer.Reduction.ARMOR)
        );
    }


    @SubscribeEvent
    public static void onPlayer(PlayerEvent.PlayerLoggedInEvent event) {
//        event.getEntity().getPersistentData().remove(GasterBlasterProItem.GASTER_BLASTER_PRO_KEY);
//        LOGGER.info("玩家数据移除Pro后{}",event.getEntity().getPersistentData());
        LOGGER.info("玩家数据{}",event.getEntity().getPersistentData());

    }




}
