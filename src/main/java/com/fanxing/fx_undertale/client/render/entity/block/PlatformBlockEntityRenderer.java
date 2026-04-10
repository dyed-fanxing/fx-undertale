package com.fanxing.fx_undertale.client.render.entity.block;

import com.fanxing.fx_undertale.entity.block.PlatformBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class PlatformBlockEntityRenderer extends EntityRenderer<PlatformBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public PlatformBlockEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PlatformBlockEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(PlatformBlockEntity entity, float yaw, float partialTick,@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        BlockState blockstate = entity.getBlockState();
        if (blockstate.isAir()) return;
        if (blockstate.getRenderShape() == RenderShape.MODEL) {
            Level level = entity.level();
            if (blockstate != level.getBlockState(entity.blockPosition()) && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                poseStack.pushPose();
                // 实体坐标原点在底部中心，直接渲染（方块模型默认原点也在底部）
                float progress = entity.getProgress(partialTick);
                poseStack.scale(progress*entity.getWidthScale(),entity.getHeightScale(),progress* entity.getWidthScale());
                poseStack.translate(-0.5F, 0.0F,-0.5F);
                BakedModel model = this.blockRenderer.getBlockModel(blockstate);
                BlockPos blockpos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
                BlockPos anchorPos = entity.blockPosition();
                for(RenderType renderType : model.getRenderTypes(blockstate, RandomSource.create(blockstate.getSeed(anchorPos)), ModelData.EMPTY)) {
                    this.blockRenderer.getModelRenderer().tesselateBlock(entity.level(),model, blockstate, blockpos,
                            poseStack, buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                            false, RandomSource.create(),
                            blockstate.getSeed(anchorPos),
                            OverlayTexture.NO_OVERLAY, ModelData.EMPTY,
                            renderType
                    );
                }
                poseStack.popPose();

                super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
            }
        }
    }
}