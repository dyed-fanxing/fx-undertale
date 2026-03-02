package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author Sakpeipei
 * @since 2025/9/9 13:50
 */
public class MobEffectTypes {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Undertale.MOD_ID);
    public static void registry(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }

}
