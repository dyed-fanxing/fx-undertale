package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.DamageTypes;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.IOBBCapability;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.EntityCollisionUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LateralBone extends Summons implements IOBBCapability,IEntityWithComplexSpawn, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float scale = 1.0f;         // 整体缩放
    private float growScale = 1.0f;     // 基于整体的增长缩放，即头部和尾部大小不变，中间变高


    private int delay = 10;
    private float speed = 1.0f;

    private float damage = 1.0f;
    private ColorAttack colorAttack = ColorAttack.WHITE;

    private OBB obb;


    public LateralBone(EntityType<? extends LateralBone> type, Level level) {
        super(type, level);
    }
    /**
     * 延迟移动，无需传递移动向量
     */
    public LateralBone(Level level, LivingEntity owner, float growScale, int delay, float speed, float damage, ColorAttack colorAttack) {
        super(EntityTypes.LATERAL_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.growScale = growScale;
        this.delay = delay;
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        updateOBB();
    }
    /**
     * 立即移动，需要传递移动向量，无需相对位置
     * velocity 移动向量，单位化的
     */
    public LateralBone(Level level, LivingEntity owner,float scale, float growScale, float speed, Vec3 velocity, float damage, ColorAttack colorAttack) {
        super(EntityTypes.LATERAL_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.delay = 0;
        this.setDeltaMovement(velocity.scale(speed));
        RotUtils.lookVec(this, velocity);
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        updateOBB();
    }


    @Override
    public void setPos(double p_20210_, double p_20211_, double p_20212_) {
        super.setPos(p_20210_, p_20211_, p_20212_);
        updateOBB();
    }

    @Override
    protected void setRot(float p_19916_, float p_19917_) {
        super.setRot(p_19916_, p_19917_);
        updateOBB();
    }
    /**
     * 更新OBB（根据当前位置和旋转）
     */
    private void updateOBB() {
        float bbHeight = this.getBbHeight();
        float bbWidth = getBbWidth();
        float xHalfSize = scale*growScale*0.5f;
        this.obb = new OBB(this.position().add(0,0.15625*scale,0),
                xHalfSize,
                bbHeight * scale*0.5f,
                bbWidth * scale*0.5f,
                this.getViewVector(1.0f), this.getUpVector(1.0f)
        );
    }

    @Override
    public @NotNull OBB getOBB() {
        return obb;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return obb.getBoundingAABB();
    }

    @Override
    public void tick() {
        super.tick();
        delay--;
        if(delay < 0){
            Entity owner = getOwner();
            if (this.level().isClientSide || (owner == null || !owner.isRemoved()) && this.level().isLoaded(this.blockPosition())) {
                super.tick();
                Vec3 velocity = this.getDeltaMovement();
//                if (TimeOfImpactUtils.isCollide(this.level(),obb,velocity, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.of(this))) {
//                    this.discard();
//                    return;
//                }
                if (!this.level().isClientSide) {
                    for (EntityHitResult hitResult : EntityCollisionUtils.getEntityHitResults(this,obb,velocity,this::canHitEntity)) {
                        if (hitResult.getType() != HitResult.Type.MISS) {
                            onHitEntity(hitResult.getEntity(),hitResult.getLocation());
                        }
                    }
                }

                this.checkInsideBlocks();
                this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
                // 更新旋转（可选，用于视觉效果）
                if (!velocity.equals(Vec3.ZERO)) {
                    RotUtils.lookVec(this, velocity);
                }
            }
        }
        if (delay == 0 && !this.level().isClientSide) {
            this.setDeltaMovement(this.getLookAngle().scale(speed));
            this.hasImpulse = true;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {

    }

    @Override
    protected void onHitEntity(Entity entity, Vec3 location) {
        DamageSource damageSource;
        if (getOwner() instanceof Sans) {
            damageSource = this.damageSources().source(DamageTypes.FRAME, this, owner);
        } else {
            damageSource = this.damageSources().indirectMagic(this, owner);
        }
        // 尝试伤害，返回是否成功（可用于后续逻辑，如命中后销毁）
        boolean hurtSuccess = entity.hurt(damageSource, damage);

        // 可根据伤害结果执行额外逻辑
        if (hurtSuccess) {
            // 例如：播放命中音效、生成粒子等
            // 如果是非穿透攻击，可以在这里销毁自己
            // this.discard();
        }
    }


    public float getScale() {
        return scale;
    }
    public float getGrowScale() {
        return growScale;
    }


    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putInt("color", this.colorAttack.getColor());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("scale")) {
            this.scale = tag.getFloat("scale");
        }
        if (tag.contains("growScale")) {
            this.growScale = tag.getFloat("growScale");
        }

        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.of(tag.getInt("color"));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.speed);
        buf.writeInt(this.delay);
        buf.writeFloat(scale);
        buf.writeFloat(this.growScale);
        buf.writeInt(this.colorAttack.getColor());
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.scale  = buf.readFloat();
        this.growScale = buf.readFloat();
        this.colorAttack = ColorAttack.of(buf.readInt());
        updateOBB();
    }
    /**
     * 获取实体添加进世界的数据包，将服务端实体的速度也加入
     */
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        Entity entity = this.getOwner();
        int i = entity == null ? 0 : entity.getId();
        Vec3 velocity = serverEntity.getPositionBase();
        return new ClientboundAddEntityPacket(this.getId(), this.getUUID(), velocity.x(), velocity.y(), velocity.z(), serverEntity.getLastSentXRot(), serverEntity.getLastSentYRot(), this.getType(), i, serverEntity.getLastSentMovement(), 0.0F);
    }

    /**
     * 客户端添加实体，进行额外的速度设置
     */
    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 velocity = new Vec3(packet.getXa(), packet.getYa(), packet.getZa());
        this.setDeltaMovement(velocity);
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
