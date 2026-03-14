package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.entity.projectile.RotationBone;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mixin(Entity.class)
public abstract class EntityTestMixin {
    @Shadow public double zo;
    @Shadow public abstract double getZ();

//    @Inject(method = "setOldPosAndRot", at = @At("HEAD"))
//    private void onSetOldPosAndRot(CallbackInfo ci) {
//        Entity entity = (Entity)(Object)this;
//        if (entity instanceof RotationBone){
//            LOGGER.info("setOldPosAndRot called, zo -> z：{} -> {}",zo, this.getZ());
//            fx_undertale$dumpStack();
//        }
//    }
//
//    @Inject(method = "absMoveTo(DDD)V", at = @At("HEAD"))
//    private void onAbsMoveTo(double x, double y, double z, CallbackInfo ci) {
//        Entity entity = (Entity)(Object)this;
//        if (entity instanceof RotationBone){
//            LOGGER.info("absMoveTo called, z: {}", z);
//            fx_undertale$dumpStack();
//        }
//    }
//
//    @Inject(method = "moveTo(DDDFF)V", at = @At("HEAD"))
//    private void onMoveTo(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
//        Entity entity = (Entity)(Object)this;
//        if (entity instanceof RotationBone){
//            LOGGER.info("moveTo called,z：{}",z);
//            // 这里不打印xo，因为还没设置，但我们可以打印堆栈
//            fx_undertale$dumpStack();
//        }
//    }
//    @Unique
//    private void fx_undertale$dumpStack() {
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//        for (int i = 2; i < Math.min(stackTrace.length, 10); i++) { // 跳过本方法
//            LOGGER.info("    at {}", stackTrace[i]);
//        }
//    }
}
