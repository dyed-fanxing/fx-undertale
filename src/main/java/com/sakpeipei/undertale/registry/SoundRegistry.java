package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Undertale.MODID);
    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    public static final DeferredHolder<SoundEvent,SoundEvent> ENEMY_ENCOUNTER_ATTACK_TIP = register("entity.enemy_encounter_attack_tip");

    public static final DeferredHolder<SoundEvent,SoundEvent> GASTER_BLASTER_WHOLE = register("entity.gaster_blaster.whole");
    public static final DeferredHolder<SoundEvent,SoundEvent> GASTER_BLASTER_CHARGE = register("entity.gaster_blaster.charge");
    public static final DeferredHolder<SoundEvent,SoundEvent> GASTER_BLASTER_SHOT = register("entity.gaster_blaster.shot");

    public static final DeferredHolder<SoundEvent,SoundEvent> SANS_SLAM = register("entity.sans.slam");
    public static final DeferredHolder<SoundEvent,SoundEvent> SANS_BONE_SPINE = register("entity.sans.bone_spine");
    public static final DeferredHolder<SoundEvent,SoundEvent> SANS_TELEPORT_TIME_JUMP = register("entity.sans.teleport_time_jump");

    public static DeferredHolder<SoundEvent, SoundEvent> register(String name){
        return SOUNDS.register(name,() ->
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,name)));
    }
}
