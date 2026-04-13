package com.fanxing.fx_undertale.client.render.component;

import org.joml.Vector3f;
import org.joml.Vector3f;

public class TrailPoint {
    public Vector3f position;
    public float createTime;

    public TrailPoint(Vector3f point, float time) {
        this.position = point;
        this.createTime = time;
    }

    @Override
    public String toString() {
        return "TrailPoint{" +
                "position=" + position +
                ", createTime=" + createTime +
                '}';
    }
}
