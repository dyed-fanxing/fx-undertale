package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.entity.AttackColored;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class GroundBone extends Entity implements GeoEntity, IEntityWithComplexSpawn, AttackColored {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ColorAttack colorAttack;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    private float damage;
    private int delay;

    private double startY;
    private double offset;
    private boolean isPlaySound;
    private int lifetime;



    // 核心插值属性
    public int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        this(level,null,1.0f,20,0,0,0);
    }

    public GroundBone(Level level, LivingEntity owner, float damage,int delay,double x, double y, double z) {
        this(level, owner, damage,delay,x,y,z,1.0f, ColorAttack.WHITE,false,10);
    }

    /**
     * @param offset 自身高度上升的比例 0 ~ 1 范围
     */
    public GroundBone(Level level, LivingEntity owner, float damage,int delay,double x, double y, double z,double offset, ColorAttack colorAttack,boolean isPlaySound,int lifetime) {
        super(EntityTypeRegistry.GROUND_BONE.get(), level);
        this.setNoGravity(true);
        if(owner != null) {
            setOwner(owner);
        }
        this.damage = damage;
        this.delay = delay;
        this.startY = y - this.getBbHeight() * offset;
        setPos(x,startY,z);
        this.offset = offset;
        this.colorAttack = colorAttack;
        this.isPlaySound = isPlaySound;
        this.lifetime = lifetime;
    }


    @Override
    public void tick() {
        super.tick();
        delay--;
        // 处理旋转插值
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        if(delay == 0 && isPlaySound){
            if(this.level().isClientSide){
                this.level().playLocalSound(this,SoundRegistry.SANS_BONE_SPINE.get(), SoundSource.HOSTILE,1,1);
            }
        }
        if (delay >= -lifetime && delay < 0) {
            float progress = (float) (-delay) / lifetime;
            float easedProgress = Mth.sin(progress * Mth.PI); // 从0到1再到0
            Vec3 pos = position();
            this.setPos(pos.x,startY + this.getBbHeight() * easedProgress,pos.z);
            // 碰撞检测
            if (!this.level().isClientSide) {
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,this.getBoundingBox(), this::canHitEntity)) {
                    onHitEntity(target);
                }
            }
        } else if (delay < -lifetime) {
            this.discard();
        }
    }
    private boolean canHitEntity(Entity entity) {
        return entity.isAlive() && entity != getOwner() && colorAttack.canHitEntity(entity);
    }

    private void onHitEntity(Entity entity) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            entity.hurt(damageSources().source(DamageTypes.FRAME, this), damage);
        } else {
            if (owner instanceof Sans) {
                entity.hurt(damageSources().source(DamageTypes.FRAME, this, owner), damage);
             } else {
                entity.hurt(damageSources().indirectMagic(owner, this), damage);
            }
        }
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }
    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity) entity;
            }
        }
        return this.owner;
    }
    @Override
    public Color getColor() {
        return colorAttack.getColor();
    }

    @Override
    protected double getDefaultGravity() {
        return 0f;
    }

    @Override
    public void lerpMotion(double p_37279_, double p_37280_, double p_37281_) {
        this.setDeltaMovement(p_37279_, p_37280_, p_37281_);
        if(this.xRotO == 0 && this.yRotO == 0){
            this.xRotO = getXRot();
            this.yRotO = getYRot();
        }
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
        return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
    }
    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("Color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("Color"));
        }
        if(tag.contains("Offset")){
            this.offset = tag.getDouble("Offset");
            this.startY = getY() - this.getBbHeight() * this.offset;
        }
        if(tag.contains("IsPlaySound")){
            this.isPlaySound = tag.getBoolean("IsPlaySound");
        }
        if(tag.contains("Lifetime")){
            this.lifetime = tag.getInt("Lifetime");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putInt("Color",this.colorAttack.getColor().getRGB());
        tag.putDouble("Offset",this.offset);
        tag.putBoolean("IsPlaySound",this.isPlaySound);
        tag.putInt("Lifetime",this.lifetime);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.delay);
        buf.writeInt(this.colorAttack.getColor().getRGB());
        buf.writeDouble(this.startY);
        buf.writeBoolean(this.isPlaySound);
        buf.writeInt(this.lifetime);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.delay = buf.readInt();
        this.colorAttack = ColorAttack.getInstance(buf.readInt());
        this.startY = buf.readDouble();
        this.isPlaySound = buf.readBoolean();
        this.lifetime = buf.readInt();

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
