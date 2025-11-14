package com.sakpeipei.mod.undertale.registry;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.attachment.GravityData;
import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.attachment.KaramMobEffectData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:07
 */
public class AttachmentTypeRegistry {
    public static DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Undertale.MODID);

    public static final Supplier<AttachmentType<KaramMobEffectData>> KARMA_MOB_EFFECT = ATTACHMENT_TYPES.register(
            "karma_mob_effect", () -> AttachmentType.builder(KaramMobEffectData::new).serialize(KaramMobEffectData.CODEC).build()
    );
    public static final Supplier<AttachmentType<KaramAttackData>> KARMA_ATTACK = ATTACHMENT_TYPES.register(
            "karma_attack", () -> AttachmentType.builder(KaramAttackData::new).serialize(KaramAttackData.CODEC).build()
    );
    public static final Supplier<AttachmentType<GravityData>> GRAVITY = ATTACHMENT_TYPES.register(
            "gravity", () -> AttachmentType.builder(GravityData::new).serialize(GravityData.CODEC).build()
    );
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
