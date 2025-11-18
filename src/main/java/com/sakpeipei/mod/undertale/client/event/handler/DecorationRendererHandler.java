package com.sakpeipei.mod.undertale.client.event.handler;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.render.decoration.Decoration;
import net.minecraft.client.Camera;
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

import static software.bernie.geckolib.GeckoLibConstants.LOGGER;

/**
 * @author yujinbao
 * @since 2025/11/17 16:13
 * 装饰物渲染处理器
 */
@EventBusSubscriber(modid = Undertale.MODID, value = Dist.CLIENT)
public class DecorationRendererHandler {
    private final static List<Decoration> DECORATIONS = new ArrayList<>();

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        if (DECORATIONS.isEmpty()) {
            return;
        }

        DECORATIONS.forEach(Decoration::tick);
        DECORATIONS.removeIf(Decoration::isRemoved);
    }

    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (DECORATIONS.isEmpty()) {
                return;
            }
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();
            Frustum frustum = event.getFrustum();

            // 创建统一的 BufferSource
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(512));
            poseStack.pushPose();

            // 统一的相机偏移（每个装饰物都需要）
            Vec3 cameraPos = camera.getPosition();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            // 遍历所有装饰物
            for (Decoration decoration : DECORATIONS) {
                if (!decoration.isRemoved()) {
                    if (decoration.shouldRender(frustum,cameraPos.x, cameraPos.y, cameraPos.z)) {
                        decoration.render(poseStack,partialTick,bufferSource, camera,event.getModelViewMatrix(),event.getProjectionMatrix());
                    }
                }
            }
            poseStack.popPose();
            bufferSource.endBatch(); // 统一的批处理提交
        }
    }

    public static void addDecoration(Decoration decoration) {
        DECORATIONS.add(decoration);
    }
}
