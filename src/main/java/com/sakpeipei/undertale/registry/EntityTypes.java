package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.sans.Sans;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.entity.summon.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Undertale.MOD_ID);
    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    public static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> builder){
        return ENTITY_TYPES.register(name,() -> builder.build(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,name).toString()));
    }


    public static final DeferredHolder<EntityType<?>,EntityType<GasterBlaster>> GASTER_BLASTER =
            ENTITY_TYPES.register("gaster_blaster",
                    () -> EntityType.Builder.<GasterBlaster>of(GasterBlaster::new, MobCategory.MISC)
                            .sized(1.5f, 1.5f)  // 碰撞箱
                            .eyeHeight(0.4f)
                            .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
                            .build(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"gaster_blaster").toString())
            );
    public static final DeferredHolder<EntityType<?>,EntityType<GasterBlasterLiving>> GASTER_BLASTER_LIVING =
            ENTITY_TYPES.register("gaster_blaster_living",
                    () -> EntityType.Builder.<GasterBlasterLiving>of(GasterBlasterLiving::new, MobCategory.MISC)
                            .sized(1.5f, 1.5f)  // 碰撞箱
                            .eyeHeight(0.4f)
                            .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
                            .build(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"gaster_blaster").toString())
            );


    public static final DeferredHolder<EntityType<?>, EntityType<Sans>> SANS = register("sans",
            EntityType.Builder.of(Sans::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.0f)  // 碰撞箱
                    .eyeHeight(1.6665f)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位)
    );

    public static final DeferredHolder<EntityType<?>, EntityType<GroundBone>> GROUND_BONE = register("ground_bone",
            EntityType.Builder.<GroundBone>of(GroundBone::new, MobCategory.MISC)
                    .sized(0.25f, 1.0f)  // 碰撞箱
                    .eyeHeight(0.5445f)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MovingGroundBone>> MOVING_GROUND_BONE = register("moving_ground_bone",
            EntityType.Builder.<MovingGroundBone>of(MovingGroundBone::new, MobCategory.MISC)
                    .sized(0.25f, 1.0f)  // 碰撞箱
                    .eyeHeight(0.5445f)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
    public static final DeferredHolder<EntityType<?>, EntityType<LateralBone>> LATERAL_BONE = register("lateral_bone",
            EntityType.Builder.<LateralBone>of(LateralBone::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)  // 碰撞箱
                    .eyeHeight(0.125f)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingBone>> FLYING_BONE = register("flying_bone",
            EntityType.Builder.<FlyingBone>of(FlyingBone::new, MobCategory.MISC)
                    .sized(0.3125f,0.3125f)
                    .eyeHeight(0.25f)
                    .clientTrackingRange(4)  // 客户端同步范围，以区块为单位
    );
}
