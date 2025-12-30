package com.sakpeipei.undertale.entity;

import com.sakpeipei.undertale.network.AnimIDPacket;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author Sakqiongzi
 * @since 2025-11-20 20:58
 */
public interface IAnimatable {
    byte getAnimID();
    void setAnimID(byte id);

    /**
     * 服务端发送动画ID
     * @param id 动画ID
     */
    default void sendAnimId(byte id){
        Entity entity = (Entity) this;
        PacketDistributor.sendToPlayersTrackingEntity(entity,new AnimIDPacket(entity.getId(),id));
    }
}
