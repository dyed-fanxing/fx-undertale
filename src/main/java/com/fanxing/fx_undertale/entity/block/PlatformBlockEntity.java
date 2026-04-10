package com.fanxing.fx_undertale.entity.block;

import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.registry.BlockTypes;
import com.fanxing.fx_undertale.registry.EntityTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class PlatformBlockEntity extends Entity implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(PlatformBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    // 运动锚点，一般就为实体初始生成时候的位置
//    protected static final EntityDataAccessor<Vector3f> DATA_ANCHOR_POS = SynchedEntityData.defineId(PlatformBlockEntity.class, EntityDataSerializers.VECTOR3);
    private static final Logger log = LoggerFactory.getLogger(PlatformBlockEntity.class);

    protected PhysicsMotionModel motion = new PhysicsMotionModel();
    // 碰撞箱
    protected float widthScale = 1.0f;
    protected float heightScale = 1.0f;
    protected Vec3 anchorPos;
    protected float ease = 0f;
    protected boolean isSaved;

    public PlatformBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setBlockState(BlockTypes.PLATFORM_BLOCK.get().defaultBlockState());
    }
    public PlatformBlockEntity(Level level,float widthScale,float heightScale) {
        this(EntityTypes.PLATFORM_BLOCK_ENTITY.get(), level);
        this.widthScale = widthScale;
        this.heightScale = heightScale;
    }

    public PlatformBlockEntity motion(PhysicsMotionModel motion) {
        this.motion = motion;
        return this;
    }
    public PlatformBlockEntity initialVelocity(Vec3 velocity) {
        setDeltaMovement(velocity);
        this.hasImpulse = true;
        return this;
    }
    public PlatformBlockEntity anchorPos(Vec3 anchorPos){
        this.anchorPos = anchorPos;
        return this;
    }
    public PlatformBlockEntity ease(float ease){
        this.ease = ease;
        return this;
    }
    public PlatformBlockEntity isSaved(boolean isSaved){
        this.isSaved = isSaved;
        return this;
    }

    // 碰撞箱
    public void setSize(float widthScale, float heightScale) {
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        refreshDimensions();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose p_19975_) {
        return super.getDimensions(p_19975_).scale(this.widthScale*getProgress(1f), this.heightScale);
    }
    public float getProgress(float partialTick) {
        if(ease <= -2f && ease >-3f) {
            float angle = (tickCount + partialTick) * (ease*0.05f); // 0.2 = -ease * 0.1
            return (Mth.sin(angle) + 1) / 2f+0.3f;
        }
        return 1f;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 currentPos = this.position();
        Vec3 newVel = motion.update(currentPos, this.getDeltaMovement(),anchorPos, tickCount);
        this.setDeltaMovement(newVel);
        // 获取站在平台上的玩家
        AABB aboveBox = this.getBoundingBox().expandTowards(0,Mth.EPSILON,0);
        List<Entity> entities = this.level().getEntities(this, aboveBox,p -> {
            double dd = aboveBox.maxY - p.getBoundingBox().minY;
            return !p.isPassenger()&& dd*dd <= Mth.EPSILON;
        });
        this.setPos(this.position().add(newVel));
        for (Entity entity : entities) {
            Vec3 newPos = entity.position().add(newVel);
            entity.setPos(newPos);
        }
        refreshDimensions();
    }


    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (anchorPos == null) {
            this.anchorPos = this.position();
        }
    }

    protected Entity.@NotNull MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }



    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return super.shouldBeSaved() && isSaved;
    }

    // ---------- 外观 ----------
    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    public void setAnchorPos(Vec3 anchorPos) {
        this.anchorPos = anchorPos;
    }

    public Vec3 getAnchorPos() {
        return this.anchorPos;
    }

    public float getWidthScale() {
        return widthScale;
    }

    public float getHeightScale() {
        return heightScale;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK_STATE, Blocks.AIR.defaultBlockState());
//        builder.define(DATA_ANCHOR_POS, new Vector3f());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity, Block.getId(this.getBlockState()));
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setBlockState(Block.stateById(packet.getData()));
        this.blocksBuilding = true;
        double d0 = packet.getX();
        double d1 = packet.getY();
        double d2 = packet.getZ();
        this.setPos(d0, d1, d2);
    }


    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition p_351015_) {
        ResourceKey<Level> resourcekey = p_351015_.newLevel().dimension();
        ResourceKey<Level> resourcekey1 = this.level().dimension();
        boolean flag = (resourcekey1 == Level.END || resourcekey == Level.END) && resourcekey1 != resourcekey;
        Entity entity = super.changeDimension(p_351015_);
//        this.forceTickAfterTeleportToDuplicate = entity != null && flag;
        return entity;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("blockState", NbtUtils.writeBlockState(this.getBlockState()));
        if(anchorPos != null) tag.put("anchorPos", newDoubleList(anchorPos.x, anchorPos.y, anchorPos.z));
        tag.putFloat("widthScale", widthScale);
        tag.putFloat("heightScale", heightScale);
        this.motion.addAdditionalSaveData(tag);
        tag.putFloat("ease", ease);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("blockState")) setBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompound("blockState")));
        if (tag.contains("anchorPos")) {
            ListTag list = tag.getList("anchorPos", 6);
            this.anchorPos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.motion = PhysicsMotionModel.fromTag(tag);
        setSize(tag.getFloat("widthScale"), tag.getFloat("heightScale"));
        if(tag.contains("ease")) ease = tag.getFloat("ease");
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.motion.writeSpawnData(buf);
        buf.writeDouble(anchorPos.x);
        buf.writeDouble(anchorPos.y);
        buf.writeDouble(anchorPos.z);
        buf.writeFloat(widthScale);
        buf.writeFloat(heightScale);
        buf.writeFloat(ease);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.motion = PhysicsMotionModel.fromBuf(buf);
        this.anchorPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        setSize(buf.readFloat(), buf.readFloat());
        this.ease = buf.readFloat();
    }
}