package com.sakpeipei.undertale.mixin.gravity;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(PathNavigation.class)
public class PathNavigationGravityMixin {
    private static final Logger log = LoggerFactory.getLogger(PathNavigationGravityMixin.class);
    @Shadow
    @Final
    protected Mob mob;

    @Shadow @Nullable protected Path path;

//    @Inject(method = "moveTo(DDDD)Z", at = @At("HEAD"))
//    private void onMoveTo(double x, double y, double z, double speed, CallbackInfoReturnable<Boolean> cir) {
//        if (mob instanceof IronGolem) {
//            log.info("moveTo called: ({}, {}, {}) speed={}", x, y, z, speed);
//        }
//    }
//
//    @Inject(method = "moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z", at = @At("HEAD"))
//    private void onMoveToPath(Path path, double speed, CallbackInfoReturnable<Boolean> cir) {
//        if (mob instanceof IronGolem) {
//            log.info("moveTo with path: {}", path != null ? "exists" : "null");
//        }
//    }
//
//    @Inject(method = "tick", at = @At("HEAD"))
//    private void onTick(CallbackInfo ci) {
//        if (mob instanceof IronGolem) {
//            log.info("PathNavigation tick - path={}, done={}",
//                    this.path != null ? "exists" : "null",
//                    path != null ? path.isDone() : "N/A");
//        }
//    }
//
//    @Inject(method = "createPath*", at = @At("RETURN"))
//    private void onCreatePath(Set<BlockPos> targets, int range, boolean bl, int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
//        if (mob instanceof IronGolem) {
//            Path path = cir.getReturnValue();
//            if (path != null) {
//                Node endNode = path.getEndNode();
//                log.info("✓ IronGolem path created: nodes={}, target={}, endNode=({},{},{})",
//                        path.getNodeCount(), targets, endNode.x, endNode.y, endNode.z);
//            } else {
//                log.info("✗ IronGolem path FAILED - targets: {}", targets);
//            }
//        }
//    }
}
