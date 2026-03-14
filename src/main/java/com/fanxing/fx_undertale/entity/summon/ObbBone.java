package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.ColoredAttacker;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.common.phys.OBB;
import com.fanxing.fx_undertale.entity.IOBB;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ObbBone extends AbstractMovingSummons implements IOBB, IEntityWithComplexSpawn, GeoEntity, ColoredAttacker {
    private static final Logger log = LoggerFactory.getLogger(ObbBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float scale = 1.0f;         // 整体缩放
    private float growScale = 1.0f;     // 基于整体的增长缩放，即头部和尾部大小不变，中间变高
    private int delay = 10;
    private float speed = 1.0f;

    private float damage = 1.0f;
    private ColorAttack colorAttack = ColorAttack.WHITE;
    private int lifetime = 20;
    private float holdTimeScale = 1.0f;
    private boolean isProjectile = false;


    private OBB obb;

    public ObbBone(EntityType<? extends ObbBone> type, Level level) {
        super(type, level);
    }
    public ObbBone(Level level, LivingEntity owner,float scale, float growScale,float damage) {
        super(EntityTypes.LATERAL_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.damage = damage;
        updateOBB();

    }

    public ObbBone delayShoot(int delay,float speed){
        this.delay = delay;
        this.speed = speed;
        return this;
    }
    public ObbBone shoot(Vec3 velocity){
        this.delay = 0;
        this.setDeltaMovement(velocity);
        RotUtils.lookVec(this, velocity);
        return this;
    }
    public ObbBone spine( int delay,int lifetime,float holdTimeScale){
        this.delay = delay;
        this.lifetime = lifetime;
        this.holdTimeScale = holdTimeScale;
        return this;
    }
    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return super.getDimensions(pose).scale(scale).scale(1f,growScale*getProgress(0));
    }
    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        updateOBB();
    }


    public float getProgress(float partialTick) {
        if (delay >= -lifetime && delay < 0) {
            return CurvesUtils.parametricHeight((-delay+partialTick) / lifetime, holdTimeScale,0f);
        }
        return 1f;
    }

    @Override
    public void tick() {
        delay--;
        if(delay < 0){
            super.tick();
        }
        if(isProjectile){
            if (delay == 0 &&!this.level().isClientSide) {
                this.setDeltaMovement(this.getLookAngle().scale(speed));
                this.hasImpulse = true;
            }
        }else{
            if (delay >= -lifetime && delay <= 0) {
                refreshDimensions();
                // 碰撞检测
                if (!this.level().isClientSide) {
                    for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,this.getOBB().getBoundingAABB(), (target)->this.canHitEntity(target) && this.obb.intersects(target.getBoundingBox()))) {
                        onHitEntity(target,null);
                    }
                }
            } else if (delay < -lifetime) {
                this.discard();
            }
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
    public int getDelay() {return delay;}
    public int getLifetime(){
        return lifetime;
    }
    public boolean isProjectile(){
        return isProjectile;
    }
    @Override
    public OBB getOBB() {
        return obb;
    }
    @Override
    public void setOBB(OBB obb) {
        this.obb = obb;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putInt("color", this.colorAttack.getColor());
        tag.putInt("lifetime", this.lifetime);
        tag.putFloat("holdTimeScale",holdTimeScale);
        tag.putBoolean("isProjectile", this.isProjectile);
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
        if(tag.contains("lifetime")){
            this.lifetime = tag.getInt("lifetime");
        }
        if(tag.contains("holdTimeScale")){
            this.holdTimeScale = tag.getFloat("holdTimeScale");
        }
        if(tag.contains("isProjectile")){
            this.isProjectile = tag.getBoolean("isProjectile");
        }
        refreshDimensions();
    }


    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.speed);
        buf.writeInt(this.delay);
        buf.writeFloat(scale);
        buf.writeFloat(this.growScale);
        buf.writeInt(this.colorAttack.getColor());
        buf.writeInt(this.lifetime);
        buf.writeFloat(this.holdTimeScale);
        buf.writeBoolean(this.isProjectile);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.scale  = buf.readFloat();
        this.growScale = buf.readFloat();
        this.colorAttack = ColorAttack.of(buf.readInt());
        this.lifetime = buf.readInt();
        this.holdTimeScale = buf.readFloat();
        this.isProjectile = buf.readBoolean();
        refreshDimensions();
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

    @Override
    public int getColor() {
        return colorAttack.getColor();
    }
}
