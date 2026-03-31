package com.fanxing.fx_undertale.entity.capability;

import com.fanxing.fx_undertale.net.packet.AnimPacket;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author FanXing
 * @since 2025-11-20 20:58
 */
public interface Animatable {
    int getAnimID();
    void setAnimID(int id);
    default void sendAnimPacket(int id){
        Entity entity = (Entity) this;
        PacketDistributor.sendToPlayersTrackingEntity(entity,new AnimPacket(entity.getId(),id,1f));
    }
}
