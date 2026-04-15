package com.fanxing.fx_undertale.item;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.PlayerAnimations;
import com.fanxing.fx_undertale.client.render.item.GasterBlasterItemRender;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.registry.ItemTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.fanxing.fx_undertale.utils.collsion.AABBCCDUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
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
    private static final ResourceLocation WIND_UP = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "attack.cast.gb.windup");
    private static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "attack.cast.gb");
    //    private static final ResourceLocation ALL =  ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "attack.cast.gb.all");
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterItem.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int CD_TICK = 20; // 1秒
    private static final int MAX_USE_DURATION = 200;

    public GasterBlasterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return MAX_USE_DURATION;
    }


    /**
     * 物品使用交互逻辑（右键触发动画）
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 检查冷却
        if (player.getCooldowns().isOnCooldown(ItemTypes.GASTER_BLASTER.get())) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                Vec3 relativePos = new Vec3(0, player.getEyeHeight(), 2f); // 玩家前方2格
                GasterBlaster blaster = new GasterBlaster(level, player,1f,1f,getUseDuration(itemStack,player)).follow(relativePos).color(Sans.ENERGY_AQUA);
                RotUtils.lookVec(blaster, player.getViewVector(1.0f));
                level.addFreshEntity(blaster);
                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag() {{
                    putInt("entityId", blaster.getId());
                }}));
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(itemStack);
            } else {
                HitResult hitResult = AABBCCDUtils.getHitResultOnViewVector(player, entity -> entity.isPickable() && entity != player.getVehicle() && !(entity instanceof TraceableEntity traceable && traceable.getOwner() != player), GasterBlaster.DEFAULT_LENGTH);
                GasterBlaster blaster = new GasterBlaster(level, player).color(Sans.ENERGY_AQUA);
                double safeDistance = player.getBbWidth() + blaster.getBbWidth() * 1.5;
                blaster.setPos(player.position().add(GravityUtils.localToWorld(player,RotUtils.rotateYXZ(new Vec3(0, safeDistance, 0.3f), player.getYRot(), player.getXRot(), player.getRandom().nextFloat() * 180f -90f))));
                blaster.aim(hitResult.getLocation());
                if (hitResult instanceof EntityHitResult entityHitResult) {
                    Entity target = entityHitResult.getEntity();
                    blaster.target(target).aimSmoothSpeed(0.15f);
                }
                blaster.restAnimPos();
                level.addFreshEntity(blaster);
                player.getCooldowns().addCooldown(ItemTypes.GASTER_BLASTER.get(), CD_TICK);
                return InteractionResultHolder.success(itemStack);
            }
        } else {
            if (player.isShiftKeyDown()) {
                if (player instanceof AbstractClientPlayer clientPlayer) {
                    PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(clientPlayer, PlayerAnimations.ATTACK);
                    if (controller != null) {
                        controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                        controller.setFirstPersonConfiguration(
                                new FirstPersonConfiguration(
                                        true,  // 显示右臂
                                        true,  // 显示左臂
                                        true,  // 显示右手物品
                                        true   // 显示左手物品
                                )
                        );
                        controller.triggerAnimation(RawAnimation.begin()
                                .thenPlay(PlayerAnimResources.getAnimation(WIND_UP))
                                .thenLoop(PlayerAnimResources.getAnimation(ATTACK))
                        );
                    }
                }
                return InteractionResultHolder.consume(itemStack);
            }
        }
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if(level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack itemStack = context.getItemInHand();
        // 获取放置位置
        BlockPos clickedPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos placePos = clickedPos.relative(direction);
        Vec3 pos = new Vec3(placePos.getX() + 0.5f, placePos.getY(), placePos.getZ() + 0.5f);
        // 从数据组件中读取
        GasterBlaster gb = new GasterBlaster(level, player,100).mountable();
        gb.setPos(pos);
        gb.setYRot(player.getYRot());
        // 检查位置是否合适
        if (!level.noCollision(gb.getBoundingBox())) {
            return InteractionResult.PASS;
        }
        level.addFreshEntity(gb);
        itemStack.consume(1,player);
        return InteractionResult.SUCCESS;
    }


    /**
     * count 是剩余还可以使用的Tick数
     */
    @Override
    public void onStopUsing(@NotNull ItemStack stack, @NotNull LivingEntity entity, int count) {
        Level level = entity.level();
        if(entity instanceof Player player){
            if (player instanceof AbstractClientPlayer clientPlayer) {
                PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(clientPlayer, PlayerAnimations.ATTACK);
                if (controller != null) {
                    controller.stopTriggeredAnimation();
                }
            }
            // 从数据组件中读取
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("entityId")) {
                    if (level.getEntity(tag.getInt("entityId")) instanceof GasterBlaster blaster) {
                        if (blaster.isFire()) {
                            blaster.startDecay();
                        } else {
                            blaster.discard();
                        }
                    }
                }
            }
            player.getCooldowns().addCooldown(ItemTypes.GASTER_BLASTER.get(), (getUseDuration(stack,entity) - count)/2);
        }
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
