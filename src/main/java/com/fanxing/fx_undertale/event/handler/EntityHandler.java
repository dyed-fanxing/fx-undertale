package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.entity.summon.GroundBone;
import com.fanxing.fx_undertale.entity.summon.GroundBoneOBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public class EntityHandler {
    private static final Logger log = LoggerFactory.getLogger(EntityHandler.class);

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        Entity affectedEntity = event.getAffectedEntity();
        if(affectedEntity instanceof GroundBone || affectedEntity instanceof GroundBoneOBB){
            event.setKnockbackVelocity(new Vec3(0,0,0));
        }
    }
}
