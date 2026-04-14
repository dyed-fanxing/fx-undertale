package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.persistentData.SoulMode;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class GravityHandler {
    private static final Logger log = LoggerFactory.getLogger(GravityHandler.class);

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
//        if(entity.getData(AttachmentTypes.GRAVITY_ACC.get()) != null) {
//            entity.addDeltaMovement(new Vec3(0,-0.08F,0));
//        }
        entity.addDeltaMovement(new Vec3(0,-entity.getData(AttachmentTypes.GRAVITY_ACC.get()),0));
    }
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if(entity.getData(AttachmentTypes.SOUL_MODE) == SoulMode.GRAVITY) {
            event.setCanceled(true);
        }
    }



    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        Entity affectedEntity = event.getAffectedEntity();
        Direction gravity = affectedEntity.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN){
            event.setKnockbackVelocity(GravityUtils.worldToLocal(gravity,event.getKnockbackVelocity()));
        }
    }

}
