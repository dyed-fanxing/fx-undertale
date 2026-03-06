package com.sakpeipei.undertale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.entity.boss.sans.Sans;
import com.sakpeipei.undertale.utils.GeoDebugHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;

@Mixin(value = GeoRenderer.class, remap = false)
public interface MixinGeoRenderer {

//    @Inject(method = "actuallyRender", at = @At("HEAD"), remap = false)
//    default void onActuallyRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
//        if (GeoDebugHelper.DEBUG_COUNTER.incrementAndGet() % 20 == 0 && animatable instanceof Sans) {
//            GeoDebugHelper.animatable = animatable;
//            Matrix4f modelView = RenderSystem.getModelViewMatrix();
//            GeoDebugHelper.DEBUG_LOGGER.info("=== RenderSystem.getModelViewMatrix() (actuallyRender HEAD) ===");
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", modelView.m00(), modelView.m01(), modelView.m02(), modelView.m03());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", modelView.m10(), modelView.m11(), modelView.m12(), modelView.m13());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", modelView.m20(), modelView.m21(), modelView.m22(), modelView.m23());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", modelView.m30(), modelView.m31(), modelView.m32(), modelView.m33());
//        }
//    }
//
//    @Inject(method = "renderCube", at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/renderer/GeoRenderer;createVerticesOfQuad(Lsoftware/bernie/geckolib/cache/object/GeoQuad;Lorg/joml/Matrix4f;Lorg/joml/Vector3f;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", remap = false))
//    default void onRenderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer,
//                              int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
//        if (GeoDebugHelper.DEBUG_COUNTER.incrementAndGet() % 20 != 0) return;
//
//        if(GeoDebugHelper.animatable != null) {
//            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
//            GeoDebugHelper.DEBUG_LOGGER.info("=== Cube poseState matrix ===");
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", poseState.m00(), poseState.m01(), poseState.m02(), poseState.m03());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", poseState.m10(), poseState.m11(), poseState.m12(), poseState.m13());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", poseState.m20(), poseState.m21(), poseState.m22(), poseState.m23());
//            GeoDebugHelper.DEBUG_LOGGER.info("[{}, {}, {}, {}]", poseState.m30(), poseState.m31(), poseState.m32(), poseState.m33());
//        }
//
//    }
//
//    @Inject(method = "createVerticesOfQuad", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V", remap = false))
//    default void onAddVertex(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
//                             int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
//        if (GeoDebugHelper.DEBUG_COUNTER.incrementAndGet() % 20 != 0) return;
//        if(GeoDebugHelper.animatable != null) {
//            for (GeoVertex vertex : quad.vertices()) {
//                Vector3f localPos = vertex.position();
//                Vector4f worldPos = poseState.transform(new Vector4f(localPos.x(), localPos.y(), localPos.z(), 1.0f));
//                GeoDebugHelper.DEBUG_LOGGER.info("Vertex: local({}, {}, {}) -> world({}, {}, {})",
//                        localPos.x(), localPos.y(), localPos.z(),
//                        worldPos.x(), worldPos.y(), worldPos.z());
//            }
//        }
//    }
}