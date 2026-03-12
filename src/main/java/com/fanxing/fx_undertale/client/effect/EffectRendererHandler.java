package com.fanxing.fx_undertale.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sakpeipei
 * @since 2025/11/17 16:13
 * 装饰物渲染处理器
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID, value = Dist.CLIENT)
public class EffectRendererHandler {
    private final static List<Effect> EFFECTS = new ArrayList<>();
    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        if (EFFECTS.isEmpty()) {
            return;
        }

        EFFECTS.forEach(Effect::tick);
        EFFECTS.removeIf(Effect::isRemoved);
    }

    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (EFFECTS.isEmpty()) {
                return;
            }
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();
            Frustum frustum = event.getFrustum();
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            poseStack.pushPose();
            Vec3 cameraPos = camera.getPosition();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

            for (Effect effect : EFFECTS) {
                if (!effect.isRemoved()) {
                    if (effect.shouldRender(frustum,cameraPos.x, cameraPos.y, cameraPos.z)) {
                        effect.render(poseStack,partialTick,bufferSource, camera,event.getModelViewMatrix(),event.getProjectionMatrix());
                    }
                }
            }

            bufferSource.endBatch();
            poseStack.popPose();
        }
    }

    public static void addDecoration(Effect effect) {
        EFFECTS.add(effect);
    }
}