package com.sakpeipei.undertale.item;

import com.sakpeipei.undertale.client.render.item.GasterBlasterItemRender;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.registry.ItemTypes;
import com.sakpeipei.undertale.utils.ProjectileUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GasterBlasterItem extends Item implements GeoItem {
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterItem.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int CD_TICK = 20; // 1秒

    public GasterBlasterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000; // 足够长的时间，实现无限按住
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW; // 显示拉弓动画
    }
    /**
     * 物品使用交互逻辑（右键触发动画）
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 检查冷却
        if (player.getCooldowns().isOnCooldown(ItemTypes.GASTER_BLASTER.get())) {
            player.displayClientMessage(Component.literal("§c冷却中！"), true);
            return InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide()) {
            if(player.isShiftKeyDown()) {
                if (player.isShiftKeyDown()) {
                    if (!level.isClientSide()) {
                        Vec3 relativePos = new Vec3(0, player.getEyeHeight(), 2f); // 玩家前方2格
                        GasterBlaster blaster = new GasterBlaster(level, player).follow(relativePos);
                        level.addFreshEntity(blaster);
                    }
                    player.startUsingItem(hand); // 进入持续使用状态
                    return InteractionResultHolder.consume(itemStack);
                }
            }else{
                HitResult hitResult = ProjectileUtils.getHitResultOnViewVector(player, Entity::isPickable, GasterBlaster.DEFAULT_LENGTH);
                GasterBlaster blaster = new GasterBlaster(level, player);
                double safeDistance = player.getBbWidth() + blaster.getBbWidth() * 1.5;
                blaster.setPos(player.position().add(RotUtils.getWorldPos(new Vec3(0,safeDistance,0),player.getRandom().nextFloat() * 180f - 90f, player.getXRot(), player.getYRot())));
                if(hitResult instanceof EntityHitResult entityHitResult) {
                    Entity target = entityHitResult.getEntity();
                    blaster.aim(new Vec3(target.getX(),target.getY(0.5f),target.getZ()));
                }else{
                    blaster.aim(hitResult.getLocation());
                }
                level.addFreshEntity(blaster);
                player.getCooldowns().addCooldown(ItemTypes.GASTER_BLASTER.get(), CD_TICK);
                return InteractionResultHolder.success(itemStack);
            }
        }
        return InteractionResultHolder.consume(itemStack);
    }




    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GasterBlasterItemRender render;
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (render == null) {
                    render = new GasterBlasterItemRender();
                }
                return render;
            }
        });
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
