package com.fanxing.fx_undertale.common.damagesource;

import com.fanxing.fx_undertale.entity.capability.CustomDeathMessage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.IDeathMessageProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomNounVerbDeathMessageProvider implements IDeathMessageProvider {
    @Override
    public @NotNull Component getDeathMessage(@NotNull LivingEntity victim, CombatEntry lastEntry, @Nullable CombatEntry fallEntry) {
        DamageSource source = lastEntry.source();
        Entity attacker = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        String damageTypeId = source.type().msgId();
        String baseKey = "death.attack." + damageTypeId;

        if (attacker != null) {
            String noun = null;
            String verb = null;

            // 1. 攻击者自定义
            if (attacker instanceof CustomDeathMessage custom) {
                noun = custom.getCustomNoun(directEntity);
                verb = custom.getCustomVerb(directEntity);
            }
            // 2. 没有自定义则用默认
            if (noun == null && directEntity != null) {
                noun = EntityType.getKey(directEntity.getType()).getPath();
            }
            // 名词必须存在
            if (noun != null) {
                // 尝试：名词 + 动词
                if (verb != null) {
                    String messageKey = baseKey + "." + noun + "." + verb + ".player";
                    if (Language.getInstance().has(messageKey)) {
                        return Component.translatable(messageKey,
                                victim.getDisplayName(),
                                attacker.getDisplayName()
                        );
                    }
                }
                // 降级：只有名词
                String nounOnlyKey = baseKey + "." + noun + ".player";
                if (Language.getInstance().has(nounOnlyKey)) {
                    return Component.translatable(nounOnlyKey,
                            victim.getDisplayName(),
                            attacker.getDisplayName()
                    );
                }
            }
            // 4. 降级：通用消息
            return Component.translatable(baseKey + ".player",
                    victim.getDisplayName(),
                    attacker.getDisplayName()
            );
        }
        return Component.translatable(baseKey, victim.getDisplayName());
    }
}
