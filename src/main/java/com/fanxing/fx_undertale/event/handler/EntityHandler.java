package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.entity.summon.GroundBone;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public class EntityHandler {
    private static final Logger log = LoggerFactory.getLogger(EntityHandler.class);

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        Entity affectedEntity = event.getAffectedEntity();
        if(affectedEntity instanceof GroundBone){
            event.setKnockbackVelocity(new Vec3(0,0,0));
        }
    }










    @SubscribeEvent
    public static void onEntityRefreshDimension(EntityEvent.Size event){
        EntityDimensions newSize = event.getNewSize();
        Entity entity = event.getEntity();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event){
        Entity entity = event.getEntity();
//        if(entity instanceof GasterBlaster){
//            entity.discard();
//            event.setCanceled(true);
//        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event){
        Entity entity = event.getEntity();
//        if(entity instanceof GroundBone){
//            entity.discard();
//        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event){
        Entity entity = event.getEntity();
        if(entity instanceof Player player){
//            log.info("玩家tick前{}的, 速度：{}，已知速度：{} ", player.tickCount,player.getDeltaMovement(),player.getKnownMovement());
//            log.info("玩家tick前{}的差分速度：({},{},{})",player.tickCount,player.getX()-player.xo,player.getY()-player.yo,player.getZ()-player.zo );
        }else if(entity instanceof IronGolem ironGolem){
//            log.info("铁傀儡tick前{}的 速度：{}，已知速度：{} ",ironGolem.tickCount,ironGolem.getDeltaMovement(),ironGolem.getKnownMovement());
//            log.info("铁傀儡tick前{}的 差分速度：({},{},{})",ironGolem.tickCount,ironGolem.getX()-ironGolem.xo,ironGolem.getY()-ironGolem.yo,ironGolem.getZ()-ironGolem.zo );
        }
    }
    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event){
        Entity entity = event.getEntity();
        if(entity instanceof Player player){
//            log.info("玩家tick后{}的 速度：{}，已知速度：{} ",player.tickCount,player.getDeltaMovement(),player.getKnownMovement());
//            log.info("玩家tick后{}的差分速度：({},{},{})",player.tickCount,player.getX()-player.xo,player.getY()-player.yo,player.getZ()-player.zo );
        }else if(entity instanceof IronGolem ironGolem){
//            log.info("铁傀儡tick后{}的 速度：{}，已知速度：{} ",ironGolem.tickCount,ironGolem.getDeltaMovement(),ironGolem.getKnownMovement());
//            log.info("铁傀儡tick后{}的 差分速度：({},{},{})",ironGolem.tickCount,ironGolem.getX()-ironGolem.xo,ironGolem.getY()-ironGolem.yo,ironGolem.getZ()-ironGolem.zo );
        }
    }
}
