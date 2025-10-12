package com.sakpeipei.mod.undertale.item;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.render.item.GasterBlasterProItemRender;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.mod.undertale.network.GasterBlasterProPacket;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.registry.ItemRegistry;
import com.sakpeipei.mod.undertale.utils.RayUtils;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.function.Consumer;

public class GasterBlasterProItem extends GasterBlasterFixedItem {


    public static final String GASTER_BLASTER_PRO_KEY = Undertale.MODID + ":gaster_blaster_pro";
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterProItem.class);

    public GasterBlasterProItem(Properties properties) {
        super(properties);
    }


    /**
     * 物品使用交互逻辑（右键触发动画）
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 1. 检查冷却
        if (player.getCooldowns().isOnCooldown(ItemRegistry.GASTER_BLASTER.get())) {
            player.displayClientMessage(Component.literal("§c冷却中！"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        if(level instanceof ServerLevel serverLevel){
            CompoundTag persistentData = player.getPersistentData();
            log.info("玩家的自定义NBT数据{}",persistentData);
            // 2. 获取炮台实体
            boolean exist = persistentData.hasUUID(GASTER_BLASTER_PRO_KEY);
            GasterBlasterPro blaster = null;
            if(exist) blaster = (GasterBlasterPro) serverLevel.getEntity(persistentData.getUUID(GASTER_BLASTER_PRO_KEY));
            if(blaster == null){
                exist = false;
                blaster = new GasterBlasterPro(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), serverLevel,player);
            }
            if(player.isShiftKeyDown()){ // shift模式，精确射击，没有目标实体不执行
                GasterBlasterPro finalBlaster = blaster;
                EntityHitResult hitResult = RayUtils.getLastestEntityHitResultOnViewVector(level,player, entity -> entity.isAlive() && entity != finalBlaster, GasterBlasterPro.DEFAULT_LENGTH * 2);
                if(hitResult != null && hitResult.getEntity() instanceof LivingEntity entity) blaster.setTarget(entity);
                else {
                    //没有瞄准目标，也就是shift+右键 空气
                    // 蓄力阶段，手动终止，不进入冷却
                    byte phase = blaster.getPhase();
                    if(phase == GasterBlasterPro.PHASE_CHARGE) {
                        blaster.clearTarget();
                        blaster.stop();
                        PacketDistributor.sendToPlayersTrackingEntity(blaster,new GasterBlasterProPacket((short) 0,blaster.getId()));
                    }
                    // 射击阶段，进入冷却
                    else if(phase == GasterBlasterPro.PHASE_GROW || phase == GasterBlasterPro.PHASE_SHOT) {
                        blaster.clearTarget();
                        blaster.cooldown();
                        PacketDistributor.sendToPlayersTrackingEntity(blaster,new GasterBlasterProPacket((short) 0,blaster.getId()));
                    }
                    return InteractionResultHolder.pass(itemStack);
                }
            }else {
                HitResult hitResult = player.pick(GasterBlasterPro.DEFAULT_LENGTH, 1.0f, false);
                blaster.lookAt(EntityAnchorArgument.Anchor.FEET,hitResult.getLocation());
                blaster.fire();
            }
            if(!exist) {
                blaster.setPos(player.position().add(0, blaster.getWidth(), 0));
                level.addFreshEntity(blaster);
                persistentData.putUUID(GASTER_BLASTER_PRO_KEY, blaster.getUUID());
            }
            return InteractionResultHolder.success(itemStack);
        }
        return InteractionResultHolder.pass(itemStack);
    }


    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        // 只在服务端执行
        if(level instanceof ServerLevel serverLevel){
            // 检查冷却
            if (player.getCooldowns().isOnCooldown(ItemRegistry.GASTER_BLASTER.get())) {
                player.displayClientMessage(Component.literal("§c物品冷却中，请等待！"), true);
                return InteractionResult.FAIL;
            }
            BlockPos blockPos = context.getClickedPos().relative(context.getClickedFace());
            GasterBlasterPro blaster;
            CompoundTag persistentData = player.getPersistentData();
            if(persistentData.hasUUID(GASTER_BLASTER_PRO_KEY)){
                blaster = (GasterBlasterPro) serverLevel.getEntity(persistentData.getUUID(GASTER_BLASTER_PRO_KEY));
                blaster.moveTo(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            }
            else{
                blaster = new GasterBlasterPro(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), level, player);
                persistentData.putUUID(GASTER_BLASTER_PRO_KEY, blaster.getUUID());
                blaster.setPos(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                level.addFreshEntity(blaster);
            }

            LogUtils.getLogger().info("右键地面，放置Pro {} , 主人{}",player.getPersistentData(),blaster.getOwner());

        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GasterBlasterProItemRender render;
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (render == null) {
                    render = new GasterBlasterProItemRender();
                }
                return render;
            }
        });
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
