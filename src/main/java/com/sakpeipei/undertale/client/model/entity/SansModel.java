package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.Optional;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    private static final Logger log = LoggerFactory.getLogger(SansModel.class);

    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "sans"), true);
    }
}
