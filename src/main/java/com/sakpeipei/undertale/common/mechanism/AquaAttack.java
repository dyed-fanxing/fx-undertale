package com.sakpeipei.undertale.common.mechanism;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * 水色攻击
 * @author yujinbao
 * @since 2025/9/24 10:10
 */
public class AquaAttack implements ColorAttack{

    private static final Logger log = LoggerFactory.getLogger(AquaAttack.class);

    @Override
    public boolean canHitEntity(Entity target){
        if(target instanceof ServerPlayer player){
            // 移动向量
            Vec3 knownMovement = player.getKnownMovement();
            if(knownMovement.horizontalDistanceSqr() > 1.0E-4 || Mth.square(knownMovement.y) > 0.02){
                log.debug("{}玩家移动，进行判定",player.getName());
            }
            return knownMovement.horizontalDistanceSqr() > 1.0E-4 || Mth.square(knownMovement.y) > 0.02;
        }
        double dx = target.getX() - target.xo;
        double dy = target.getY() - target.yo;
        double dz = target.getZ() - target.zo;
        return dx * dx + dy * dy + dz * dz > 2.5000003E-7;
    }

    @Override
    public Color getColor() {
        return Color.CYAN;
    }
}
