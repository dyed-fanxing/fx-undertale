package com.sakpeipei.undertale.item;

import com.sakpeipei.undertale.client.render.item.GasterBlasterItemRender;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.registry.ItemRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GasterBlasterItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int CD_TICK = 20; // 1秒

    public GasterBlasterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }


    /**
     * 物品使用交互逻辑（右键触发动画）
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 检查冷却
        if (player.getCooldowns().isOnCooldown(ItemRegistry.GASTER_BLASTER.get())) {
            player.displayClientMessage(Component.literal("§c冷却中！"), true);
            System.out.println("冷却中!!!");
            return InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide()) {
            // 1. 射线检测，获取终点射击位置
            HitResult hitResult = player.pick(GasterBlaster.DEFAULT_LENGTH, 1.0f, false);
            Vec3 targetPos = hitResult.getLocation();;
            // 2. 创建炮台实体
            GasterBlaster blaster = new GasterBlaster(EntityTypeRegistry.GASTER_BLASTER.get(), level, player,1.0f,(short) 2000);
            // 2. 计算炮台生成位置（圆形分布） //向上安全距离的向量
            double safeDistance = player.getBbWidth() + blaster.getBbWidth() * 1.5;
            blaster.setPos(player.position().add(new Vec3(0,safeDistance,0)
                // 生成扇形，不包含下方180度扇形区域， -90 对齐 MC坐标系
                .zRot((( player.getRandom().nextFloat() * 180 ) - 90) * Mth.DEG_TO_RAD)
                .xRot(-player.getXRot() * Mth.DEG_TO_RAD)
                .yRot(-player.getYRot() * Mth.DEG_TO_RAD)
            ));
////             4. 设置旋转
//            Vec3 direction = targetPos.subtract(blaster.position()).normalize();
//            blaster.setYRot(RotUtils.yRot(direction.x, direction.z));
//            blaster.setXRot(RotUtils.xRot(direction.y));
            // 使用原版方法，从FEET锚点（底层就是this.position的位置），看向目标位置
            blaster.lookAt(EntityAnchorArgument.Anchor.FEET,targetPos);
            // 6. 生成炮台
            level.addFreshEntity(blaster);

            player.getCooldowns().addCooldown(ItemRegistry.GASTER_BLASTER.get(), CD_TICK);
            return InteractionResultHolder.success(itemStack);
        }
        return InteractionResultHolder.consume(itemStack);
    }

//    @Override
//    public int getMaxStackSize(@NotNull ItemStack stack) {
//        return 1;
//    }

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
}
