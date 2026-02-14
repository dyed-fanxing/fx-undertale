package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.registry.AttachmentTypes;
import com.sakpeipei.undertale.utils.GravityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
//public abstract class GroundPathNavigationMixin extends PathNavigation {
public abstract class GroundPathNavigationMixin{
    private static final Logger log = LoggerFactory.getLogger(GroundPathNavigationMixin.class);

//    public GroundPathNavigationMixin(Mob p_26515_, Level p_26516_) {
//        super(p_26515_, p_26516_);
//    }

//    @Redirect(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
//    private BlockPos below(BlockPos instance) {
//        Direction gravity = mob.getData(AttachmentTypes.GRAVITY).getGravity();
//        if (gravity == Direction.DOWN) return instance.below();
//        return instance.relative(gravity);
//    }
//
//    @Redirect(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"))
//    private BlockPos above(BlockPos instance) {
//        Direction gravity = mob.getData(AttachmentTypes.GRAVITY).getGravity();
//        if (gravity == Direction.DOWN) return instance.above();
//        return instance.relative(gravity.getOpposite());
//    }
//
//
//    /**
//     * 获取临时位置并将getSurfaceY整合进一起
//     */
//    @Inject(method = "getTempMobPos", at = @At("HEAD"), cancellable = true)
//    private void onGetTempMobPos(CallbackInfoReturnable<Vec3> cir) {
//        Direction gravity = mob.getData(AttachmentTypes.GRAVITY).getGravity();
//        if (gravity == Direction.DOWN) return;
//        cir.cancel();
//        Direction opposite = gravity.getOpposite();
//        Vec3i up = opposite.getNormal();
//        // 从实体位置开始，沿着up方向找非空气方块
//        BlockPos blockPos = this.mob.blockPosition();
//        BlockPos.MutableBlockPos pos = blockPos.mutable();
//        if (this.mob.isInWater() && this.canFloat()) {
//            int count = 0;
//            while(this.level.getBlockState(pos).is(Blocks.WATER)) {
//                pos.move(opposite);
//                if (++count > 16) {
//                    cir.setReturnValue(GravityUtils.getEntitySurfaceBlockPos(mob,blockPos,up));
//                    return;
//                }
//            }
//            cir.setReturnValue(GravityUtils.getEntitySurfaceBlockPos(mob,pos,up));
//        } else {
//            cir.setReturnValue(new Vec3(pos.getX() + up.getX()*0.5,pos.getY()+0.5 * up.getY(), pos.getZ()+0.5 * up.getZ()));
//        }
//    }
}
