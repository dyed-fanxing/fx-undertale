package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.entity.AttackColored;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.utils.ProjectileUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import com.sakpeipei.undertale.utils.TimeOfImpactUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
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
import java.util.List;

/**
 * 地面移动骨骼 - 作为环境危险实体而非弹射物
 * @author Sakqiongzi
 * @since 2025-10-06 21:18
 */
public class MovingGroundBone extends Entity implements IEntityWithComplexSpawn, AttackColored,TraceableEntity, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(MovingGroundBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float heightScale = 1.0f;
    private float damage = 1.0f;
    private float speed = 1.0f;
    private int delay = 10;
    private ColorAttack colorAttack = ColorAttack.WHITE;
    private LivingEntity owner;


    public MovingGroundBone(EntityType<? extends MovingGroundBone> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public MovingGroundBone(Level level, LivingEntity owner,int delay,float heightScale,float speed, float damage, ColorAttack colorAttack) {
        this(EntityTypeRegistry.MOVING_GROUND_BONE.get(), level);
        this.owner = owner;
        this.delay = delay;
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
        if (!this.level().isClientSide) {
            if (delay == 0) {
                // 启动移动，但不使用弹射物的shoot方法
                this.setDeltaMovement(this.getLookAngle().scale(speed));
            }

            // 移动后执行CCD碰撞检测
            if (delay <= 0 && !this.getDeltaMovement().equals(Vec3.ZERO)) {
                if(TimeOfImpactUtils.getBlockHitResult(this, ClipContext.Block.COLLIDER).getType() == HitResult.Type.MISS){
                    log.debug("发生方块碰撞");
                    this.discard();
                    return;
                }
                List<EntityHitResult> entityHitResults = ProjectileUtils.getEntityHitResultsOnMoveVector(this, this::canHitEntity, false);
                for (EntityHitResult entityHitResult : entityHitResults) {
                    if(entityHitResult.getType() != HitResult.Type.MISS && entityHitResult.getEntity() instanceof LivingEntity entity){
                        applyDamageTo(entity);
                    }
                }

            }
        }

        // 应用移动（基于速度，而非弹射物逻辑）
        Vec3 delta = this.getDeltaMovement();
        this.setPos(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z);

        // 更新旋转（可选，用于视觉效果）
        if (!delta.equals(Vec3.ZERO)) {
            RotUtils.lookVec(this,this.getDeltaMovement());
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
            // 使用魔法或自定义伤害类型，确保不被盾牌完全格挡
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
        tag.putInt("color", this.colorAttack.getColor().getRGB());
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putFloat("heightScale", heightScale);
        if (owner != null) {
            tag.putUUID("ownerUUID", owner.getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.delay = tag.getInt("delay");
        this.speed = tag.getFloat("speed");
        if (tag.contains("heightScale")) {
            this.heightScale = tag.getFloat("heightScale");
            refreshDimensions();
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("color"));
        }
        if (tag.contains("ownerUUID")) {
            if (((ServerLevel)this.level()).getEntity(tag.getUUID("ownerUUID")) instanceof LivingEntity entity) {
                this.owner = entity;
            }
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.heightScale);
        buf.writeInt(this.colorAttack.getColor().getRGB());
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.heightScale = buf.readFloat();
        this.colorAttack = ColorAttack.getInstance(buf.readInt());
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        // 可在此添加需要与客户端同步的数据
    }

    // ========== Getter方法 ==========
    @Override
    public @Nullable Entity getOwner() {
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
        // 你的动画控制器逻辑
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


}