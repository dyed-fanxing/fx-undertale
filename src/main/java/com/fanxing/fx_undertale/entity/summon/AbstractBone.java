package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.entity.ColoredAttacker;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.fanxing.lib.entity.capability.Growable;
import com.fanxing.lib.entity.capability.OBBHolder;
import com.fanxing.lib.entity.capability.Scalable;
import com.fanxing.lib.entity.summon.AbstractMovingSummons;
import com.fanxing.lib.phys.OBB;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;

public abstract class AbstractBone<T extends AbstractBone<T>> extends AbstractMovingSummons
        implements Scalable, Growable, OBBHolder, ColoredAttacker, IEntityWithComplexSpawn, GeoEntity {

    private static final Logger log = LoggerFactory.getLogger(AbstractBone.class);
    // 通用属性
    protected float scale = 1.0f;
    protected float growScale = 1.0f;
    protected float holdTimeScale = 1f;
    protected float damage = 1.0f;
    protected OBB obb;
    protected ColorAttack colorAttack = ColorAttack.WHITE; // 可选
    protected int lifetime = 100;

    // 构造器
    public AbstractBone(EntityType<? extends AbstractBone<T>> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public AbstractBone(EntityType<? extends AbstractBone<T>> type, Level level, LivingEntity owner, float scale, float growScale, int lifetime, float damage) {
        super(type, level, owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.lifetime = lifetime;
        this.damage = damage;
    }

    // 链式配置方法（可被子类复用）
    public T holdTimeScale(float holdTimeScale) {
        this.holdTimeScale = holdTimeScale;
        return (T) this;
    }

    public T colorAttack(ColorAttack colorAttack) {
        this.colorAttack = colorAttack;
        return (T) this;
    }


    public T gravity(Direction gravity) {
        // 1. 当前姿态的四元数（YXZ 顺序）
        Quaternionf qCurrent = new Quaternionf().rotationYXZ(this.getYRot()*Mth.DEG_TO_RAD, this.getXRot()*Mth.DEG_TO_RAD, 0);
        // 3. 新姿态 = qGravity * qCurrent （左乘：先应用当前姿态，再整体旋转到新重力坐标系），转回欧拉角
        Vector3f euler = GravityUtils.getLocalToWorldF(gravity).mul(qCurrent).getEulerAnglesYXZ(new Vector3f());
        setYRot(euler.y * Mth.RAD_TO_DEG);
        setXRot(euler.x * Mth.RAD_TO_DEG);
        return (T) this;
    }
    // ========== 尺寸与 OBB ==========
    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return super.getDimensions(pose).scale(scale, scale * growScale * getGrowProgress(0));
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        return obb == null ?super.makeBoundingBox():obb.getBoundingAABB();
    }

    @Override
    public void updateOBB() {
        this.obb = OBB.fromFoot(this);
    }



    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return obb == null ? super.getBoundingBoxForCulling() : obb.getBoundingAABB();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && colorAttack.canHitEntity(entity);
    }

    @Override
    public boolean fudgePositionAfterSizeChange(EntityDimensions p_347526_) {
        return false;
    }

    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public float getScale() {
        return scale;
    }
    @Override
    public float getGrowScale() {
        return growScale;
    }
    @Override
    public OBB getOBB(float partialTicks) {
        return partialTicks == 1f?obb:OBB.fromFoot(this,partialTicks);
    }

    public ColorAttack getColorAttack() {
        return colorAttack;
    }
    @Override
    public int getColor() {
        return colorAttack.getColor();
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    // ========== 数据序列化（公共部分） ==========
    // 攻击物不需要持久化
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);
        tag.putFloat("holdTimeScale", holdTimeScale);
        tag.putFloat("damage", damage);
        tag.putInt("lifetime", lifetime);
        tag.putInt("color", colorAttack.getColor());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("scale")) scale = tag.getFloat("scale");
        if(tag.contains("growScale")) growScale = tag.getFloat("growScale");
        if(tag.contains("holdTimeScale")) holdTimeScale = tag.getFloat("holdTimeScale");
        if(tag.contains("damage")) damage = tag.getFloat("damage");
        if(tag.contains("lifetime")) lifetime = tag.getInt("lifetime");
        if(tag.contains("color")) colorAttack = ColorAttack.of(tag.getInt("color"));
    }

    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(scale);
        buf.writeFloat(growScale);
        buf.writeFloat(holdTimeScale);
        buf.writeFloat(damage);
        buf.writeInt(lifetime);
        buf.writeInt(colorAttack.getColor());
    }

    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        scale = buf.readFloat();
        growScale = buf.readFloat();
        holdTimeScale = buf.readFloat();
        damage = buf.readFloat();
        lifetime = buf.readInt();
        colorAttack = ColorAttack.of(buf.readInt());
    }

}
