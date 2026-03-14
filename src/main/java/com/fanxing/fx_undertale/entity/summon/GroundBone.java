package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.ColoredAttacker;
import com.fanxing.fx_undertale.entity.IScalable;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.IOBB;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地面骨头 - 支持 OBB 碰撞检测
 *
 * @author FanXing
 * @since 2025-08-18
 */
public class GroundBone extends Summons implements GeoEntity, IEntityWithComplexSpawn, ColoredAttacker, IOBB<GroundBone>, IScalable<GroundBone> {

    private static final Logger log = LoggerFactory.getLogger(GroundBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 基础属性 ==========
    private ColorAttack colorAttack = ColorAttack.WHITE;
    private float damage = 1.0f;
    private int delay = 20;
    private int lifetime = 10;

    // ========== 缩放属性 ==========
    private float scale = 1.0f;      // 整体缩放
    private float growScale = 1.0f;  // 高度缩放

    // ========== 曲线属性 ==========
    private float holdTimeScale = 1f;

    // ========== OBB 碰撞 ==========
    private OBB obb;

    // ========== 构造函数 ==========

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
    }

    public GroundBone(Level level, LivingEntity owner, float scale, float growScale, float damage,int lifetime, int delay) {
        super(EntityTypes.GROUND_BONE.get(), level, owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.lifetime = lifetime;
        this.delay = delay;
        this.damage = damage;
    }
    // ========== 链式配置方法 ==========
    public GroundBone colorAttack(ColorAttack colorAttack){
        this.colorAttack = colorAttack;
        return this;
    }
    public GroundBone gravity(Direction gravity) {
        this.setData(AttachmentTypes.GRAVITY, Gravity.applyGravity(this, gravity));
        return this;
    }
    public GroundBone holdTimeScale(float holdTimeScale) {
        this.holdTimeScale = holdTimeScale;
        return this;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return IScalable.super.getDimensions(pose).scale(1f, growScale * getProgress(0));
    }
    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        updateOBB();
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return IOBB.super.getBoundingBoxForCulling();
    }

    /**
     * 获取生长进度曲线
     */
    public float getProgress(float partialTick) {
        if (delay >= -lifetime && delay < 0) {
            return CurvesUtils.parametricHeight((-delay + partialTick) / lifetime, holdTimeScale,0.8f);
        }
        return 1f;
    }

    // ========== 核心逻辑 ==========
    @Override
    public void tick() {
        super.tick();
        delay--;
        // 生命周期内（从延迟结束到消失）
        if (delay >= -lifetime && delay <= 0) {
            this.refreshDimensions();
            if (!this.level().isClientSide) {
                AABB aabb = this.obb.getBoundingAABB();
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, aabb,
                        (target) -> this.canHitEntity(target) && (this.obb == null || this.obb.intersects(target.getBoundingBox())))) {
                    onHitEntity(target, null);
                }
            }
        }
        // 生命周期结束
        else if (delay < -lifetime) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && colorAttack.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(Entity entity, Vec3 location) {
        Entity owner = getOwner();
        DamageSource damageSource;
        if (owner instanceof Sans) {
            damageSource = damageSources().source(DamageTypes.FRAME, this, owner);
        } else if (owner != null) {
            damageSource = damageSources().indirectMagic(this, owner);
        } else {
            damageSource = damageSources().magic();
        }
        entity.hurt(damageSource, damage);
    }

    // ========== Getter ==========
    public float getScale() {
        return scale;
    }
    public float getGrowScale() {
        return growScale;
    }
    public int getDelay() {
        return delay;
    }
    public int getLifetime() {
        return lifetime;
    }
    public float getHoldTimeScale() {
        return holdTimeScale;
    }
    @Override
    public int getColor() {
        return colorAttack.getColor();
    }
    @Override
    public OBB getOBB() {
        return obb;
    }
    @Override
    public void setOBB(OBB obb) {
        this.obb = obb;
    }
    // ========== 物理参数 ==========
    @Override
    protected double getDefaultGravity() {
        return 0f;
    }

    // ========== 数据同步 ==========

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);
        tag.putInt("lifetime", lifetime);
        tag.putInt("delay", delay);
        tag.putInt("color", colorAttack.getColor());
        tag.putFloat("holdTimeScale", holdTimeScale);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("scale")) this.scale = tag.getFloat("scale");
        if (tag.contains("growScale")) this.growScale = tag.getFloat("growScale");
        if (tag.contains("lifetime")) this.lifetime = tag.getInt("lifetime");
        if (tag.contains("delay")) this.delay = tag.getInt("delay");
        if (tag.contains("color")) this.colorAttack = ColorAttack.of(tag.getInt("color"));
        if (tag.contains("holdTimeScale")) this.holdTimeScale = tag.getFloat("holdTimeScale");
        refreshDimensions();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.scale);
        buf.writeFloat(this.growScale);
        buf.writeInt(this.lifetime);
        buf.writeInt(this.delay);
        buf.writeInt(this.colorAttack.getColor());
        buf.writeFloat(this.holdTimeScale);
        buf.writeEnum(this.getData(AttachmentTypes.GRAVITY).getGravity());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.scale = buf.readFloat();
        this.growScale = buf.readFloat();
        this.lifetime = buf.readInt();
        this.delay = buf.readInt();
        this.colorAttack = ColorAttack.of(buf.readInt());
        this.holdTimeScale = buf.readFloat();
        this.setData(AttachmentTypes.GRAVITY, Gravity.applyGravity(this, buf.readEnum(Direction.class)));
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    // ========== GeoEntity ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}