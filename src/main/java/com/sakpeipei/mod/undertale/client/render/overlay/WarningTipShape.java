package com.sakpeipei.mod.undertale.client.render.overlay;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author yujinbao
 * @since 2025/11/17 14:33
 */
public class WarningTipShape {
    private final List<Vec3> baseOutline;
    private final double eruptionHeight;
    private final int durationTicks;
    private int ageTicks = 0;
    private final UUID groupId;

    public WarningTipShape(List<Vec3> outline, double height, int duration, UUID groupId) {
        this.baseOutline = outline;
        this.eruptionHeight = height;
        this.durationTicks = duration;
        this.groupId = groupId;
    }

    public List<Vec3> getBaseOutline() {
        return baseOutline;
    }

    public List<Vec3> getTopOutline() {
        // 顶部轮廓 = 底部轮廓 + 钻出高度
        return baseOutline.stream()
                .map(point -> point.add(0, eruptionHeight, 0))
                .collect(Collectors.toList());
    }

    public UUID getGroupId() {
        return groupId;
    }

    public boolean shouldRender() {
        return ageTicks < durationTicks;
    }

    public void tick() {
        ageTicks++;
    }

    public float getAlpha() {
        float progress = (float) ageTicks / durationTicks;
        // 淡入淡出效果
        if (progress < 0.2f) return progress / 0.2f;
        if (progress > 0.8f) return 1.0f - (progress - 0.8f) / 0.2f;
        return 1.0f;
    }

    public AABB getBoundingBox() {
        if (baseOutline.isEmpty()) return null;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        // 计算包含底部和顶部的边界框
        for (Vec3 point : baseOutline) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        for (Vec3 point : getTopOutline()) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}