package com.sakpeipei.mod.undertale.data.damagetype;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import static com.sakpeipei.mod.undertale.Undertale.MODID;

public class DamageTypes {

    public static final ResourceKey<DamageType> GASTER_BLASTER_BEAM = create( "gaster_blaster_beam");
    public static final ResourceKey<DamageType> FRAME = create("frame");
    public static final ResourceKey<DamageType> KARMA = create("karma");

    public static ResourceKey<DamageType> create(String name){
        return   ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}
