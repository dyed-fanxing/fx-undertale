package com.sakpeipei.mod.undertale.entity.projectile;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.AttackColored;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * @author Sakqiongzi
 * @since 2025-10-06 21:18
 */
public class GroundBoneProjectile extends AbstractPenetrableProjectile implements IEntityWithComplexSpawn, AttackColored,GeoEntity, GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ColorAttack colorAttack;
    protected Vec3 targetPos;     // 目标位置

    private float damage;
    private float speed;
    private int delay;

    public GroundBoneProjectile(EntityType<? extends GroundBoneProjectile> type, Level level) {
        super(type, level);
        this.colorAttack = ColorAttack.WHITE;
    }
    public GroundBoneProjectile(Level level, LivingEntity owner,double x,double y,double z, float damage, float speed, ColorAttack colorAttack) {
        this(EntityTypeRegistry.GROUND_BONE_PROJECTILE.get(), level);
        this.setNoGravity(true);
        setPos(x,y,z);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.level().isClientSide) {
            delay--;
            if(delay == 0){
                this.shoot(targetPos.x - this.getX(),targetPos.y - this.getY(),targetPos.z - this.getZ(), speed ,0);
            }
        }
    }

    @Override
    protected boolean isCollision() {
        return false;
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

    public void delayShoot(int delay, @NotNull Vec3 targetPos) {
        this.delay = delay;
        this.targetPos = targetPos;
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
        tag.putInt("Color",this.colorAttack.getColor().getColor());
        tag.putInt("Delay",delay);
        tag.putFloat("Speed",speed);
        if(targetPos != null){
            tag.put("TargetPos", this.newDoubleList(targetPos.x, targetPos.y, targetPos.z));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.delay = tag.getInt("Delay");
        this.speed = tag.getFloat("Speed");
        if(tag.contains("Color")){
            this.colorAttack = ColorAttack.getInstance(tag.getInt("Color"));
        }
        if (tag.contains("TargetPos")) {
            ListTag list = tag.getList("TargetPos", 6);
            this.targetPos = new Vec3(list.getDouble(0),list.getDouble(1),list.getDouble(2));
        }
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.colorAttack.getColor().getColor());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.colorAttack = ColorAttack.getInstance(buf.readInt());
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

