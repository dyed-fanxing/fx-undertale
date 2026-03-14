package com.fanxing.fx_undertale.entity.summon;

import com.google.common.base.MoreObjects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class Summons extends Entity implements TraceableEntity {
    private static final Logger log = LoggerFactory.getLogger(Summons.class);
    protected UUID ownerUUID;
    protected Entity owner;
    protected int ownerId = -1;

    public Summons(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    public Summons(EntityType<?> entityType, Level level,Entity owner) {
        super(entityType, level);
        if(owner != null){
            this.ownerUUID = owner.getUUID();
            this.owner = owner;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(this.level().isClientSide && owner == null && ownerId != -1){
            Entity owner = this.level().getEntity(ownerId);
            if(owner != null){
                setOwner(owner);
            }
        }
    }

    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return entity!=owner && !entity.isRemoved() && !(entity instanceof TraceableEntity traceable && traceable.getOwner() == owner);
    }

    protected void onHitBlock(BlockHitResult hitResult){
    }
    protected void onHitEntity(Entity entity, Vec3 location){
    }


    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getOwner(), this);
    }

    protected boolean ownedBy(Entity entity) {
        if(entity == null) return false;
        return this.ownerUUID.equals(entity.getUUID());
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public Entity getOwner() {
        if(owner != null && !owner.isRemoved()){
            return owner;
        } else if( ownerUUID != null && this.level() instanceof ServerLevel level) {
            owner = level.getEntity(ownerUUID);
        }
        return owner;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    /**
     * 获取添加实体数据包，用于服务端发送给客户端，进行加入世界同步数据
     * 同步拥有者
     */
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        Entity owner = this.getOwner();
        return new ClientboundAddEntityPacket(this, serverEntity, owner == null ? 0 : owner.getId());
    }

    /**
     * 客户端解析服务端发送的添加实体数据包
     * 设置拥有者
     */
    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        ownerId = packet.getData();
        Entity owner = this.level().getEntity(ownerId);
        if (owner != null) {
            this.setOwner(owner);
        }
    }

    @Override
    public void restoreFrom(@NotNull Entity entity) {
        super.restoreFrom(entity);
        if(entity instanceof Summons summons){
            this.owner = summons.owner;
        }
    }


    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("ownerUUID", this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("ownerUUID")) {
            this.ownerUUID = tag.getUUID("ownerUUID");
        }
    }

}
