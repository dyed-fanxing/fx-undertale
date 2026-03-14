package com.fanxing.fx_undertale.entity;

import com.fanxing.fx_undertale.utils.ProjectileUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * 抽象的位置驱动实体基类（碰撞处理内联在 tick 中）
 * 每帧流程：
 * 1. 子类通过 {@link #computeDesiredPos()} 计算期望到达的位置。
 * 2. 基类自动进行碰撞检测（方块和实体），并按距离排序处理。
 * 3. 对于每次碰撞，调用对应的钩子方法 {@link #onHitBlock(BlockHitResult)} 和 {@link #onHitEntity(EntityHitResult)}，
 *    并根据返回值决定是否停止后续检测。
 * 4. 最后将实体设置到安全位置（如果碰撞方块则停在碰撞点，否则为期望位置）。
 */
public abstract class AbstractPosDrivenEntity extends Entity implements IEntityWithComplexSpawn {
    protected int penetrateCount = 1;

    public AbstractPosDrivenEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide || this.level().isLoaded(this.blockPosition())){
            super.tick();
            Vec3 to = computeDesiredPos();
            Vec3 from = this.position();

            // 方块碰撞检测
            BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 rayTestTo = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
            // 实体碰撞检测（沿截断后的路径）
            List<EntityHitResult> entityHits = getEntityHitResults(from, rayTestTo);
            // 合并所有碰撞并按距离排序
            entityHits.sort(Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(from)));
            for (EntityHitResult entityHit : entityHits) {
                if(penetrateCount > 0 && entityHit.getType() != HitResult.Type.MISS) {
                    penetrateCount--;
                    onHitEntity(entityHit);
                }else{
                    // 穿透次数用完停留在该位置
                    to = entityHit.getLocation();
                    break;
                }
            }
            if(blockHit.getType() != HitResult.Type.MISS) {
                onHitBlock(blockHit);
            }
            this.checkInsideBlocks();
            ParticleOptions particleoptions = this.getTrailParticle();
            if (particleoptions != null) {
                this.level().addParticle(particleoptions, to.x, to.y + this.getBbHeight()*0.5f, to.z, 0, 0, 0);
            }
            // 设置最终位置
            this.setPos(to);
        } else {
            this.discard();
        }
    }
    // ---------- 子类必须实现的抽象方法 ----------

    /**
     * 子类必须实现此方法，返回本 tick 期望移动到的位置（不考虑碰撞）。
     */
    protected abstract Vec3 computeDesiredPos();

    protected List<EntityHitResult> getEntityHitResults(Vec3 from, Vec3 to) {
        return ProjectileUtils.getEntityHitResults(this, from, to, this.getBoundingBox().expandTowards(to.subtract(from)), this::canHitEntity);
    }
    /**
     * 判断是否可击中某实体。子类可重写。
     */
    protected boolean canHitEntity(Entity entity) {
        return !entity.isSpectator() && entity.isAlive() && entity.isPickable();
    }

    /**
     * 当实体击中方块时调用。返回 true 表示停止继续处理后续碰撞（例如方块阻挡），false 则继续穿透。
     * 默认行为：销毁实体并返回 true。
     */
    protected void onHitBlock(BlockHitResult result) {}
    /**
     * 当实体击中实体时调用。返回 true 表示停止继续处理后续碰撞，false 则继续穿透。
     * 默认行为：对目标造成 1 点伤害，并返回 false（继续穿透）。
     */
    protected void onHitEntity(EntityHitResult result) {}

    protected ParticleOptions getTrailParticle() {
        return com.fanxing.fx_undertale.registry.ParticleTypes.CUSTOM_WHITE_ASH.get();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        Vec3 pos = serverEntity.getPositionBase();
        return new ClientboundAddEntityPacket(
                getId(), getUUID(), pos.x, pos.y, pos.z,
                serverEntity.getLastSentXRot(), serverEntity.getLastSentYRot(),
                getType(), 0, serverEntity.getLastSentMovement(), 0.0
        );
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
    }


    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("penetrateCount", penetrateCount);
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.penetrateCount = tag.getInt("penetrateCount");
    }
    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer){
        buffer.writeInt(penetrateCount);
    }
    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buffer){
        this.penetrateCount = buffer.readInt();
    }

}