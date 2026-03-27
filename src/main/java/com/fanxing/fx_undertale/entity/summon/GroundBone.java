package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地面骨头 - 支持 OBB 碰撞检测
 * @author FanXing
 * @since 2025-08-18
 */
public class GroundBone extends AbstractBone<GroundBone> {

    private static final Logger log = LoggerFactory.getLogger(GroundBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int delay = 20;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
    }
    public GroundBone(Level level, LivingEntity owner, float scale, float growScale, float damage, int lifetime, int delay) {
        super(EntityTypes.GROUND_BONE.get(), level, owner,scale,growScale,lifetime,damage);
        this.delay = delay;
    }

    @Override
    public float getGrowProgress(float partialTick) {
        if (delay >= -lifetime && delay < 0) {
            if (holdTimeScale == -1f) { // 特殊值-1f使用sin曲线，这个比较符合重力猛摔后的骨刺刺出并返回的效果
                return Mth.sin(((-delay + partialTick)/ lifetime)*Mth.PI);
            } else {
                return CurvesUtils.parametricHeight((-delay + partialTick) / lifetime, holdTimeScale, 0.8f);
            }
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
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, obb == null ? this.getBoundingBox() : this.obb.getBoundingAABB(), this::canHitEntity)) {
                    onHitEntity(new EntityHitResult(target));
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
        return super.canHitEntity(entity) && (this.obb == null || this.obb.intersects(entity.getBoundingBox()));
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
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

    // ========== 物理参数 ==========
    @Override
    protected double getDefaultGravity() {
        return 0f;
    }

    public int getDelay() {
        return delay;
    }

    // ========== 数据同步 ==========
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("delay", delay);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("delay")) this.delay = tag.getInt("delay");
        refreshDimensions();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeInt(this.delay);
        buf.writeEnum(this.getData(AttachmentTypes.GRAVITY).getGravity());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.delay = buf.readInt();
        this.setData(AttachmentTypes.GRAVITY, Gravity.applyGravity(this, buf.readEnum(Direction.class)));
        refreshDimensions();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}