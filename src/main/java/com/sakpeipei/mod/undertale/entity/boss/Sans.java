package com.sakpeipei.mod.undertale.entity.boss;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;


public class Sans extends PathfinderMob implements Enemy, RangedAttackMob, NeutralMob, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(Sans.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("move.walk");
    private final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("move.idle");


    private final static short ATTACK_RANGE = 16; // 攻击距离

    private short misses; // miss次数

    public Sans(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        switch ( level.getDifficulty()){
            case PEACEFUL,EASY -> misses = 5;
            case NORMAL -> misses = 15;
            case HARD -> misses = 10;
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));


        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11,new RandomStrollGoal(this,0.5f));
        // 远程攻击，需要实现performRangedAttack，继承可远程攻击接口
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 16.0F));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float power) {
        // 示例：发射一个自定义弹射物
        FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(),this.level(),this);
        bone.shoot(target.getX() - this.getX(), target.getY() - this.getY(), target.getZ() - this.getZ(), 1.0F, 0F);
        this.level().addFreshEntity(bone);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        if(isInvulnerableTo( source)){
            return false;
        }
        if(misses > 0) {
            if(source.getEntity() instanceof LivingEntity livingEntity){
                this.setLastHurtByMob(livingEntity); // 核心：设置仇恨目标
            }
            LogUtils.getLogger().info("misses:{}", misses);
            misses--;
            teleport();
            return false;
        }
        return super.hurt(source, power);
    }

    private void teleport() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        RandomSource random = this.random; // 使用末影人自身的随机源
        double radius = ATTACK_RANGE;
        int maxAttempts = 5; // 最大尝试次数

        // 优先尝试平面（XZ轴）传送
        for (int i = 0; i < maxAttempts; i++) {
            double angle = random.nextDouble() * 2 * Math.PI; // 随机角度
            double dx = Math.cos(angle) * radius * (0.75 + random.nextDouble() * 0.25); // 75%~100%半径
            double dz = Math.sin(angle) * radius * (0.75 + random.nextDouble() * 0.25);
            double dy = 0; // 平面高度不变

            if (tryTeleportTo(target.getX() + dx, target.getY() + dy, target.getZ() + dz)) {
                return; // 传送成功则退出
            }
        }

        // 平面尝试失败后，尝试球体范围内其他位置（包括Y轴）
        for (int i = 0; i < maxAttempts; i++) {
            // 球坐标系随机点（θ为极角，φ为方位角）
            double theta = random.nextDouble() * Math.PI; // [0, π]
            double phi = random.nextDouble() * 2 * Math.PI; // [0, 2π]
            double r = radius * (0.75 + random.nextDouble() * 0.25); // 75%~100%半径

            double dx = r * Math.sin(theta) * Math.cos(phi);
            double dy = r * Math.cos(theta);
            double dz = r * Math.sin(theta) * Math.sin(phi);

            if (tryTeleportTo(target.getX() + dx, target.getY() + dy, target.getZ() + dz)) {
                return; // 传送成功则退出
            }
        }

        // 所有尝试失败，默认传送到目标正上方（保底逻辑）
        this.teleportTo(target.getX(), target.getY() + 3, target.getZ());
    }

    /**
     * Sans的传送逻辑（基于末影人原版代码优化）
     * @return 是否传送成功
     */
    private boolean tryTeleportTo(double x, double y, double z) {
        // 2. 检查目标位置是否有效（类似末影人原版逻辑）
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        Level level = this.level();

        // 从目标点向下搜索可站立位置（避免悬空）
        while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos.move(Direction.DOWN);
        }

        BlockState blockState = level.getBlockState(pos);
        // 3. 仅传送到固体且非水方块上
        if (!blockState.getCollisionShape(level, pos).isEmpty() && !blockState.getFluidState().is(FluidTags.WATER)) {
            // 触发传送事件（允许其他模组修改坐标）
            EntityTeleportEvent.EnderEntity event = EventHooks.onEnderTeleport(this, x, y, z);
            if (event.isCanceled()) {
                return false;
            }

            // 执行实际传送
            boolean success = this.randomTeleport(
                    event.getTargetX(),
                    event.getTargetY(),
                    event.getTargetZ(),
                    true // 生成粒子效果
            );

            // 4. 传送成功后的处理
            if (success) {
                // 发送游戏事件（用于红石/侦测器）
                level.gameEvent(GameEvent.TELEPORT, this.position(), GameEvent.Context.of(this));
                // 播放音效（除非Sans是静音的）
                if (!this.isSilent()) {
                    level.playSound(null, this.xo, this.yo, this.zo,
                            SoundEvents.ENDERMAN_TELEPORT, // 使用末影人音效或自定义
                            this.getSoundSource(), 1.0F, 1.0F
                    );
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }
            return success;
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", state -> {
            AnimationController<Sans> controller = state.getController();
            if(state.isMoving()){
                controller.setAnimation(WALK_ANIM);
            }else {
                controller.setAnimation(IDLE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }



    //可攻击的中立生物需要实现的
    @Override
    public int getRemainingPersistentAngerTime() {
        return 0;
    }

    @Override
    public void setRemainingPersistentAngerTime(int i) {

    }

    @Override
    public @Nullable UUID getPersistentAngerTarget() {
        return null;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uuid) {

    }

    @Override
    public void startPersistentAngerTimer() {

    }



    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
