package com.sakpeipei.mod.undertale.mechanism;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.Color;

/**
 * 水色攻击
 * @author yujinbao
 * @since 2025/9/24 10:10
 */
public class AquaAttack implements ColorAttack{

    private static final Logger log = LogManager.getLogger(AquaAttack.class);

    @Override
    public boolean canHitEntity(Entity target){
        if(target instanceof ServerPlayer player){
            log.info("位移{},速度{},判定{}",player.getKnownMovement(),player.getKnownMovement().lengthSqr(),player.getKnownMovement().lengthSqr() > 1.0E-4);
            return player.getKnownMovement().lengthSqr() > 1.0E-4;
        }
        double dx = target.getX() - target.xo;
        double dy = target.getY() - target.yo;
        double dz = target.getZ() - target.zo;
        return dx * dx + dy * dy + dz * dz > (double)2.5000003E-7F;
    }

    @Override
    public Color getColor() {
        return new Color(0xFF42FCFF);
    }
}
