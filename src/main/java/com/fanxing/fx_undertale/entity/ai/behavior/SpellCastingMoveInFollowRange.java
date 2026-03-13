package com.fanxing.fx_undertale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.fanxing.fx_undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpellCastingMoveInFollowRange<T extends Mob> extends Behavior<T> {
    public static final float STRAFE_SCALE = 0.5f;
    private static final Logger log = LoggerFactory.getLogger(SpellCastingMoveInFollowRange.class);
    protected final double closeRangeSqr;
    protected final double closeRange;
    protected final double midRangeSqr;
    protected final double followRangeSqr;
    protected final float speedModifier;


    // 内部状态（用于后退摇摆）
    protected int right = 1;      // 上次左右方向（1右，-1左）
    protected int front = 1;          // 上次前进方向（1右，-1左）
    protected boolean isSticky;        // 是否卡住（遇到障碍）
    protected int strafeRightTime = 5;       // 左右方向切换计时
    protected int strafeFrontTime = 5;       // 前后方向切换计时
    protected int unableMoveTime = 0;
    protected double disSqr;

    public SpellCastingMoveInFollowRange(Mob mob,double closeRangeFactor,double midRangeFactor,float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET,MemoryStatus.REGISTERED,MemoryModuleType.LOOK_TARGET,MemoryStatus.REGISTERED,MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),Integer.MAX_VALUE);
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        followRangeSqr = followRange*followRange;
        closeRange = followRange*closeRangeFactor;
        closeRangeSqr = closeRange*closeRange;
        double midRange = followRange*midRangeFactor;
        midRangeSqr = midRange*midRange;
        this.speedModifier = speedModifier;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        right = 1;
        isSticky = false;
        strafeRightTime = 5;
        strafeFrontTime = 5;
        mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> disSqr = mob.distanceToSqr(target));
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            disSqr = mob.distanceToSqr(target);
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,new IgnoringSensorEntityTracker(target, true));
            if(!mob.hasLineOfSight(target)){
                mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(new IgnoringSensorEntityTracker(target, false), speedModifier, 5));
            }else{
                if(disSqr <= closeRangeSqr) {
                    mob.setYRot(mob.yHeadRot);
                    handleCloseRange(mob, target,disSqr);
                }else if(disSqr <= midRangeSqr) {
                    mob.getMoveControl().strafe(0, 0);
                    handleMidRange(mob,target,disSqr);
                }else{
                    handleFarRange(mob,target,disSqr);
                }
            }
        });
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        return disSqr <= followRangeSqr;
    }

    /**
     * 后退核心逻辑，包含方向检测、左右摇摆和障碍处理。
     */
    protected void handleCloseRange(T mob, LivingEntity target,double disSqr) {
        // 获取当前面向方向
        Vec3 viewVector = mob.getViewVector(1.0f);
        Vec3 backDir = new Vec3(-viewVector.x, 0, -viewVector.z).normalize(); // 正后方方向
        double backDis = closeRange; // 最大后退距离
        boolean isBack = false;
        while (backDis > 5f) {
            if (isDirectionClear(mob, backDir, backDis)) {
                isBack = true;
                // 方向切换计时
                if (strafeRightTime-- <= 0) {
                    right = -right;
                    strafeRightTime = 5 + mob.getRandom().nextInt(5);
                }
                isSticky = false;
                unableMoveTime = 0;
                break;
            } else {
                unableMoveTime++;
                if (!isSticky) {
                    Vec3 rightDir = backDir.yRot(-90 * Mth.DEG_TO_RAD);
                    Vec3 leftDir = backDir.yRot(90 * Mth.DEG_TO_RAD);
                    if (isDirectionClear(mob, rightDir, backDis)) {
                        unableMoveTime=0;
                        right = 1;
                        isSticky = true;
                        break;
                    } else if (isDirectionClear(mob, leftDir, backDis)) {
                        unableMoveTime=0;
                        right = -1;
                        isSticky = true;
                        break;
                    } else {
                        backDis *= 0.5f;
                    }
                } else {
                    backDis *= 0.5f;
                }
            }
        }

        handleJump(mob);

        handleUnableToMove(mob,target);
        // 无法移动，可考虑传送（但这里不处理，由专门的传送行为负责）

        mob.getMoveControl().strafe(isBack ? -STRAFE_SCALE : 0, right * STRAFE_SCALE);
    }

    protected void handleMidRange(T mob, LivingEntity target,double disSqr){
        mob.setYRot(mob.yHeadRot);
        if (strafeRightTime-- <= 0) {
            strafeRightTime = 5 + mob.getRandom().nextInt(25);
            right = mob.getRandom().nextBoolean() ? 1 : -1;
        }
        if (strafeFrontTime-- <= 0) {
            strafeFrontTime = 5 + mob.getRandom().nextInt(25);
            front = mob.getRandom().nextInt(3)-1;
        }
        mob.getMoveControl().strafe(front * STRAFE_SCALE, right * STRAFE_SCALE);

    }

    protected void handleFarRange(T mob, LivingEntity target,double disSqr){
        mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(new IgnoringSensorEntityTracker(target, true), speedModifier, 0));
    }

    protected void handleJump(T mob){
        // 跳跃辅助（检测是否需要跳跃跨越障碍）
        Level level = mob.level();
        float bbWidth = mob.getBbWidth();
        Vec3 movement = mob.getDeltaMovement();
        Vec3 scaleDD = new Vec3(movement.x, 0, movement.z).normalize().scale(bbWidth);
        AABB canJumpAABB = mob.getBoundingBox().move(scaleDD.x, 1.0f, scaleDD.z);
        if (level.noCollision(canJumpAABB)) {
            canJumpAABB = mob.getBoundingBox().move(scaleDD.x, mob.maxUpStep(), scaleDD.z);
            if (!level.noCollision(canJumpAABB)) {
                mob.getJumpControl().jump();
            }
        }

    }

    protected void handleUnableToMove (T mob, LivingEntity target) {

    }

    /**
     * 检测从当前位置沿 dir 方向移动 dist 距离是否畅通
     */
    protected boolean isDirectionClear(T mob, Vec3 dir, double dist) {
        Level level = mob.level();
        Vec3 foot = mob.position().add(0, 1E-5F, 0);
        Vec3 heightPos = new Vec3(mob.getX(), mob.getY(0.5f + 1E-5f), mob.getZ());
        Vec3 scaleDir = dir.scale(dist);
        BlockHitResult heightHit = level.clip(new ClipContext(heightPos, heightPos.add(scaleDir),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        boolean heightClear = heightHit.getType() == HitResult.Type.MISS;
        BlockHitResult footHit = level.clip(new ClipContext(foot, foot.add(scaleDir),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        boolean footClear = footHit.getType() == HitResult.Type.MISS;
        if (heightClear) {
            if (footClear) return true;
            // 仅脚部碰撞且眼睛畅通 → 可能是一格高障碍
            BlockPos hitPos = footHit.getBlockPos();
            BlockState state = level.getBlockState(hitPos);
            double blockTop = hitPos.getY() + state.getShape(level, hitPos).max(Direction.Axis.Y);
            Vec3 standPos = new Vec3(hitPos.getX() + 0.5, blockTop, hitPos.getZ() + 0.5);
            AABB entityBox = mob.getBoundingBox().move(standPos.subtract(mob.position()));
            return level.noCollision(entityBox);
        }
        return false;
    }
}
