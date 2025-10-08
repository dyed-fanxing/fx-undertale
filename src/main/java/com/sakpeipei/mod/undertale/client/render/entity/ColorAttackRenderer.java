package com.sakpeipei.mod.undertale.client.render.entity;

import com.sakpeipei.mod.undertale.entity.AttackColored;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.ClientUtil;
import software.bernie.geckolib.util.Color;

/**
 * 具有颜色攻击的渲染器
 * @author Sakqiongzi
 * @since 2025-10-07 21:59
 */
public abstract class ColorAttackRenderer<T extends Entity & GeoAnimatable & AttackColored> extends GeoEntityRenderer<T> {
    public ColorAttackRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        Color color = animatable.getColor();
        if (animatable.isInvisible() && !animatable.isInvisibleTo(ClientUtil.getClientPlayer()))
            color = Color.ofARGB(Mth.ceil(color.getAlpha() * 38 / 255f), color.getRed(), color.getGreen(), color.getBlue());

        return color;
    }

}
