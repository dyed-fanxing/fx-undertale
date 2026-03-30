package com.fanxing.fx_undertale.item;

import com.fanxing.fx_undertale.client.render.item.MagicBoneItemRender;
import com.fanxing.fx_undertale.common.phys.motion.GravityMotion;    // 根据你的实际包路径调整
import com.fanxing.fx_undertale.common.phys.motion.OscillationMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.ProportionalNavigationModel;
import com.fanxing.fx_undertale.common.phys.motion.SpringMotionModel;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class MagicBone extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public MagicBone(Properties properties) {
        super(properties.stacksTo(1)); // 调试用，设为不可堆叠
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag data = player.getPersistentData();
            String modelType = data.getString("model_type");
            float angularVelocity = data.getFloat("angularVelocity");
            float roll = data.getFloat("roll");
            float pitch = data.getFloat("pitch");
            float yaw = data.getFloat("yaw");
            int lifetime = data.getInt("lifetime");
            Vec3 spawnPos = player.position().add(0, 0, 16);
            float scale = data.getFloat("scale");
            float growScale = data.getFloat("growScale");
            RotationBone bone = new RotationBone(level, null,scale,growScale,lifetime, 1f);
            RotationBone bone1 = new RotationBone(level, null,scale,growScale,lifetime, 1f);
            bone.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            Vec3 toTarget = player.position().subtract(bone.position());
            if ("spring".equals(modelType)) {
                float frequency = data.getFloat("frequency");
                float damping = data.getFloat("damping");
                float speed = data.getFloat("speed");
                SpringMotionModel model = new SpringMotionModel(frequency,damping);
                bone.setXRot(pitch);
                bone.setYRot(yaw);
                bone.motion(model,player.position());

                player.sendSystemMessage(Component.literal("生成弹簧骨骼，参数：" + frequency + ", " + damping + ", " + speed + ", " + angularVelocity));
                bone1.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                bone1.setXRot(pitch);
                bone1.setYRot(yaw);
                bone1.motion(model,player.position());
                level.addFreshEntity(bone1);
            } else {
                // 默认引力模型
                float strength = data.getFloat("strength");
                float softening = data.getFloat("softening");
                float speed = data.getFloat("speed");
                GravityMotion model = new GravityMotion(strength, 8F, (float) toTarget.length(), softening);
                player.sendSystemMessage(Component.literal("生成引力骨骼，参数：" + strength + ", " + softening + ", " + speed));
            }
            if ("proportional".equals(modelType)) {
                float turnRate = data.getFloat("turnRate");
                float speed = data.getFloat("speed");
                float angle = data.getFloat("angle");

                // 计算速度方向（水平面内绕Y轴旋转）
                Vec3 radial = toTarget.normalize();
                Vec3 horizontalRadial = new Vec3(radial.x, 0, radial.z).normalize();
                if (horizontalRadial.lengthSqr() < 1e-6) horizontalRadial = new Vec3(1, 0, 0);
                ProportionalNavigationModel model = new ProportionalNavigationModel(turnRate, speed);
                player.sendSystemMessage(Component.literal("生成比例导引骨骼，参数：turnRate=" + turnRate + ", speed=" + speed + ", angle=" + angle));
            }

            if ("oscillation".equals(modelType)) {
                float speed = data.getFloat("speed");
                float angle = data.getFloat("angle");

                // 计算速度方向（水平面内绕Y轴旋转）
                Vec3 radial = toTarget.normalize();
                Vec3 horizontalRadial = new Vec3(radial.x, 0, radial.z).normalize();
                if (horizontalRadial.lengthSqr() < 1e-6) horizontalRadial = new Vec3(1, 0, 0);
                OscillationMotionModel model = new OscillationMotionModel(speed);
                player.sendSystemMessage(Component.literal("生成振荡骨骼，参数：speed=" + speed + ", angle=" + angle));
            }
            level.addFreshEntity(bone);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // 以下为 GeckoLib 动画相关，保持不变
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 无需动画控制器
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MagicBoneItemRender render;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (render == null) {
                    render = new MagicBoneItemRender();
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