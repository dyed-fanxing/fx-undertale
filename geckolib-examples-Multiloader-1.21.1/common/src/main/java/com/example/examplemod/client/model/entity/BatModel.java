package com.example.examplemod.client.model.entity;

import com.example.examplemod.ExampleModCommon;
import com.example.examplemod.entity.BatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;

/**
 * Example {@link GeoModel} for the {@link BatEntity}
 * @see com.example.examplemod.client.renderer.entity.BatRenderer BatRenderer
 */
public class BatModel extends DefaultedEntityGeoModel<BatEntity> {
	// 我们在这里使用备用超级构造函数来告诉模型它应该为我们处理回头率
	public BatModel() {
		super(ResourceLocation.fromNamespaceAndPath(ExampleModCommon.MODID, "bat"), true);
	}
}
