package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.persistentData.SoulMode;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Undertale.MOD_ID)
public class GravityHandler {
    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if(entity.getData(AttachmentTypes.SOUL_MODE) == SoulMode.GRAVITY) {
            entity.addDeltaMovement(new Vec3(0,-0.04F,0));
        }
    }
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if(entity.getData(AttachmentTypes.SOUL_MODE) == SoulMode.GRAVITY) {
            event.setCanceled(true);
        }
    }
}
