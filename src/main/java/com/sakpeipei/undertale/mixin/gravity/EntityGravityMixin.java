package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static net.minecraft.world.entity.Entity.collideBoundingBox;

/**
 * @author Sakpeipei
 * @since 2025/11/14 10:46
 * Entity重力相关方法
 */
@Mixin(Entity.class)
public abstract class EntityGravityMixin{
    private static final Logger log = LoggerFactory.getLogger(EntityGravityMixin.class);

    @Shadow
    private Vec3 position;
    @Shadow
    private Vec3 deltaMovement;
    @Shadow
    private float eyeHeight;
    @Shadow
    private EntityDimensions dimensions;
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    @Shadow
    @Nullable
    private Entity vehicle;

    @Shadow
    private Level level;

    @Shadow
    public Optional<BlockPos> mainSupportingBlockPos;

    @Shadow
    private boolean onGroundNoBlocks;

    @Shadow
    protected abstract float getBlockSpeedFactor();

    @Shadow
    private BlockPos blockPosition;

    @Shadow
    @Nullable
    private BlockState inBlockState;

    @Shadow
    private ChunkPos chunkPosition;

    @Shadow
    private EntityInLevelCallback levelCallback;
    @Shadow
    private static List<VoxelShape> collectColliders(@org.jetbrains.annotations.Nullable Entity p_344804_, Level p_345583_, List<VoxelShape> p_345198_, AABB p_345837_) {
        return null;
    }
    @Shadow
    private static Vec3 collideWithShapes(Vec3 p_198901_, AABB p_198902_, List<VoxelShape> p_198903_) {
        return null;
    }


    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true)
    public void calculateViewVector(float xRot, float yRot, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity)(Object)this;
        GravityData data = self.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN) {
            cir.setReturnValue(data.localToWorld(cir.getReturnValue()));
        }
    }


    /**
     * 根据重力方向重新构建碰撞箱
     */
    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    protected void makeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        if (gravity != Direction.DOWN){
            cir.cancel();
            double halfWidth = this.dimensions.width() * 0.5f;
            cir.setReturnValue(switch (gravity) {
                case UP -> this.dimensions.makeBoundingBox(position.x, position.y - self.getBbHeight(), position.z);
                case EAST -> this.dimensions.makeBoundingBox(position.x - halfWidth, position.y, position.z);
                case WEST -> this.dimensions.makeBoundingBox(position.x + halfWidth, position.y, position.z);
                case SOUTH -> new AABB(position.x-halfWidth, position.y-halfWidth, position.z, position.x+halfWidth, position.y+halfWidth, position.z - self.getBbHeight());
                case NORTH -> new AABB(position.x-halfWidth, position.y-halfWidth, position.z, position.x+halfWidth, position.y+halfWidth, position.z + self.getBbHeight());
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }

    /**
     * 根据重力方向返回眼睛Y轴位置
     */
    @Inject(method = "getEyeY", at = @At(value = "HEAD"), cancellable = true)
    public void getEyeY(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        if (gravity != Direction.DOWN){
            cir.cancel();
            cir.setReturnValue(switch (gravity) {
                case UP -> this.position.y - eyeHeight;
                case EAST, WEST, SOUTH, NORTH -> this.position.y;
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }

    /**
     * 根据重力方向返回眼睛位置
     */
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"), cancellable = true)
    public void getEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        if (gravity != Direction.DOWN){
            cir.cancel();
            cir.setReturnValue(switch (gravity) {
                case UP -> new Vec3(self.getX(), self.getEyeY(), self.getZ());
                case EAST -> new Vec3(self.getX() - eyeHeight, self.getEyeY(), self.getZ());
                case WEST -> new Vec3(self.getX() + eyeHeight, self.getEyeY(), self.getZ());
                case SOUTH -> new Vec3(self.getX(), self.getEyeY(), self.getZ() - eyeHeight);
                case NORTH -> new Vec3(self.getX(), self.getEyeY(), self.getZ() + eyeHeight);
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }

    /**
     * 根据重力方向返回眼睛位置
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"), cancellable = true)
    public void getEyePositionLerp(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        if (gravity != Direction.DOWN){
            cir.cancel();
            cir.setReturnValue(switch (gravity) {
                case UP -> new Vec3(
                        Mth.lerp(partialTick, self.xo, self.getX()),
                        Mth.lerp(partialTick, self.yo, self.getY()) - this.eyeHeight,
                        Mth.lerp(partialTick, self.zo, self.getZ())
                );
                case EAST -> new Vec3(
                        Mth.lerp(partialTick, self.xo, self.getX()) - this.eyeHeight,
                        Mth.lerp(partialTick, self.yo, self.getY()),
                        Mth.lerp(partialTick, self.zo, self.getZ())
                );
                case WEST -> new Vec3(
                        Mth.lerp(partialTick, self.xo, self.getX()) + this.eyeHeight,
                        Mth.lerp(partialTick, self.yo, self.getY()),
                        Mth.lerp(partialTick, self.zo, self.getZ())
                );
                case SOUTH -> new Vec3(
                        Mth.lerp(partialTick, self.xo, self.getX()),
                        Mth.lerp(partialTick, self.yo, self.getY()),
                        Mth.lerp(partialTick, self.zo, self.getZ()) - this.eyeHeight
                );
                case NORTH -> new Vec3(
                        Mth.lerp(partialTick, self.xo, self.getX()),
                        Mth.lerp(partialTick, self.yo, self.getY()),
                        Mth.lerp(partialTick, self.zo, self.getZ()) + this.eyeHeight
                );
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }

    /**
     * 根据重力方向设置自身所在的方块位置blockPosition
     * 默认坐标系下为向下对齐，所以需要修正
     */
    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    public final void setPosRaw(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        GravityData data = self.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity == Direction.DOWN) return;
        ci.cancel();
        Vec3i normal = gravity.getNormal();
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            // blockPosition 是玩家下身所在的那个方块，由于方块是以默认世界坐标系的点来取的，所以需要进行调整
            int i = normal.getY() < 0 ? Mth.floor(x) : Mth.floor(x - 1.0E-6); // 以xMin代表这个方块
            int j = normal.getY() < 0 ? Mth.floor(y) : Mth.floor(y - 1.0E-6); // 以yMin代表这个方块
            int k = normal.getZ() < 0 ? Mth.floor(z) : Mth.floor(z - 1.0E-6); // 以zMin代表这个方块
            if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPos(i, j, k);
                this.inBlockState = null;
                if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
                    this.chunkPosition = new ChunkPos(this.blockPosition);
                }
            }

            this.levelCallback.onMove();
        }

        if (self.isAddedToLevel() && !this.level.isClientSide && !self.isRemoved()) {
            this.level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        }
    }

    /**
     * 根据重力方向检查脚下的可支撑方块位置mainSupportingBlockPos
     * 默认坐标系下为向下对齐，所以需要修正
     */
    @Inject(method = "checkSupportingBlock", at = @At("HEAD"), cancellable = true)
    private void checkSupportingBlock(boolean verticalCollisionBelow, Vec3 deltaMovement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        GravityData gravityData = self.getData(AttachmentTypes.GRAVITY);
        Direction gravity = gravityData.getGravity();
        if (gravity == Direction.DOWN) return;
        if (verticalCollisionBelow) {
            AABB aabb = self.getBoundingBox();
            Vec3i normal = gravity.getNormal();
            AABB aabb1 = aabb.expandTowards(new Vec3(normal.getZ(), normal.getY(), normal.getZ()).scale(1.0E-6));
            Optional<BlockPos> optional = this.level.findSupportingBlock(self, aabb1);
            if (optional.isEmpty() && !this.onGroundNoBlocks) {
                if (deltaMovement != null) {
                    Vec3 logicExpand = new Vec3(-deltaMovement.x, 0, -deltaMovement.z);
                    Vec3 worldExpand = gravityData.localToWorld(logicExpand);
                    AABB aabb2 = aabb1.move(worldExpand);
                    optional = this.level.findSupportingBlock(self, aabb2);
                    this.mainSupportingBlockPos = optional;
                }
            } else {
                this.mainSupportingBlockPos = optional;
            }
            this.onGroundNoBlocks = optional.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
            if (this.mainSupportingBlockPos.isPresent()) {
                this.mainSupportingBlockPos = Optional.empty();
            }
        }
        ci.cancel();
    }

    /**
     * 根据重力方向设置脚下的方块位置BlockPos
     * 默认坐标系下为向下对齐，所以需要修正
     */
    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void BlockPos(float dy, CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        GravityData gravityData = self.getData(AttachmentTypes.GRAVITY);
        Direction gravity = gravityData.getGravity();
        if (gravity == Direction.DOWN) return;
        cir.cancel();
        Vec3i normal = gravity.getNormal();
        Vec3 gravityVec3 = new Vec3(normal.getX(), normal.getY(), normal.getZ());
        if (this.mainSupportingBlockPos.isPresent()) {
            BlockPos blockpos = this.mainSupportingBlockPos.get();
            if (!(dy > 1.0E-5F)) {
                cir.setReturnValue(blockpos);
            } else {
                BlockState blockstate = self.level().getBlockState(blockpos);
                Vec3 offsetPos = this.position.add(gravityVec3.scale(dy));
                //由于LevelChunk.getBlockState取方块默认是以方块底部的坐标为准，这里由于重力向上向上取方块，取到了这个方块的顶部，所以取的是上一个方块，所以需要调整
                cir.setReturnValue((!((double) dy <= 0.5) || !blockstate.collisionExtendsVertically(self.level(), blockpos, self)) ? new BlockPos(
                        normal.getX() == 0 ? blockpos.getX() : (normal.getY() < 0 ? Mth.floor(offsetPos.x) : Mth.floor(offsetPos.x - 1.0E-6)),
                        normal.getY() == 0 ? blockpos.getY() : (normal.getY() < 0 ? Mth.floor(offsetPos.y) : Mth.floor(offsetPos.y - 1.0E-6)),
                        normal.getZ() == 0 ? blockpos.getZ() : (normal.getZ() < 0 ? Mth.floor(offsetPos.z) : Mth.floor(offsetPos.z - 1.0E-6))
                ) : blockpos);
            }
        } else {
            Vec3 offsetPos = this.position.add(gravityVec3.scale(dy));
            int i = normal.getY() < 0 ? Mth.floor(offsetPos.x) : Mth.floor(offsetPos.x - 1.0E-6); // 以xMin代表这个方块
            int j = normal.getY() < 0 ? Mth.floor(offsetPos.y) : Mth.floor(offsetPos.y - 1.0E-6); // 以yMin代表这个方块
            int k = normal.getZ() < 0 ? Mth.floor(offsetPos.z) : Mth.floor(offsetPos.z - 1.0E-6); // 以zMin代表这个方块
            cir.setReturnValue(new BlockPos(i, j, k));
        }
    }

    /**
     * 将局部速度转换为世界速度加到世界位置
     */
    @ModifyArgs(method = "move",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void setPos(Args args) {
        Entity self = (Entity) (Object) this;
        GravityData data = self.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity == Direction.DOWN) return;
        double posX = self.getX();
        double posY = self.getY();
        double posZ = self.getZ();
        // 减出增量，对增量进行转换
        Vector3f vector3f = data.localToWorld((float) ((double) args.get(0) - posX), (float) ((double) args.get(1) - posY), (float) ((double) args.get(2) - posZ));
        args.set(0, posX+vector3f.x);
        args.set(1, posY+vector3f.y);
        args.set(2, posZ+vector3f.z);
    }

    /**
     * 根据重力方向重写碰撞检测
     */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void collide(Vec3 logicDD, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (((Object) this instanceof Player)) {
            GravityData gravityData = self.getData(AttachmentTypes.GRAVITY.get());
            Direction gravity = gravityData.getGravity();
            if (gravity == Direction.DOWN) return; // 标准重力
            cir.cancel();
            Vec3 collidedWorldDD;
            AABB aabb;
            Vec3 worldDD;
            List<VoxelShape> list;
            if (logicDD.lengthSqr() == 0.0) {
                cir.setReturnValue(logicDD);
                return;
            } else {
                aabb = self.getBoundingBox();
                worldDD = gravityData.localToWorld(logicDD);
                // 1. 将位移转换为全局，用于碰撞检测判断
                list = self.level().getEntityCollisions(self, aabb.expandTowards(worldDD));
                // 检测出来的可以进行移动的世界位移
                collidedWorldDD = collideBoundingBox(self, worldDD, aabb, self.level(), list);
            }
            // 3. 将世界位移转换到局部位移系用于逻辑判断
            Vec3 collidedLogicDD = gravityData.worldToLocal(collidedWorldDD);
            // 由于转换有误差，所以当误差小于一定差值时，即可认为碰撞检测出的可移动位移和输入的一样，即无碰撞
            double diffX = logicDD.x - collidedLogicDD.x;
            double diffY = logicDD.y - collidedLogicDD.y;
            double diffZ = logicDD.z - collidedLogicDD.z;
            boolean collidedX = !(diffX * diffX <= 1.0E-7);
            boolean collidedY = !(diffY * diffY <= 1.0E-7);
            boolean collidedZ = !(diffZ * diffZ <= 1.0E-7);
            boolean isFall = collidedY && logicDD.y < 0.0;
            collidedLogicDD = new Vec3(collidedX ? collidedLogicDD.x : logicDD.x, collidedY ? collidedLogicDD.y : logicDD.y, collidedZ ? collidedLogicDD.z : logicDD.z);
            // 4. 使用局部坐标进行逻辑判断
            float maxUpStep = self.maxUpStep();
            if (maxUpStep > 0.0F && (isFall || self.onGround()) && (collidedX || collidedZ)) {
                Vec3i normal = gravity.getNormal();
                Vec3 gravityVec3 = new Vec3(normal.getX(), normal.getY(), normal.getZ());
                AABB aabb1 = isFall ? aabb.move(gravityVec3.multiply(collidedWorldDD)) : aabb;
                AABB aabb2 = aabb1.expandTowards(gravityVec3.x == 0 ? worldDD.x: -gravityVec3.x*maxUpStep, gravityVec3.y == 0 ?worldDD.y:  -gravityVec3.y*maxUpStep, gravityVec3.z == 0 ? worldDD.z :-gravityVec3.z*maxUpStep);
                if (!isFall) {
                    aabb2 = aabb2.expandTowards(gravityVec3.scale(1.0E-5F));
                }

                List<VoxelShape> list1 = collectColliders(self, self.level(), list, aabb2);
                // float f = (float)vec3.y; 取重力方向上碰撞后可移动的速度
                float[] afloat = undertale$collectCandidateStepUpHeights(aabb1, list1, maxUpStep, gravity, (float) collidedWorldDD.multiply(gravityVec3).length());

                log.info("可上升的高度列表：{}", afloat);
                for (float f1 : afloat) {
                    // 尝试"上升"0.5，检测水平是否可以移动，如果可以则上升走过去
                    Vec3 shapeCollideWorldDD = collideWithShapes(new Vec3(gravityVec3.x == 0 ? worldDD.x : -f1 * gravityVec3.x, gravityVec3.y == 0 ? worldDD.y : -f1 * gravityVec3.y, gravityVec3.z == 0 ? worldDD.z : -f1 * gravityVec3.z), aabb1, list1);
                    Vec3 shapeCollideLogicDD = gravityData.worldToLocal(shapeCollideWorldDD);
                    if (shapeCollideLogicDD.horizontalDistanceSqr() > collidedLogicDD.horizontalDistanceSqr()) {
                        // 这里没测出来，放在原版里感觉-d0 = vec3.y 感觉直接用vec3.y也对，也就是直接用检测出的重力方向上可移动的位移
//                        double d0 = aabb.minY - aabb1.minY;
                        cir.setReturnValue(shapeCollideLogicDD.add(collidedLogicDD));
                        return;
                    }
                }
            }
            cir.setReturnValue(collidedLogicDD);
        }
    }
    /**
     * 根据重力方向重收集可以走上 <=maxUpStep高度方块 的高度列表
     */
    @Unique
    private static float[] undertale$collectCandidateStepUpHeights(AABB aabb, List<VoxelShape> voxelShapes, float maxUpStep, Direction gravity, float dy) {
        FloatSet floatset = new FloatArraySet(4);
        for (VoxelShape voxelshape : voxelShapes) {
            DoubleList coords = voxelshape.getCoords(gravity.getAxis());
            // 是否为升序，所有负数方向需要升序，正数需要降序
            boolean isAsc = gravity.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
            int start = isAsc ? 0 : coords.size() - 1;
            int end = isAsc ? coords.size() : -1;
            int step = isAsc ? 1 : -1;
            for (int i = start; i != end; i += step) {
                double d0 = coords.getDouble(i);
                float f = 0;
                switch (gravity) {
                    case UP -> f = (float) (aabb.maxY - d0);
                    case EAST -> f = (float) (aabb.maxX - d0);
                    case WEST -> f = (float) (d0 - aabb.minX);
                    case SOUTH -> f = (float) (aabb.maxZ - d0);
                    case NORTH -> f = (float) (d0 - aabb.minZ);
                }
                if (!(f < 0.0F) && f != dy) {
                    if (f > maxUpStep) {
                        break;
                    }
                    floatset.add(f);
                }
            }
        }
        float[] afloat = floatset.toFloatArray();
        FloatArrays.unstableSort(afloat);
        return afloat;
    }

}
