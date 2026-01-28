package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.entity.AttackColored;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.utils.ProjectileUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import com.sakpeipei.undertale.utils.TimeOfImpactUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.UUID;

/**
 * 地面移动骨骼 - 作为环境危险实体而非弹射物
 *
 * @author Sakqiongzi
 * @since 2025-10-06 21:18
 */
public class MovingGroundBone extends Entity implements IEntityWithComplexSpawn, AttackColored, TraceableEntity, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(MovingGroundBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerUUID;
    private LivingEntity owner;

    private float heightScale = 1.0f;

    private int delay = 10;
    private float speed = 1.0f;

    private float damage = 1.0f;
    private ColorAttack colorAttack = ColorAttack.WHITE;


    public MovingGroundBone(EntityType<? extends MovingGroundBone> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    /**
     * 延迟移动，无需传递移动向量
     */
    public MovingGroundBone(Level level, LivingEntity owner, int delay, float heightScale, float speed, float damage, ColorAttack colorAttack) {
        this(EntityTypeRegistry.MOVING_GROUND_BONE.get(), level);
        this.setNoGravity(true);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.delay = delay;
        this.heightScale = heightScale;
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        refreshDimensions();
    }
    /**
     * 立即移动，需要传递移动向量，无需相对位置
     * velocity 移动向量，单位化的
     */
    public MovingGroundBone(Level level, LivingEntity owner, float heightScale, float speed,Vec3 velocity, float damage, ColorAttack colorAttack) {
        this(EntityTypeRegistry.MOVING_GROUND_BONE.get(), level);
        this.setNoGravity(true);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.delay = 0;
        this.setDeltaMovement(velocity.scale(speed));
        this.heightScale = heightScale;
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        refreshDimensions();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        // 保持高度缩放
        return super.getDimensions(pose).scale(1f, heightScale);
    }

    @Override
    public void tick() {
        super.tick();
        delay--;
        if (delay < 0) {
            if (TimeOfImpactUtils.isBlockCollide(this, ClipContext.Block.COLLIDER)) {
                this.discard();
                return;
            }
            if (!this.level().isClientSide) {
                for (EntityHitResult entityHitResult : ProjectileUtils.getEntityHitResultsOnMoveVector(this, this::canHitEntity, false)) {
                    if (entityHitResult.getType() != HitResult.Type.MISS && entityHitResult.getEntity() instanceof LivingEntity entity) {
                        applyDamageTo(entity);
                    }
                }
            }
            this.checkInsideBlocks();

            Vec3 vec3 = this.getDeltaMovement();
            this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
            // 更新旋转（可选，用于视觉效果）
            if (!vec3.equals(Vec3.ZERO)) {
                RotUtils.lookVec(this, vec3);
            }
        }

        if (delay == 0 && !this.level().isClientSide) {
            this.setDeltaMovement(this.getLookAngle().scale(speed));
            this.hasImpulse = true;
        }

    }
    /**
     * 判断是否可命中实体
     */
    private boolean canHitEntity(Entity entity) {
        return entity.isAlive() && entity != owner && colorAttack.canHitEntity(entity) && entity instanceof LivingEntity;
    }

    /**
     * 应用伤害到目标
     * 关键：使用自定义DamageSource，绕过弹射物伤害逻辑
     */
    private void applyDamageTo(LivingEntity target) {
        DamageSource damageSource;
        if (owner instanceof Sans) {
            damageSource = this.damageSources().source(DamageTypes.FRAME, this, owner);
        } else {
            damageSource = this.damageSources().indirectMagic(this, owner);
        }
        // 尝试伤害，返回是否成功（可用于后续逻辑，如命中后销毁）
        boolean hurtSuccess = target.hurt(damageSource, damage);

        // 可根据伤害结果执行额外逻辑
        if (hurtSuccess) {
            // 例如：播放命中音效、生成粒子等
            // 如果是非穿透攻击，可以在这里销毁自己
            // this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("ownerUUID", owner.getUUID());
        }
        tag.putInt("color", this.colorAttack.getColor().getRGB());
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putFloat("heightScale", heightScale);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains("ownerUUID")) {
            if (((ServerLevel) this.level()).getEntity(tag.getUUID("ownerUUID")) instanceof LivingEntity entity) {
                this.owner = entity;
            }
        }
        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
        if (tag.contains("heightScale")) {
            this.heightScale = tag.getFloat("heightScale");
            refreshDimensions();
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("color"));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.speed);
        buf.writeInt(this.delay);
        buf.writeFloat(this.heightScale);
        buf.writeInt(this.colorAttack.getColor().getRGB());
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.heightScale = buf.readFloat();
        this.colorAttack = ColorAttack.getInstance(buf.readInt());
        refreshDimensions();
    }

    /**
     * 获取实体添加进世界的数据包，将服务端实体的速度也加入
     */
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        Entity entity = this.getOwner();
        int i = entity == null ? 0 : entity.getId();
        Vec3 vec3 = serverEntity.getPositionBase();
        return new ClientboundAddEntityPacket(this.getId(), this.getUUID(), vec3.x(), vec3.y(), vec3.z(), serverEntity.getLastSentXRot(), serverEntity.getLastSentYRot(), this.getType(), i, serverEntity.getLastSentMovement(), 0.0F);
    }

    /**
     * 客户端添加实体，进行额外的速度设置
     */
    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 vec3 = new Vec3(packet.getXa(), packet.getYa(), packet.getZa());
        this.setDeltaMovement(vec3);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public @Nullable Entity getOwner() {
        if(owner != null){
            return owner;
        }
        if(this.level() instanceof ServerLevel level && ownerUUID != null){
            owner = (LivingEntity) level.getEntity(ownerUUID);
        }
        return owner;
    }

    public float getHeightScale() {
        return heightScale;
    }

    @Override
    public Color getColor() {
        return colorAttack.getColor();
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


}