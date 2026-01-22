package com.sakpeipei.undertale.entity;

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
