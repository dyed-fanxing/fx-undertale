package com.sakpeipei.undertale.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.*;

/**
 * @author Sakpeipei
 * @since 2026/1/6 13:50
 */
public interface RenderTypes {
    /**
     * 与原版beam的区别为 NO_CULL，因为这个GB炮是跟着实体渲染的，而原版的信标光束是跟着方块渲染的，顺序不一样
     * 如果使用原版的beam，会导致光束不会覆盖穿过的实体，而是会在光束中看到实体，且光束的两端会被GB炮覆盖导致透明
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_BEAM_NO_CULL = Util.memoize((texture, translucent) -> RenderType.create(
            "entity_beam", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE) // 透明只写颜色，不透明写颜色+深度
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));
    RenderType ENTITY_TRANSLUCENT_EMISSIVE_WHITE = RenderType.ENTITY_TRANSLUCENT_EMISSIVE.apply(ResourceLocations.WHITE_TEXTURE, true);
    RenderType ENTITY_NO_TRANSLUCENT_EMISSIVE_WHITE = RenderType.ENTITY_TRANSLUCENT_EMISSIVE.apply(ResourceLocations.WHITE_TEXTURE, false);

    static RenderType energySwirlTriangle(ResourceLocation resourceLocation, float offsetU, float offsetV) {
        return RenderType.create("energy_swirl_triangle", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, false))
                        .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(offsetU, offsetV))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false)
        );
    }
    Function<ResourceLocation, RenderType> ENERGY_TRIANGLE = Util.memoize((texture) -> RenderType.create("energy_triangle", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    // ========== ENERGY 能量渲染效果 ==========

    /**
     * ENERGY_TRIANGLE_FAN
     * 能量效果 - 扇形模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLE_FAN = Util.memoize((texture, translucent) -> RenderType.create(
            "energy_triangle_fan", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    /**
     * ENERGY_TRIANGLES
     * 能量效果 - 三角形模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLES = Util.memoize((texture, translucent) -> RenderType.create(
            "energy_triangles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    /**
     * ENERGY_TRIANGLE_STRIP
     * 能量效果 - 条带模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLE_STRIP = Util.memoize((texture, translucent) -> RenderType.create(
            "energy_triangle_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    // ========== ENERGY_SWIRL 能量漩涡渲染效果 ==========

    /**
     * ENERGY_SWIRL_TRIANGLE_FAN
     * 能量漩涡效果 - 扇形模式
     */
    Function<ResourceLocation, RenderType> ENERGY_SWIRL_TRIANGLE_FAN = Util.memoize((texture) -> RenderType.create("energy_swirl_triangle_fan", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    /**
     * ENERGY_SWIRL_TRIANGLE_STRIP
     * 能量漩涡效果 - 条带模式
     */
    Function<ResourceLocation, RenderType> ENERGY_SWIRL_TRIANGLE_STRIP = Util.memoize((texture) -> RenderType.create("energy_swirl_triangle_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    // ========== ENTITY_TRANSLUCENT_EMISSIVE 渲染类型 ==========

    /**
     * 适合渲染：圆形、圆柱顶底面、胶囊体半球等扇形结构
     * 顶点数最少：segments + 2 个顶点
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN = Util.memoize((texture, translucent) -> RenderType.create(
            "triangle_fan", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    /**
     * 适合渲染：球体（纬度-经度方式）、任意三角形网格等
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLES = Util.memoize((texture, translucent) -> RenderType.create(
            "triangles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));

    /**
     * 适合渲染：圆柱侧面、胶囊体圆柱部分侧面等条带结构
     * 顶点数最少：segments * 2 个顶点
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP = Util.memoize((texture, translucent) -> RenderType.create(
            "triangle_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));
}
