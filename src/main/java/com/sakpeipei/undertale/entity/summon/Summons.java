package com.sakpeipei.undertale.entity.summon;

import com.google.common.base.MoreObjects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class Summons extends Entity implements TraceableEntity {
    protected UUID ownerUUID;
    protected Entity owner;

    public Summons(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    public Summons(EntityType<?> entityType, Level level,Entity owner) {
        super(entityType, level);
        this.ownerUUID = owner.getUUID();
        this.owner = owner;
    }

    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return entity!=owner && !entity.isRemoved() && !(entity instanceof TraceableEntity traceable && traceable.getOwner() != owner);
    }

    protected abstract void onHitBlock(BlockHitResult hitResult);
    protected abstract void onHitEntity(Entity entity, Vec3 location);


    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getOwner(), this);
    }

    protected boolean ownedBy(Entity entity) {
        if(entity == null) return false;
        return this.ownerUUID.equals(entity.getUUID());
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public @Nullable Entity getOwner() {
        if(owner != null && !owner.isRemoved()){
            return owner;
        } else if( ownerUUID != null && this.level() instanceof ServerLevel level) {
            owner = level.getEntity(ownerUUID);
        }
        return owner;
    }


    @Override
    public void restoreFrom(@NotNull Entity entity) {
        super.restoreFrom(entity);
        if(entity instanceof Summons summons){
            this.owner = summons.owner;
        }
    }


    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("ownerUUID", this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("ownerUUID")) {
            this.ownerUUID = tag.getUUID("ownerUUID");
        }
    }

}
