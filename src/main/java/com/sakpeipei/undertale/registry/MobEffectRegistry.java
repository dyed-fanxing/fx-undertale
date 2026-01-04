package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.mobEffect.KarmaMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author yujinbao
 * @since 2025/9/9 13:50
 */
public class MobEffectRegistry {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Undertale.MODID);
    public static void registry(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }

    public static DeferredHolder<MobEffect,MobEffect> KARMA = MOB_EFFECTS.register("karma",() -> new KarmaMobEffect(MobEffectCategory.HARMFUL,9154528));
}
