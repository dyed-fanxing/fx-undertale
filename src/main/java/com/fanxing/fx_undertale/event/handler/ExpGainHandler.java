package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.attachment.PlayerSoul;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class ExpGainHandler {

    // 记录命名时间
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack itemStack = player.getItemInHand(event.getHand());
        if (itemStack.getItem() == Items.NAME_TAG && event.getTarget() instanceof LivingEntity target) {
            // 有命名牌，且目标是活物
            target.getPersistentData().putLong("NameTime", event.getLevel().getGameTime());
        }
    }

    // 记录驯服时间
    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event) {
        var entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            entity.getPersistentData().putLong("TameTime", entity.level().getGameTime());
        }
    }



    // 击杀事件：常规EXP + 特殊奖励
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            var level = player.level();
            if (level.isClientSide) return;

            LivingEntity entity = event.getEntity();
            PlayerSoul playerSoul = player.getData(AttachmentTypes.PLAYER_SOUL.get());

            // 常规EXP（按实体类型）
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String entityId = id.toString();
//            var configMap = ModConfig.COMMON.getEntityExp();
//            var cfg = configMap.get(entityId);
//            if (cfg != null) {
//                int currentKills = soulData.getKillCount(entityId);
//                if (cfg.limit < 0 || currentKills < cfg.limit) {
//                    soulData.addExp(cfg.exp, ModConfig.COMMON.expPerLevel.get());
//                    soulData.incrementKillCount(entityId);
//                }
//            }
//
//            // 特殊奖励：命名生物且存活足够天数
//            long now = level.getGameTime();
//            int bonus = 0;
//            var data = killed.getPersistentData();
//
//            if (data.contains("NameTime")) {
//                long nameTime = data.getLong("NameTime");
//                if (now - nameTime >= ModConfig.COMMON.namedKillDays.get() * 24000L) {
//                    bonus += ModConfig.COMMON.namedKillExp.get();
//                }
//            }
//
//            // 驯服生物且存活足够天数
//            if (data.contains("TameTime")) {
//                long tameTime = data.getLong("TameTime");
//                if (now - tameTime >= ModConfig.COMMON.tamedKillDays.get() * 24000L) {
//                    bonus += ModConfig.COMMON.tamedKillExp.get();
//                }
//            }
//
//            if (bonus > 0) {
//                soulData.addExp(bonus, ModConfig.COMMON.expPerLevel.get());
//            }
        }
    }
}
