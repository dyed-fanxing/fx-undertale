    package com.sakpeipei.mod.undertale.client.render.entity;

    import com.mojang.blaze3d.vertex.PoseStack;
    import com.mojang.math.Axis;
    import com.sakpeipei.mod.undertale.client.model.entity.GasterBlasterFixedModel;
    import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
    import net.minecraft.client.renderer.MultiBufferSource;
    import net.minecraft.client.renderer.entity.EntityRendererProvider;
    import org.jetbrains.annotations.NotNull;
    import software.bernie.geckolib.renderer.GeoEntityRenderer;

    public class GasterBlasterFixedRender extends GeoEntityRenderer<GasterBlasterFixed> {
        public GasterBlasterFixedRender(EntityRendererProvider.Context renderManager) {
            super(renderManager, new GasterBlasterFixedModel());
        }
        @Override
        protected void applyRotations(GasterBlasterFixed animatable, PoseStack poseStack,
                                      float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees( animatable.getXRot()));
            super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        }

        @Override
        public void render(@NotNull GasterBlasterFixed entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            GasterBlasterBeamRenderer.renderFixed(entity,partialTick,poseStack,bufferSource,packedLight);
        }
}
