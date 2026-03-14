package com.fanxing.fx_undertale.client.render.summon;

import com.fanxing.fx_undertale.client.render.ColorAttackRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.client.model.entity.ObbBoneModel;
import com.fanxing.fx_undertale.common.phys.AxisQuaternionf;
import com.fanxing.fx_undertale.entity.summon.ObbBone;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * @author FanXing
 * @since 2025-08-18 20:58
 */
public class OBBBoneRender extends ColorAttackRenderer<ObbBone> {

    private static final Logger log = LoggerFactory.getLogger(OBBBoneRender.class);


    public OBBBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ObbBoneModel());
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, ObbBone animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(animatable.getScale(), animatable.getScale(), poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected void applyRotations(ObbBone animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        if(animatable.isProjectile()){
            poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot()));
            poseStack.mulPose(AxisQuaternionf.Z_NEG_90);
            poseStack.translate(-0.15625,-0.5f*animatable.getBbHeight(),0);
        }else{
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,-animatable.yRotO,-animatable.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick,animatable.xRotO,animatable.getXRot())));
        }
    }

    @Override
    public Color getRenderColor(ObbBone animatable, float partialTick, int packedLight) {
        Color original = super.getRenderColor(animatable, partialTick, packedLight);
        //todo 待做光影兼容，在光影下因自发光强度过曝
        return Color.ofARGB((int)(original.getAlpha() * 0.75f),original.getRed(),original.getGreen(),original.getBlue());
    }
}
