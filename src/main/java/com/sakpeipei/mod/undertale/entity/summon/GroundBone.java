package com.sakpeipei.mod.undertale.entity.summon;

import com.sakpeipei.mod.undertale.common.DamageTypes;
import com.sakpeipei.mod.undertale.entity.AttackColored;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class GroundBone extends Entity implements GeoEntity, IEntityWithComplexSpawn, AttackColored {
    private static final Logger log = LogManager.getLogger(GroundBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ColorAttack colorAttack;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    private float damage;
    private int delay;
    private double startY;
    private boolean isPlaySound;
    private int lifetime;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
    }

    public GroundBone(Level level, LivingEntity owner, float damage,int delay,double x, double y, double z) {
        this(level, owner, damage,delay,x,y,z, ColorAttack.WHITE,false,10);
    }

    public GroundBone(Level level, LivingEntity owner, float damage,int delay,double x, double y, double z, ColorAttack colorAttack,boolean isPlaySound,int lifetime) {
        this(EntityTypeRegistry.GROUND_BONE.get(), level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.delay = delay;
        this.startY = y - this.getBbHeight();
        setPos(x,startY,z);
        this.colorAttack = colorAttack;
        this.isPlaySound = isPlaySound;
        this.lifetime = lifetime;
    }


    @Override
    public void tick() {
        super.tick();
        delay--;
        if(delay == 0 && isPlaySound){
            this.level().playSound(null,this.getX(),this.getY(),this.getZ(), SoundRegistry.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
        }
        if (delay >= -lifetime && delay < 0) {
            float progress = (float) (-delay) / lifetime;
            float easedProgress = (float) Math.sin(progress * Math.PI); // 从0到1再到0

            setPos(getX(), startY + this.getBbHeight() * easedProgress, getZ());
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
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("Color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("Color"));
        }
        if(tag.contains("StartY")){
            this.startY = tag.getDouble("StartY");
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
        tag.putInt("Color",this.colorAttack.getColor().getColor());
        tag.putDouble("StartY",this.startY);
        tag.putBoolean("IsPlaySound",this.isPlaySound);
        tag.putInt("Lifetime",this.lifetime);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.delay);
        buf.writeInt(this.colorAttack.getColor().getColor());
        buf.writeDouble(this.startY);
        buf.writeInt(this.lifetime);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.delay = buf.readInt();
        this.colorAttack = ColorAttack.getInstance(buf.readInt());
        this.startY = buf.readDouble();
        this.lifetime = buf.readInt();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
