package com.sakpeipei.undertale.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.network.AnimPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

/**
 * @author Sakqiongzi
 * @since 2025-11-20 20:58
 */
public interface IAnimatable {
    byte getAnimID();
    void setAnimID(byte id);
    float getAnimSpeed();
    void setAnimSpeed(float speed);
}
