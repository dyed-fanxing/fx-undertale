package com.sakpeipei.undertale.entity.summon;

import com.google.common.base.MoreObjects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public abstract class LivingSummons extends LivingEntity implements TraceableEntity {
    protected UUID ownerUUID;
    protected Entity owner;
    protected int ownerId = -1;



    protected LivingSummons(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
    protected LivingSummons(EntityType<? extends LivingEntity> entityType, Level level,LivingEntity owner) {
        super(entityType, level);
        if(owner != null){
            this.ownerUUID = owner.getUUID();
            this.owner = owner;
        }
    }

    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return entity!=owner && !entity.isRemoved() && !(entity instanceof TraceableEntity traceable && traceable.getOwner() == owner);
    }


    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getOwner(), this);
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
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("ownerUUID", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("ownerUUID")) {
            this.ownerUUID = tag.getUUID("ownerUUID");
        }
    }


    private static final Iterable<ItemStack> NO_ITEMS = List.of(ItemStack.EMPTY, ItemStack.EMPTY);
    private static final Iterable<ItemStack> NO_ARMOR = List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

    @Override
    public @NotNull Iterable<ItemStack> getHandSlots() {
        return NO_ITEMS;
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return NO_ARMOR;
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        // 可以留空，但为了安全可以忽略
    }


    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
