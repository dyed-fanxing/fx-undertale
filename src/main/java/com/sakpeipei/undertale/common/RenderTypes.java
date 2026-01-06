package com.sakpeipei.undertale.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;

/**
 * @author yujinbao
 * @since 2026/1/6 13:50
 */
public interface RenderTypes {
    /**
     * 与原版beam的区别为 NO_CULL，因为这个GB炮是跟着实体渲染的，而原版的信标光束是跟着方块渲染的，顺序不一样
     * 如果使用原版的beam，会导致光束不会覆盖穿过的实体，而是会在光束中看到实体，且光束的两端会被GB炮覆盖导致透明
     */
    Function<ResourceLocation, RenderType> GB_BEAM = Util.memoize(
            texture -> RenderType.create("gb_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, false,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false))
    );
}
