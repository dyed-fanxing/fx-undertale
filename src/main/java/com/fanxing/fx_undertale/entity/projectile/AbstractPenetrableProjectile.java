package com.fanxing.fx_undertale.entity.projectile;

import com.fanxing.fx_undertale.utils.collsion.ProjectileUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author FanXing
 * @since 2025-09-26 21:05
 * 可跟随owner的弹射物
 * 与 AbstractHurtingProjectile的区别在于，可穿透攻击，即会检测路径上多个可命中实体，而不是第一个
 */
public abstract class AbstractPenetrableProjectile extends Projectile implements IEntityWithComplexSpawn {
    private static final Logger log = LoggerFactory.getLogger(AbstractPenetrableProjectile.class);
    public float accelerationPower;
    protected int lifetime;
    public AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level) {
        this(type, level,0.1f,200);
    }
    public AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level,float accelerationPower,int lifetime) {
        super(type, level);
        this.accelerationPower = accelerationPower;
        this.lifetime = lifetime;
    }


    @Override
    public void tick() {
        Entity entity = this.getOwner();
        if (this.level().isClientSide || (entity == null || !entity.isRemoved()) && this.level().isLoaded(this.blockPosition()) && this.tickCount < lifetime) {
            super.tick();
            BlockHitResult blockHitResult = getBlockHitResult();
            List<HitResult> hitResults = new ArrayList<>(getEntityHitResults(blockHitResult.getLocation()));
            hitResults.add(blockHitResult);
            for (HitResult hitResult : hitResults) {
                if (hitResult.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hitResult)) {
                    ProjectileDeflection projectileDeflection = this.hitTargetOrDeflectSelf(hitResult);
                    this.hasImpulse = true;
                    if(projectileDeflection != ProjectileDeflection.NONE){
                        break;
                    }
                }
            }
            this.checkInsideBlocks();
            Vec3 vec3 = this.getDeltaMovement();
            double speedSqr = vec3.lengthSqr();
            // 有位移时才更新旋转，否则由子类决定没位移时的朝向
            if(speedSqr != 0){
                this.updateRotation();
            }
            double d0 = this.getX() + vec3.x;
            double d1 = this.getY() + vec3.y;
            double d2 = this.getZ() + vec3.z;
            this.setPos(d0, d1, d2);
            spawnTrailParticle();

            // 下一tick处理的速度
            float f;
            if (this.isInWater()) {
                for(int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.BUBBLE, d0 - vec3.x * (double)0.25F, d1 - vec3.y * (double)0.25F, d2 - vec3.z * (double)0.25F, vec3.x, vec3.y, vec3.z);
                }
                f = this.getLiquidInertia();

            } else {
                f = this.getInertia();
            }
            if(!this.isNoGravity()){
                this.applyGravity();
            }
            this.setDeltaMovement(vec3.add(vec3.normalize().scale(this.accelerationPower)).scale(f));
        } else {
            this.discard();
        }
    }

    protected BlockHitResult getBlockHitResult() {
        AABB boundingBox = this.getBoundingBox();
        Vec3 from = boundingBox.getCenter();
        Vec3 velocity = this.getDeltaMovement();
        return this.level().clip(new ClipContext(from, from.add(velocity), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }
    protected List<EntityHitResult> getEntityHitResults(Vec3 to) {
        return ProjectileUtils.getEntityHitResults(this, this.getBoundingBox().getCenter(), to, this::canHitEntity);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return super.canHitEntity(entity) && !ownedBy(entity) && !(entity instanceof TraceableEntity traceable && traceable.getOwner() == getOwner());
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float p_341906_) {
        return !this.isInvulnerableTo(damageSource);
    }

    protected float getInertia() {
        return 0.95F;
    }

    protected float getLiquidInertia() {
        return 0.8F;
    }


    protected void spawnTrailParticle() {
        Vec3 position = position();
        ParticleOptions particleoptions = this.getTrailParticle();
        if (particleoptions != null) {
            this.level().addParticle(particleoptions, position.x, position.y + this.getBbHeight()*0.5f, position.z, 0, 0, 0);
        }
    }
    protected ParticleOptions getTrailParticle() {
        return com.fanxing.fx_undertale.registry.ParticleTypes.CUSTOM_WHITE_ASH.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double r) {
        double d0 = this.getBoundingBox().getSize() * (double)2.0F;
        if (Double.isNaN(d0)) {
            d0 = 4.0F;
        }

        d0 *= 64.0F;
        return r < d0 * d0;
    }

    /**
     * 获取实体添加进世界的数据包，将服务端实体的速度和拥有者也加入
     */
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        Entity owner = this.getOwner();
        int ownerId = owner == null ? 0 : owner.getId();
        Vec3 vec3 = serverEntity.getPositionBase();
        return new ClientboundAddEntityPacket(this.getId(), this.getUUID(), vec3.x(), vec3.y(), vec3.z(), serverEntity.getLastSentXRot(), serverEntity.getLastSentYRot(), this.getType(), ownerId, serverEntity.getLastSentMovement(), 0.0F);
    }

    /**
     * 客户端添加实体包，速度和拥有者设置
     */
    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 vec3 = new Vec3(packet.getXa(), packet.getYa(), packet.getZa());
        this.setDeltaMovement(vec3);

    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("accelerationPower")) {
            this.accelerationPower = tag.getFloat("accelerationPower");
        }
        if (tag.contains("lifetime")) {
            this.lifetime = tag.getInt("lifetime");
        }
    }
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("accelerationPower", this.accelerationPower);
        tag.putInt("lifetime", this.lifetime);
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(accelerationPower);
        buffer.writeInt(lifetime);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        this.accelerationPower = buffer.readFloat();
        this.lifetime = buffer.readInt();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }
}
