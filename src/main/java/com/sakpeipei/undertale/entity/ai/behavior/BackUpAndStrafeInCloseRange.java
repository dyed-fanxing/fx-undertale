package com.sakpeipei.undertale.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
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

import java.util.List;
import java.util.Optional;

public class BackUpAndStrafeInCloseRange<T extends Mob> extends Behavior<T> {
    private static final float BACK_SPEED = 0.75f;
    private static final Logger log = LoggerFactory.getLogger(BackUpAndStrafeInCloseRange.class);
    protected final double rangeFactor;
    protected double rangeSqr;
    // 内部状态（用于后退摇摆）
    protected int lastRight = 1;      // 上次后退方向（1右，-1左）
    protected boolean isSticky;        // 是否卡住（遇到障碍）
    protected int strafeTime = 5;       // 方向切换计时
    protected int unableMoveTime = 0;

    public BackUpAndStrafeInCloseRange(double rangeFactor) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,MemoryModuleType.WALK_TARGET,MemoryStatus.REGISTERED,MemoryModuleType.LOOK_TARGET,MemoryStatus.REGISTERED),Integer.MAX_VALUE);
        this.rangeFactor = rangeFactor;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, T mob) {
        Optional<LivingEntity> optional = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        rangeSqr = mob.getAttributeValue(Attributes.FOLLOW_RANGE)*rangeFactor;
        rangeSqr = rangeSqr*rangeSqr;
        return optional.isPresent()&&mob.distanceToSqr(optional.get()) <= rangeSqr;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        lastRight = 1;
        isSticky = false;
        strafeTime = 5;
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            // 保持面向目标
            BehaviorUtils.lookAtEntity(mob, target);
            mob.setYRot(mob.yHeadRot);
            // 执行后退逻辑
            handleBackup(mob, target);
        });
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        Optional<LivingEntity> optional = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent()&&mob.distanceToSqr(optional.get()) <= rangeSqr && mob.hasLineOfSight(optional.get());
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        mob.getMoveControl().strafe(0, 0);
    }

    /**
     * 后退核心逻辑，包含方向检测、左右摇摆和障碍处理。
     */
    protected void handleBackup(T mob, LivingEntity target) {
        // 获取当前面向方向
        Vec3 viewVector = mob.getViewVector(1.0f);
        Vec3 backDir = new Vec3(-viewVector.x, 0, -viewVector.z).normalize(); // 正后方方向
        float backDis = (float) Math.sqrt(rangeSqr); // 最大后退距离
        boolean isBack = false;
        while (backDis > 5f) {
            if (isDirectionClear(mob, backDir, backDis)) {
                isBack = true;
                // 方向切换计时
                if (strafeTime > 0) {
                    strafeTime--;
                    if (strafeTime == 0) {
                        lastRight = -lastRight;
                        strafeTime = 5;
                    }
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
                        lastRight = 1;
                        isSticky = true;
                        break;
                    } else if (isDirectionClear(mob, leftDir, backDis)) {
                        unableMoveTime=0;
                        lastRight = -1;
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

        mob.getMoveControl().strafe(isBack ? -BACK_SPEED : 0, lastRight * BACK_SPEED);
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
