package com.sakpeipei.undertale.mixin.gravity;

import com.ibm.icu.impl.Pair;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
        GravityData data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vec3 eyePos = player.getEyePosition(1.0F);
        double width = player.getBbWidth() * 0.8F;
        double scale = player.getScale() * 0.1F;
        for (int i = 0; i < 8; ++i) {
            double offset1 = ((float)((i) % 2) - 0.5F) * width;
            double offset2 = ((float)((i >> 1) % 2) - 0.5F) * scale;
            double offset3 = ((float)((i >> 2) % 2) - 0.5F) * width;
            switch (data.getGravity()) {
                // 高度轴是 Y 负方向
                case UP -> pos.set(eyePos.x + offset1,eyePos.y - offset2,eyePos.z + offset3);
                // 高度轴是 Z，水平面是 XY
                case SOUTH -> pos.set( eyePos.x + offset1,eyePos.y + offset3,eyePos.z + offset2);
                case NORTH -> pos.set( eyePos.x + offset1,eyePos.y - offset3,eyePos.z - offset2);
                // 高度轴是 X，水平面是 YZ
                case EAST -> pos.set(eyePos.x + offset2,eyePos.y + offset1,eyePos.z + offset3);
                case WEST -> pos.set(eyePos.x - offset2,eyePos.y + offset1,eyePos.z - offset3);
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
