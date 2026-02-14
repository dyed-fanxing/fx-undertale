package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerGravityMixin {

    @Shadow protected abstract void moveTowardsClosestSpace(double p_108705_, double p_108706_);

    @Redirect(method = "aiStep",at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;noPhysics:Z",
                    ordinal = 0, opcode = Opcodes.GETFIELD)
    )
    private boolean redirectNoPhysics(LocalPlayer player) {
        GravityData data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() == Direction.DOWN) {
            return player.noPhysics;  // 原版逻辑
        }
        // 你的重力版本水平检测
        double w = player.getBbWidth() * 0.35;
        switch (data.getGravity()) {
            case SOUTH, NORTH -> {
                this.moveTowardsClosestSpace(player.getX() - w, player.getY() + w);
                this.moveTowardsClosestSpace(player.getX() - w, player.getY() - w);
                this.moveTowardsClosestSpace(player.getX() + w, player.getY() - w);
                this.moveTowardsClosestSpace(player.getX() + w, player.getY() + w);
            }
            case EAST, WEST -> {
                this.moveTowardsClosestSpace(player.getY() - w, player.getZ() + w);
                this.moveTowardsClosestSpace(player.getY() - w, player.getZ() - w);
                this.moveTowardsClosestSpace(player.getY() + w, player.getZ() - w);
                this.moveTowardsClosestSpace(player.getY() + w, player.getZ() + w);
            }
        }
        return true;
    }

    // 2. 重写 moveTowardsClosestSpace 方法
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onMoveTowardsClosestSpace(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        GravityData data = player.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;

        ci.cancel();

        switch (data.getGravity()) {
            case SOUTH, NORTH -> {
                // 水平面是 XY，高度是 Z
                BlockPos blockpos = BlockPos.containing(x, z, player.getZ());
                gravity$moveTowardsClosestSpace(player, blockpos, data.getGravity());
            }
            case EAST, WEST -> {
                // 水平面是 YZ，高度是 X
                BlockPos blockpos = BlockPos.containing(player.getX(), x, z);
                gravity$moveTowardsClosestSpace(player, blockpos, data.getGravity());
            }
        }
    }
    @Unique
    private void gravity$moveTowardsClosestSpace(LocalPlayer player, BlockPos blockpos, Direction gravity) {
        if (!gravity$suffocatesAt(player, blockpos, gravity)) return;

        double d0 = player.getX() - blockpos.getX();
        double d1 = player.getY() - blockpos.getY();
        double d2 = player.getZ() - blockpos.getZ();

        Direction escapeDir = null;
        double minDist = Double.MAX_VALUE;

        Direction[] dirs = switch (gravity) {
            case SOUTH, NORTH -> new Direction[]{Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};
            case EAST, WEST -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
            default -> new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        };

        for (Direction dir : dirs) {
            BlockPos relative = blockpos.relative(dir);
            if (gravity$suffocatesAt(player, relative, gravity)) continue;

            double dist = switch (gravity) {
                case SOUTH, NORTH -> {
                    if (dir.getAxis() == Direction.Axis.X) yield Math.abs(d0 - dir.getStepX());
                    if (dir.getAxis() == Direction.Axis.Y) yield Math.abs(d1 - dir.getStepY());
                    yield Double.MAX_VALUE;
                }
                case EAST, WEST -> {
                    if (dir.getAxis() == Direction.Axis.Y) yield Math.abs(d1 - dir.getStepY());
                    if (dir.getAxis() == Direction.Axis.Z) yield Math.abs(d2 - dir.getStepZ());
                    yield Double.MAX_VALUE;
                }
                default -> Double.MAX_VALUE;
            };

            if (dist < minDist) {
                minDist = dist;
                escapeDir = dir;
            }
        }

        if (escapeDir != null) {
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(switch (gravity) {
                case SOUTH, NORTH -> {
                    if (escapeDir.getAxis() == Direction.Axis.X)
                        yield new Vec3(0.1 * escapeDir.getStepX(), delta.y, delta.z);
                    if (escapeDir.getAxis() == Direction.Axis.Y)
                        yield new Vec3(delta.x, 0.1 * escapeDir.getStepY(), delta.z);
                    yield delta;
                }
                case EAST, WEST -> {
                    if (escapeDir.getAxis() == Direction.Axis.Y)
                        yield new Vec3(delta.x, 0.1 * escapeDir.getStepY(), delta.z);
                    if (escapeDir.getAxis() == Direction.Axis.Z)
                        yield new Vec3(delta.x, delta.y, 0.1 * escapeDir.getStepZ());
                    yield delta;
                }
                default -> delta;
            });
        }
    }

    @Unique
    private boolean gravity$suffocatesAt(LocalPlayer player, BlockPos pos, Direction gravity) {
        AABB box = player.getBoundingBox();
        AABB checkBox = switch (gravity) {
            case SOUTH, NORTH -> new AABB(pos.getX(), pos.getY(), box.minZ,
                    pos.getX() + 1, pos.getY() + 1, box.maxZ);
            case EAST, WEST -> new AABB(box.minX, pos.getY(), pos.getZ(),
                    box.maxX, pos.getY() + 1, pos.getZ() + 1);
            default -> new AABB(pos.getX(), box.minY, pos.getZ(),
                    pos.getX() + 1, box.maxY, pos.getZ() + 1);
        };
        return player.level().collidesWithSuffocatingBlock(player, checkBox.deflate(1.0E-7));
    }
}
