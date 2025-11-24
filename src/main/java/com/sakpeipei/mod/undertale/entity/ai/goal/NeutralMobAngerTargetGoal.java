package com.sakpeipei.mod.undertale.entity.ai.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-11-22 20:59
 * 中立生物设置生气目标Goal
 */
public class NeutralMobAngerTargetGoal extends Goal {
    NeutralMob mob;
    Level level;

    public NeutralMobAngerTargetGoal(NeutralMob mob, Level level) {
        this.mob = mob;
        this.level = level;
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() == null && mob.getPersistentAngerTarget() != null;
    }

    @Override
    public void start() {
        UUID angerTarget = mob.getPersistentAngerTarget();
        if(angerTarget != null && level instanceof ServerLevel serverLevel && serverLevel.getEntity(angerTarget) instanceof LivingEntity entity){
            mob.setTarget(entity);
        }
    }
}
