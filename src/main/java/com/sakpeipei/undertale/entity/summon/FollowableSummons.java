package com.sakpeipei.undertale.entity.summon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;

public abstract class FollowableSummons extends Summons implements IEntityWithComplexSpawn {
    protected Vec3 relativePos;         // 跟随拥有者的相对位置
    protected boolean isFollow;




    // 核心插值属性
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYRot;
    protected double lerpXRot;

    public FollowableSummons(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public FollowableSummons(EntityType<?> entityType, Level level, Entity owner) {
        super(entityType, level, owner);
    }



    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }


    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (relativePos != null) {
            tag.put("relativePos", this.newDoubleList(relativePos.x, relativePos.y, relativePos.z));
        }
        tag.putBoolean("isFollow", isFollow);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.isFollow = tag.getBoolean("isFollow");
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.isFollow);
        if(relativePos != null) {
            buf.writeBoolean(true);
            buf.writeDouble(this.relativePos.x);
            buf.writeDouble(this.relativePos.y);
            buf.writeDouble(this.relativePos.z);
        }else{
            buf.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.isFollow = buf.readBoolean();
        if(buf.readBoolean()) {
            this.relativePos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
    }
}
