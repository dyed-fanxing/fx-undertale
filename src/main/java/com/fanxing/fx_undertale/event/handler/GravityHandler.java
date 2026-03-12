package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.persistentData.SoulMode;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = FxUndertale.MOD_ID)
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
