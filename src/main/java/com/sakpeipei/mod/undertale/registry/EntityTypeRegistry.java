package com.sakpeipei.mod.undertale.registry;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityTypeRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Undertale.MODID);
    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    public static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> builder){
        return ENTITY_TYPES.register(name,() -> builder.build(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,name).toString()));
    }


    public static final DeferredHolder<EntityType<?>,EntityType<GasterBlasterFixed>> GASTER_BLASTER_FIXED =
            ENTITY_TYPES.register("gaster_blaster_fixed",
                    () -> EntityType.Builder.<GasterBlasterFixed>of(GasterBlasterFixed::new, MobCategory.MISC)
                            .sized(1.5f, 1f)  // 碰撞箱
                            .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
                            .build(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster").toString())
            );

    public static final DeferredHolder<EntityType<?>,EntityType<GasterBlasterPro>> GASTER_BLASTER_PRO =
            ENTITY_TYPES.register("gaster_blaster_pro",
                    () -> EntityType.Builder.<GasterBlasterPro>of(GasterBlasterPro::new, MobCategory.MISC)
                            .sized(7.5f, 5f)  // 碰撞箱
                            .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
                            .build(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster").toString())
            );
    public static final DeferredHolder<EntityType<?>, EntityType<Sans>> SANS = register("sans",
            EntityType.Builder.of(Sans::new, MobCategory.MONSTER)
                    .sized(0.5f, 1.8f)  // 碰撞箱
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位)
    );

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingBone>> FLYING_BONE = register("flying_bone",
            EntityType.Builder.<FlyingBone>of(FlyingBone::new, MobCategory.MISC)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
    public static final DeferredHolder<EntityType<?>, EntityType<GroundBone>> GROUND_BONE = register("ground_bone",
            EntityType.Builder.of(GroundBone::new, MobCategory.MISC)
                    .sized(0.1f, 0.5f)  // 碰撞箱
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
}
