package com.sakpeipei.undertale.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Sans 疲劳层
 * @author Sakpeipei
 * @since 2025/12/31 10:30
 */
public class SansFatigueLayer extends GeoRenderLayer<Sans>{
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/sans_sweat_1.png"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/sans_sweat_2.png"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/sans_sweat_3.png"),
    };
    public SansFatigueLayer(GeoRenderer<Sans> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, Sans animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        int stamina = animatable.getStamina();
        int sweat = -1;
        if(stamina == 0){
            sweat = 2;
        } else if(stamina <= animatable.getMaxStamina()/10){
            sweat = 1;
        } else if(stamina <= animatable.getMaxStamina()/2){
            sweat = 0;
        }
        if(sweat != -1){
            renderType = RenderType.entityCutoutNoCull(TEXTURES[sweat]);
            GeoRenderer<Sans> renderer = getRenderer();
            renderer.reRender(
                    bakedModel, poseStack, bufferSource, animatable,
                    renderType, bufferSource.getBuffer(renderType), partialTick,
                    packedLight, packedOverlay, renderer.getRenderColor(animatable, partialTick, packedLight).argbInt()
            );
        }
    }
}