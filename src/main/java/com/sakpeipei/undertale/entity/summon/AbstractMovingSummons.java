package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.utils.ProjectileUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import com.sakpeipei.undertale.utils.TimeOfImpactUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

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
        if (this.level().isClientSide || (owner == null || !owner.isRemoved()) && this.level().isLoaded(this.blockPosition())) {
            super.tick();
            AABB boundingBox = this.getBoundingBox();
            Vec3 from = boundingBox.getCenter();
            Vec3 velocity = this.getDeltaMovement();
            Vec3 to = from.add(velocity);
            AABB searchArea = this.getBoundingBox().expandTowards(velocity);
            if (TimeOfImpactUtils.isBlockCollide(this.level(),boundingBox,searchArea,velocity,getClipType(), ClipContext.Fluid.NONE, CollisionContext.of(this))) {
                this.discard();
                return;
            }
            if (!this.level().isClientSide) {
                for (EntityHitResult hitResult : ProjectileUtils.getEntityHitResults(this,from,to,searchArea, this::canHitEntity)) {
                    if (hitResult.getType() != HitResult.Type.MISS) {
                        onHitEntity(hitResult.getEntity(), hitResult.getLocation());
                    }
                }
            }

            this.checkInsideBlocks();
            this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
            // 更新旋转（可选，用于视觉效果）
            if (!velocity.equals(Vec3.ZERO)) {
                RotUtils.lookVec(this, velocity);
            }
        }
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
