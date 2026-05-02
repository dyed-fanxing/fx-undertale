package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.boss.sans.SansAi;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.lib.client.render.component.TrailFan;
import com.fanxing.lib.entity.capability.Growable;
import com.fanxing.lib.entity.capability.Scalable;
import com.fanxing.lib.util.CurvesUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DisplayBone extends Entity implements Scalable, Growable, IEntityWithComplexSpawn,GeoEntity {
    private int lifetime = 5; // 0.5秒
    private float scale;

    public TrailFan trail1;
    public TrailFan trail2;
    public DisplayBone(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public DisplayBone(Level level,int lifetime,float scale) {
        super(EntityTypes.DISPLAY_BONE.get(), level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.lifetime = lifetime;
        this.scale = scale;
    }


    @Override
    public void tick() {
        if(tickCount >= lifetime) {
            this.discard();
            return;
        }
        super.tick();
    }
    @Override
    public boolean shouldRenderAtSqrDistance(double r) {
        double d0 = this.getBoundingBox().getSize() * (double)2.0F;
        if (Double.isNaN(d0)) {
            d0 = 4.0F;
        }

        d0 *= 256F;
        return r < d0 * d0;
    }

    public float getScaleProgress(float partialTick) {
        return scale* CurvesUtils.riseHoldFallBezier((tickCount+partialTick)/lifetime,0.5F,1.0f);
    }
    @Override
    public float getGrowProgress(float partialTick) {
        return 1f;
    }
    @Override
    public float getScale() {
        return scale;
    }
    @Override
    public float getGrowScale() {
        return 1f;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if(tag.contains("lifetime")) this.lifetime = tag.getInt("lifetime");
        if(tag.contains("scale")) this.scale = tag.getFloat("scale");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}


    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        trail1 = new TrailFan(lifetime*0.5f).color(SansAi.ENERGY_AQUA.getLast());
        trail2 = new TrailFan(lifetime*0.5f).color(SansAi.ENERGY_AQUA.getLast());
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.lifetime);
        buf.writeFloat(this.scale);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.lifetime = buf.readInt();
        this.scale = buf.readFloat();
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}



}
