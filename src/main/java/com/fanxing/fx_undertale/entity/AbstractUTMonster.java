package com.fanxing.fx_undertale.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;


public abstract class AbstractUTMonster extends Monster {
    public AbstractUTMonster(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= getDeathTime() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource p_21014_) {
        super.die(p_21014_);
    }

    public int getDeathTime(){
        return 20;
    }
}