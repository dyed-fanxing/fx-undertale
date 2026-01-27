package com.sakpeipei.undertale.entity.projectile;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.entity.AttackColored;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.utils.CollisionDetectionUtils;
import com.sakpeipei.undertale.utils.ProjectileUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.List;


/**
 * @author Sakqiongzi
 * @since 2025-10-06 21:18
 */
public class GroundBoneProjectile extends AbstractPenetrableProjectile implements IEntityWithComplexSpawn, AttackColored,GeoEntity, GeoAnimatable {
    private static final Logger log = LoggerFactory.getLogger(GroundBoneProjectile.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float heightScale = 1.0f;

    private float damage = 1.0f;
    private float speed = 1.0f;
    private int delay = 10;

    protected Vec3 movement;     // 运动向量
    private ColorAttack colorAttack = ColorAttack.WHITE;

    public GroundBoneProjectile(EntityType<? extends GroundBoneProjectile> type, Level level) {
        super(type, level);
    }
    public GroundBoneProjectile(Level level, LivingEntity owner,double x,double y,double z,float heightScale,float damage, float speed,  ColorAttack colorAttack) {
        this(EntityTypeRegistry.GROUND_BONE_PROJECTILE.get(), level);
        this.setNoGravity(true);
        setOwner(owner);
        this.heightScale = heightScale;
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        setPos(x,y,z);
        refreshDimensions();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(1f,heightScale);
    }

    @Override
    public void tick() {
        super.tick();
        delay--;
        if(!this.level().isClientSide) {
            if(delay == 0){
                this.shoot(movement.x,movement.y,movement.z, speed ,0);
            }
            if(delay < -200){
                this.discard();
            }
        }
    }

    @Override
    public boolean canHitEntity(@NotNull Entity entity){
        return super.canHitEntity(entity) && colorAttack.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        // 设置伤害逻辑
        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource;
            if(owner instanceof Sans ){
                damageSource = damageSources().source(DamageTypes.FRAME,this,owner);
            }else{
                damageSource = this.damageSources().indirectMagic(this, owner);
            }
            livingTarget.hurt(damageSource, damage);
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    public void delayShoot(int delay, @NotNull Vec3 movement) {
        this.delay = delay;
        this.movement = movement;
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
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("color",this.colorAttack.getColor().getRGB());
        tag.putInt("delay",delay);
        tag.putFloat("speed",speed);
        tag.putFloat("heightScale",heightScale);
        if(movement != null){
            tag.put("movement", this.newDoubleList(movement.x, movement.y, movement.z));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.delay = tag.getInt("delay");
        this.speed = tag.getFloat("speed");
        if(tag.contains("heightScale")){
            this.heightScale = tag.getFloat("heightScale");
            this.refreshDimensions();
        }
        if(tag.contains("color")){
            this.colorAttack = ColorAttack.getInstance(tag.getInt("color"));
        }
        if (tag.contains("movement")) {
            ListTag list = tag.getList("movement", 6);
            this.movement = new Vec3(list.getDouble(0),list.getDouble(1),list.getDouble(2));
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
        super.defineSynchedData(builder);
    }


    public float getHeightScale() {
        return heightScale;
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}