package com.sakpeipei.undertale.client.render.debug;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoveCollisionVisualizer {
    private static final List<DetectionData> activeDetections = new ArrayList<>();

    public static record DetectionData(
            Vec3 start,           // 射线起点
            Vec3 end,             // 射线终点
            Vec3 velocity,        // 速度向量
            AABB entityAABB,      // 实体原始碰撞箱
            AABB expandedAABB,    // 实体扩大后的碰撞箱
            Vec3 hitPoint,        // 命中点（如果有）
            int color,           // 颜色
            int lifeTicks        // 生命周期
    ) {}

    /**
     * 添加一次检测用于可视化
     */
    public static void addDetection(Vec3 start, Vec3 velocity, Entity entity, double inflateX, double inflateY, double inflateZ, AABB targetAABB) {
        Vec3 end = start.add(velocity);
        AABB expandedAABB = targetAABB.inflate(inflateX, inflateY, inflateZ);

        // 计算命中点
        Optional<Vec3> hitPoint = expandedAABB.clip(start, end);

        // 分配颜色：绿色=无碰撞，红色=有碰撞
        int color = hitPoint.isPresent() ? 0xFF0000 : 0x00FF00;

        activeDetections.add(new DetectionData(start, end, velocity, targetAABB, expandedAABB,hitPoint.orElse(null), color, 100));
    }

    /**
     * 在游戏内渲染可视化信息
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Frustum frustum = event.getFrustum();

        // 创建统一的 BufferSource
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(512));
        // 保存当前状态
        poseStack.pushPose();

        // 获取相机位置并应用偏移
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);


        // 恢复状态
        poseStack.popPose();
    }



}
