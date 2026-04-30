package com.fanxing.fx_undertale.mixin;

import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilderDebug {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
//        System.out.println("BufferBuilder created: mode=" + mode + ", format=" + format + ", hash=" + System.identityHashCode(this));
    }

    @Inject(method = "beginElement", at = @At("HEAD"))
    private void onBegin(VertexFormatElement element, CallbackInfoReturnable<Long> cir) {
//        System.out.println("BufferBuilder.begin called: " + this + " mode=" + element);
//        new Exception("begin stack").printStackTrace(System.out);
    }
    @Inject(method = "build", at = @At("HEAD"))
    private void onBuild(CallbackInfoReturnable<MeshData> cir) {
//        System.out.println("BufferBuilder.build() called on " + this);
    }
}