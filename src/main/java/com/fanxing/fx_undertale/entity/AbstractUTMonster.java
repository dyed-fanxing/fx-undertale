package com.fanxing.fx_undertale.entity;

import com.fanxing.fx_undertale.registry.SoundEvnets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


public abstract class AbstractUTMonster extends Monster {
    public float deathProgress;
    protected DamageSource deathSource;

    public AbstractUTMonster(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= getDeathTime() && !this.level().isClientSide() && !this.isRemoved()) {
            dropAllDeathLoot((ServerLevel) level(),deathSource);
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        this.setNoAi(true);
        deathSource = damageSource;
        super.die(damageSource);
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundEvnets.DEATH.get();
    }

    protected void dropAllDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource damageSource) {
        if(this.deathTime < getDeathTime()) return;
        super.dropAllDeathLoot(level, damageSource);
    }

    public int getDeathTime(){
        return 20;
    }
}