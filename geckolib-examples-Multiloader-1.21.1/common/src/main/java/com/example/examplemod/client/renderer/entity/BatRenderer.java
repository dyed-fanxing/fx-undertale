package com.example.examplemod.client.renderer.entity;

import com.example.examplemod.client.model.entity.BatModel;
import com.example.examplemod.entity.BatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * 用于渲染 {@link BatEntity} 的示例 {@link software.bernie.geckolib.renderer.GeoRenderer}
 * @see BatModel
 */
public class BatRenderer extends GeoEntityRenderer<BatEntity> {
	private int currentTick = -1; // 记录当前渲染的tick，避免重复生成粒子

	public BatRenderer(EntityRendererProvider.Context context) {
		super(context, new BatModel());

		// 添加发光层，使蝙蝠能够像鲁道夫（圣诞驯鹿）一样发光
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	// 在渲染时围绕蝙蝠耳朵生成粒子
	@Override
	public void renderFinal(PoseStack poseStack, BatEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay, int colour) {
		if (this.currentTick < 0 || this.currentTick != animatable.tickCount) {
			this.currentTick = animatable.tickCount;

			// 找到名为"leftear"的骨骼，以其位置为参考点生成粒子
			this.model.getBone("leftear").ifPresent(ear -> {
				RandomSource rand = animatable.getRandom();
				Vector3d earPos = ear.getWorldPosition();

				animatable.getCommandSenderWorld().addParticle(ParticleTypes.PORTAL,
						earPos.x(),
						earPos.y(),
						earPos.z(),
						rand.nextDouble() - 0.5D, // 随机偏移量
						-rand.nextDouble(),
						rand.nextDouble() - 0.5D);
			});
		}

		super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, colour);
	}
}