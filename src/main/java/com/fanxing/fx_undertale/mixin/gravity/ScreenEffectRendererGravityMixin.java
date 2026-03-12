package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererGravityMixin {

    /**
     * 被方块阻挡时渲染屏幕贴图
     */
    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true)
    private static void onGetOverlayBlock(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        Gravity data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vec3 eyePos = player.getEyePosition(1.0F);
        double width = player.getBbWidth() * 0.8F;
        double scale = player.getScale() * 0.1F;
        for (int i = 0; i < 8; ++i) {
            double offsetX = ((float)((i) % 2) - 0.5F) * width;
            double offsetY = ((float)((i >> 1) % 2) - 0.5F) * scale;
            double offsetZ = ((float)((i >> 2) % 2) - 0.5F) * width;
            switch (data.getGravity()) {
                // 高度轴是 Y 负方向
                case UP -> pos.set(eyePos.x + offsetX,eyePos.y - offsetY,eyePos.z + offsetZ);
                // 高度轴是 Z，水平面是 XY
                case SOUTH -> pos.set( eyePos.x + offsetX,eyePos.y + offsetZ,eyePos.z + offsetY);
                case NORTH -> pos.set( eyePos.x + offsetX,eyePos.y - offsetZ,eyePos.z - offsetY);
                // 高度轴是 X，水平面是 YZ
                case EAST -> pos.set(eyePos.x + offsetY,eyePos.y + offsetX,eyePos.z + offsetZ);
                case WEST -> pos.set(eyePos.x - offsetY,eyePos.y + offsetX,eyePos.z - offsetZ);
            }
            BlockState blockstate = player.level().getBlockState(pos);
            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(player.level(), pos)) {
                cir.setReturnValue(Pair.of(blockstate, pos.immutable()));
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
