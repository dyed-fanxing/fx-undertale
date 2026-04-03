package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.utils.collsion.AABBCCDUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.fanxing.fx_undertale.utils.collsion.TimeOfImpactUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractMovingSummons extends Summons {

    public AbstractMovingSummons(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    public AbstractMovingSummons(EntityType<?> entityType, Level level, Entity owner) {
        super(entityType, level,owner);
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (this.level().isClientSide || (owner == null || !owner.isRemoved()) && this.level().isLoaded(this.blockPosition()) ) {
            super.tick();
            BlockHitResult blockHitResult = getBlockHitResult();
            for (EntityHitResult hitResult : getEntityHitResults(blockHitResult.getLocation())) {
                if (hitResult.getType() != HitResult.Type.MISS) {
                    onHitEntity(hitResult);
                }
            }
            onHitBlock(blockHitResult);
            this.checkInsideBlocks();
            Vec3 vec3 = this.getDeltaMovement();
            double speedSqr = vec3.lengthSqr();
            if(speedSqr != 0) {
                // 更新旋转（可选，用于视觉效果）
                updateRotation(vec3);
            }
            this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
        }
    }
    void updateRotation(Vec3 velocity) {
        RotUtils.lookVec(this, velocity);
    }
    protected BlockHitResult getBlockHitResult() {
        return TimeOfImpactUtils.getBlockHitResult(this.level(), this.getBoundingBox(),this.getDeltaMovement(), getClipType(), ClipContext.Fluid.NONE, CollisionContext.of(this));
    }
    protected List<EntityHitResult> getEntityHitResults(Vec3 to){
        return AABBCCDUtils.getEntityHitResults(this,this.getBoundingBox().getCenter(),to,this::canHitEntity);
    }

    protected ClipContext.Block getClipType() {
        return ClipContext.Block.COLLIDER;
    }

    /**
     * 获取实体添加进世界的数据包，将服务端实体的速度也加入
     */
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        Entity entity = this.getOwner();
        int i = entity == null ? 0 : entity.getId();
        Vec3 velocity = serverEntity.getPositionBase();
        return new ClientboundAddEntityPacket(this.getId(), this.getUUID(), velocity.x(), velocity.y(), velocity.z(), serverEntity.getLastSentXRot(), serverEntity.getLastSentYRot(), this.getType(), i, serverEntity.getLastSentMovement(), 0.0F);
    }

    /**
     * 客户端添加实体，进行额外的速度设置
     */
    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 velocity = new Vec3(packet.getXa(), packet.getYa(), packet.getZa());
        this.setDeltaMovement(velocity);
    }

}
