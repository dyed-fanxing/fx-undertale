package com.fanxing.fx_undertale.common;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;

public interface RenderStateShards {
    RenderStateShard.TransparencyStateShard PREMULTIPLIED_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "premultiplied_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );
}
