package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * @author Sakpeipei
 * @since 2025/11/18 14:44
 * 特效：仅客户端用于特效显示或渲染贴图等
 * 如：攻击预警提示框，魔法法阵贴图等
 */
@OnlyIn(Dist.CLIENT)
public abstract class Effect implements Tickable {
    protected int age;
    protected int lifetime;
    protected boolean removed;

    public Effect(int age, int lifetime) {
        this.age = age;
        this.lifetime = lifetime;
    }
    public Effect(int lifetime) {
        this(0,lifetime);
    }

    @Override
    public void tick(){
        if(!removed){
            age++;
            if(age >= lifetime){
                removed = true;
            }
        }
    }

    /**
     * 渲染方法 - 使用 RenderLevelStageEvent 的所有可用参数
     */
    public void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,Camera camera,Matrix4f modelViewMatrix, Matrix4f projectionMatrix){
        render(poseStack,partialTick,bufferSource,camera);
    }
    protected abstract void render(PoseStack poseStack,float partialTick,MultiBufferSource bufferSource,Camera camera);

    public boolean shouldRender(Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        AABB aabb = getBoundingBox();
        double disSqr = getBoundingBox().getCenter().distanceToSqr(cameraX, cameraY, cameraZ);
        // 在渲染距离内，且在视锥内
        return shouldRenderAtDis(disSqr) && frustum.isVisible(aabb);
    }

    protected boolean shouldRenderAtDis(double disSqr){
        double size = this.getBoundingBox().getSize();
        if (Double.isNaN(size)) {
            size = 1.0;
        }

        size *= 64.0 * getScale();
        return disSqr < size * size;
    }

    protected abstract AABB getBoundingBox();

    /**
     * 最大渲染距离的平方 - 可以和实体保持一致
     */
    protected double getScale() {
        return 1.0;
    }


    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public int getLifetime() {
        return lifetime;
    }
    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }
    public boolean isRemoved() {
        return removed;
    }
    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}
