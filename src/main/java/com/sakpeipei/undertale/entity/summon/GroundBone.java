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
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class GroundBone extends Summons implements GeoEntity, IEntityWithComplexSpawn, AttackColored {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ColorAttack colorAttack = ColorAttack.WHITE;
    private float damage = 1.0f;
    private int delay = 20;

    private boolean isPlaySound = false;
    private int lifetime = 10;
    private boolean isCurve = true;    // 曲线

    private float scale = 1.0f; // 整体缩放
    private float growScale = 1.0f; // 在基于整体缩放的基础上的高度缩放

    // 核心插值属性
    public int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type,level);
    }

    public GroundBone(Level level, LivingEntity owner, float damage,int delay) {
        this(level, owner,1.0f,1.0f,damage,10,delay, ColorAttack.WHITE,false,false);
    }

    public GroundBone(Level level, LivingEntity owner,float scale,float growScale,float damage,int lifetime,int delay,ColorAttack colorAttack,boolean isPlaySound,boolean isCurve) {
        super(EntityTypeRegistry.GROUND_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;

        this.lifetime = lifetime;
        this.delay = delay;
        this.damage = damage;
        this.colorAttack = colorAttack;
        this.isPlaySound = isPlaySound;
        this.isCurve = isCurve;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(scale).scale(1f,growScale*getBoneProgress());
    }
    public float getBoneProgress() {
        if(isCurve){
            if (delay >= -lifetime && delay < 0) {
                return Mth.sin((float) (-delay) / lifetime * Mth.PI);
            }
            return 0f;
        }else{
            return 1f;
        }
    }

    @Override
    public void tick() {
        super.tick();
        delay--;
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        if(this.level().isClientSide){
            if(delay == 0 && isPlaySound){
                this.level().playLocalSound(this,SoundRegistry.SANS_BONE_SPINE.get(), SoundSource.HOSTILE,1,1);
            }
        }
        if (delay >= -lifetime && delay < 0) {
            if(isCurve){
                this.refreshDimensions();
            }
            // 碰撞检测
            if (!this.level().isClientSide) {
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,this.getBoundingBox(), this::canHitEntity)) {
                    onHitEntity(target,null);
                }
            }
        } else if (delay < -lifetime) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return  super.canHitEntity(entity) && colorAttack.canHitEntity(entity);
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {

    }


    @Override
    protected void onHitEntity(Entity entity, Vec3 location) {
        Entity owner = getOwner();
        if (owner instanceof Sans) {
            entity.hurt(damageSources().source(DamageTypes.FRAME, this, owner), damage);
        } else {
            entity.hurt(damageSources().indirectMagic(owner, this), damage);
        }
    }

    public float getScale() {
        return scale;
    }
    public float getGrowScale() {
        return growScale;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isCurve() {
        return isCurve;
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
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);

        tag.putInt("lifetime",this.lifetime);
        tag.putInt("delay",this.delay);
        tag.putInt("color",this.colorAttack.getColor().getRGB());
        tag.putBoolean("isPlaySound",this.isPlaySound);
        tag.putBoolean("isCurve",this.isCurve);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("scale")) {
            this.scale = tag.getFloat("scale");
        }
        if (tag.contains("growScale")) {
            this.growScale = tag.getFloat("growScale");
        }
        if(tag.contains("lifetime")){
            this.lifetime = tag.getInt("lifetime");
        }
        if(tag.contains("delay")){
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("color"));
        }
        if(tag.contains("isPlaySound")){
            this.isPlaySound = tag.getBoolean("isPlaySound");
        }
        if(tag.contains("isCurve")){
            this.isCurve = tag.getBoolean("isCurve");
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.scale);
        buf.writeFloat(this.growScale);

        buf.writeInt(this.lifetime);
        buf.writeInt(this.delay);

        buf.writeInt(this.colorAttack.getColor().getRGB());
        buf.writeBoolean(this.isPlaySound);
        buf.writeBoolean(this.isCurve);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.scale  = buf.readFloat();
        this.growScale = buf.readFloat();

        this.lifetime = buf.readInt();
        this.delay = buf.readInt();

        this.colorAttack = ColorAttack.getInstance(buf.readInt());
        this.isPlaySound = buf.readBoolean();
        this.isCurve = buf.readBoolean();
        refreshDimensions();

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }



    private static final RawAnimation GROW = RawAnimation.begin().thenPlay("grow");
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack", 0, state -> {
            if(delay < 0 && isCurve){
                state.setAndContinue(GROW);
                state.setControllerSpeed((float) 20 /lifetime);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }




    /**
     * 分段参数方程实现
     * @param t 时间 (0到1)
     * @param holdTime 停留时间比例 (0到1)
     * @return 高度 (0到1)
     */
    public static float parametricHeight(float t, float holdTime) {
        // 定义分段
        float riseTime = (1.0f - holdTime) / 2.0f;
        if (t < riseTime) {
            // 上升阶段：使用二次贝塞尔曲线
            return bezier(t / riseTime, 0, 0.2f, 1);
        } else if (t < riseTime + holdTime) {
            // 停留阶段
            return 1.0f;
        } else {
            // 下降阶段：使用正弦衰减
            float t2 = (t - riseTime - holdTime) / riseTime;
            return (float) Math.cos(t2 * Math.PI / 2);
        }
    }
    // 二次贝塞尔曲线
    private static float bezier(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * p0 +
                2 * oneMinusT * t * p1 +
                t * t * p2;
    }
}
