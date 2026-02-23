package com.sakpeipei.undertale.entity;

import com.sakpeipei.undertale.net.packet.AnimPacket;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author Sakqiongzi
 * @since 2025-11-20 20:58
 */
public interface IAnimatable {
    byte getAnimID();
    void setAnimID(byte id);
    default void sendAnimPacket(byte id){
        Entity entity = (Entity) this;
        PacketDistributor.sendToPlayersTrackingEntity(entity,new AnimPacket(entity.getId(),id,1f));
    }
}
