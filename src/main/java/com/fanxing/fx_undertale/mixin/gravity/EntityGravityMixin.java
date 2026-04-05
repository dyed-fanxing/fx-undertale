package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.llamalad7.mixinextras.sugar.Local;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

import static com.fanxing.fx_undertale.utils.GravityUtils.COLLIED_EPSILON;
import static com.fanxing.fx_undertale.utils.GravityUtils.FLOOR_EPSILON;
import static net.minecraft.world.entity.Entity.collideBoundingBox;

/**
 * @author Sakpeipei
 * @since 2025/11/14 10:46
 * Entity重力相关方法
 */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
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
    private static List<VoxelShape> collectColliders(@Nullable Entity p_344804_, Level p_345583_, List<VoxelShape> p_345198_, AABB p_345837_) {
        return null;
    }


    @Shadow public abstract EntityDimensions getDimensions(Pose p_19975_);

    @Shadow public abstract boolean isNoGravity();

    @Shadow public abstract float maxUpStep();

    @Shadow private boolean onGround;

    @Shadow public abstract boolean onGround();

    @Shadow
    private static Vec3 collideWithShapes(Vec3 p_198901_, AABB p_198902_, List<VoxelShape> p_198903_){
        return null;
    }

    @Shadow protected abstract void onInsideBlock(BlockState p_20005_);

    @Shadow public int tickCount;

    @Shadow public abstract AABB getBoundingBox();

    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true)
    public void calculateViewVector(float xRot, float yRot, CallbackInfoReturnable<Vec3> cir) {
        if(this instanceof OBBHolder) return;
        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
            cir.setReturnValue(GravityUtils.localToWorld(gravity,cir.getReturnValue()));
        }
    }


    /**
     * 根据重力方向重新构建碰撞箱
     */
    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    protected void makeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if(this instanceof OBBHolder) return;
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
            cir.cancel();
            EntityDimensions dimensions = getDimensions(null);
            double halfWidth = dimensions.width() * 0.5f;
            cir.setReturnValue(switch (gravity) {
                case UP -> new AABB(position.x - halfWidth, position.y - self.getBbHeight(), position.z - halfWidth, position.x + halfWidth, position.y, position.z + halfWidth);
                case EAST -> new AABB(position.x, position.y - halfWidth, position.z - halfWidth, position.x - self.getBbHeight(), position.y + halfWidth, position.z + halfWidth);
                case WEST -> new AABB(position.x, position.y - halfWidth, position.z - halfWidth, position.x + self.getBbHeight(), position.y + halfWidth, position.z + halfWidth);
                case SOUTH -> new AABB(position.x - halfWidth, position.y - halfWidth, position.z, position.x + halfWidth, position.y + halfWidth, position.z - self.getBbHeight());
                case NORTH -> new AABB(position.x - halfWidth, position.y - halfWidth, position.z, position.x + halfWidth, position.y + halfWidth, position.z + self.getBbHeight());
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }

    /**
     * 根据重力方向返回眼睛Y轴位置
     */
    @Inject(method = "getEyeY", at = @At(value = "HEAD"), cancellable = true)
    public void getEyeY(CallbackInfoReturnable<Double> cir) {
        if(this instanceof OBBHolder) return;
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN)  {
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
        if(this instanceof OBBHolder) return;
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
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
        if(this instanceof OBBHolder) return;
        Entity self = (Entity) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
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
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity == Direction.DOWN ) return;
        ci.cancel();
        Vec3i normal = gravity.getNormal();
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            // blockPosition 是玩家下身所在的那个方块，由于方块是以默认世界坐标系的点来取的，所以需要进行调整
            int i = normal.getX() < 0 ? Mth.floor(x) : Mth.floor(x - FLOOR_EPSILON); // 以xMin代表这个方块
            int j = normal.getY() < 0 ? Mth.floor(y) : Mth.floor(y - FLOOR_EPSILON); // 以yMin代表这个方块
            int k = normal.getZ() < 0 ? Mth.floor(z) : Mth.floor(z - FLOOR_EPSILON); // 以zMin代表这个方块
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
     */
    @Inject(method = "checkSupportingBlock", at = @At("HEAD"), cancellable = true)
    private void checkSupportingBlock(boolean verticalCollisionBelow, Vec3 deltaMovement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity == Direction.DOWN) return;
        if (verticalCollisionBelow) {
            AABB aabb = self.getBoundingBox();
            Vec3i normal = gravity.getNormal();
            AABB aabb1 = aabb.expandTowards(new Vec3(normal.getZ(), normal.getY(), normal.getZ()).scale(FLOOR_EPSILON));
            Optional<BlockPos> optional = this.level.findSupportingBlock(self, aabb1);
            if (optional.isEmpty() && !this.onGroundNoBlocks) {
                if (deltaMovement != null) {
                    Vec3 logicExpand = new Vec3(-deltaMovement.x, 0, -deltaMovement.z);
                    Vec3 worldExpand = GravityUtils.localToWorld(gravity,logicExpand);
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
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
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
                        normal.getX() == 0 ? blockpos.getX() : (normal.getY() < 0 ? Mth.floor(offsetPos.x) : Mth.floor(offsetPos.x - FLOOR_EPSILON)),
                        normal.getY() == 0 ? blockpos.getY() : (normal.getY() < 0 ? Mth.floor(offsetPos.y) : Mth.floor(offsetPos.y - FLOOR_EPSILON)),
                        normal.getZ() == 0 ? blockpos.getZ() : (normal.getZ() < 0 ? Mth.floor(offsetPos.z) : Mth.floor(offsetPos.z - FLOOR_EPSILON))
                ) : blockpos);
            }
        } else {
            Vec3 offsetPos = this.position.add(gravityVec3.scale(dy));
            int i = normal.getX() < 0 ? Mth.floor(offsetPos.x) : Mth.floor(offsetPos.x - FLOOR_EPSILON); // 以xMin代表这个方块
            int j = normal.getY() < 0 ? Mth.floor(offsetPos.y) : Mth.floor(offsetPos.y - FLOOR_EPSILON); // 以yMin代表这个方块
            int k = normal.getZ() < 0 ? Mth.floor(offsetPos.z) : Mth.floor(offsetPos.z - FLOOR_EPSILON); // 以zMin代表这个方块
            cir.setReturnValue(new BlockPos(i, j, k));
        }
    }

    /**
     * 将局部速度转换为世界速度加到世界位置
     */
    @ModifyArgs(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"), remap = false)
    private void setPos(Args args) {
        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY);
        if (gravity == Direction.DOWN) return;

        double posX = self.getX();
        double posY = self.getY();
        double posZ = self.getZ();
        // 计算世界位移
        Vec3 vec3 = GravityUtils.localToWorld(gravity,(double) args.get(0) - posX,(double) args.get(1) - posY,(double) args.get(2) - posZ);
        double newX = posX + vec3.x;
        double newY = posY + vec3.y;
        double newZ = posZ + vec3.z;
        log.info("原位置：{},计算出的：({},{},{})",self.position(), newX, newY, newZ);
//        // 主动对齐：根据重力轴将对应坐标舍入到整数（方块边界）
//        switch (gravity) {
//            case UP -> {
//                if (Math.abs(newY - Math.round(newY)) < COLLIED_EPSILON) newY = Math.round(newY);
//            }
//            case EAST,WEST -> {
//                if (Math.abs(newX - Math.round(newX)) < COLLIED_EPSILON) newX = Math.round(newX);
//            }
//            case SOUTH, NORTH -> {
//                if (Math.abs(newZ - Math.round(newZ)) < COLLIED_EPSILON) newZ = Math.round(newZ);
//            }
//        }
//        log.info("精度取整数后的新位置：({},{},{})",newX, newY, newZ);
        args.set(0, newX);
        args.set(1, newY);
        args.set(2, newZ);
    }


    /**
     * 根据重力方向重写碰撞检测
     */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void collide(Vec3 logicDD, CallbackInfoReturnable<Vec3> cir) {

        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypes.GRAVITY.get());
        if (gravity == Direction.DOWN) return;
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
            worldDD = GravityUtils.localToWorld(gravity,logicDD);
            // 1. 将位移转换为全局，用于碰撞检测判断
            list = self.level().getEntityCollisions(self, aabb.expandTowards(worldDD));
            // 2. 检测出来的可以进行移动的世界位移
            //KEY 这里直接使用原版的collideBoundingBox方法，也就是使用原版的collideWithShapes进行三个轴的可移动碰撞检测，
            //    collidedWorldDD = collideBoundingBox(self,worldDD, aabb,self.level(), list);
            //    原版是优先世界Y，然后XZ，但最后三个轴都会检测，所以也可以用
            //    这里直接使用重写的方法，优先从重力轴开始检测
            List<VoxelShape> collideShapes = collectColliders(self, level, list, aabb.expandTowards(worldDD));
            collidedWorldDD = undertale$gravityCollideWithShapes(worldDD, aabb, collideShapes,gravity);
        }
        // 3. 将世界位移转换到局部位移系用于逻辑判断
        Vec3 collidedLogicDD = GravityUtils.worldToLocal(gravity,collidedWorldDD);
        // 由于转换有误差，所以当误差小于一定差值时，即可认为碰撞检测出的可移动位移和输入的一样，即无碰撞
        double diffX = logicDD.x - collidedLogicDD.x;
        double diffY = logicDD.y - collidedLogicDD.y;
        double diffZ = logicDD.z - collidedLogicDD.z;
        boolean isCollidedX = !(diffX * diffX <= GravityUtils.COLLIED_EPSILON); // KEY 是否发生碰撞的精度要大些，但不能完全用等于，因为重力转换会有精度误差
        boolean isCollidedY = !(diffY * diffY <= GravityUtils.COLLIED_EPSILON);
        boolean isCollidedZ = !(diffZ * diffZ <= GravityUtils.COLLIED_EPSILON);
        boolean isLanding = isCollidedY && logicDD.y < (double) 0.0F; // 是否正好落地

        // 因为原版move方法 后续的检测逻辑会使用 输入的位移和碰撞检测后可移动的位移进行相等判断
        // 如果不将在可接受精度内的位移转换回输入的，则会因精度误差导致原版的后续检测失败
        collidedLogicDD = new Vec3(isCollidedX ? collidedLogicDD.x : logicDD.x, isCollidedY ? collidedLogicDD.y : logicDD.y, isCollidedZ ? collidedLogicDD.z : logicDD.z);
        if(collidedLogicDD.lengthSqr() == logicDD.lengthSqr()) collidedWorldDD = worldDD;
        else collidedWorldDD = GravityUtils.localToWorld(gravity,collidedLogicDD);

        // 4. 使用局部坐标进行逻辑判断
        float maxUpStep = self.maxUpStep();
        if (maxUpStep > 0.0F && (isLanding || self.onGround()) && (isCollidedX || isCollidedZ)) {
            Vec3i normal = gravity.getNormal();
            Vec3 gravityVec3 = new Vec3(normal.getX(), normal.getY(), normal.getZ());
            //碰撞世界位移 在重力轴上的分量
            Vec3 collidedWorldDDGravityPortion = GravityUtils.getAxialComponent(gravity,collidedWorldDD);
            //只取碰撞位移上的重力分量进行移动aabb得到aabb1
            AABB aabb1 = isLanding ? aabb.move(collidedWorldDDGravityPortion) : aabb;
//            AABB aabb2 = aabb1.expandTowards(gravityVec3.x == 0 ? worldDD.x : -gravityVec3.x * maxUpStep, gravityVec3.y == 0 ? worldDD.y : -gravityVec3.y * maxUpStep, gravityVec3.z == 0 ? worldDD.z : -gravityVec3.z * maxUpStep);
            AABB aabb2 = aabb1.expandTowards(GravityUtils.compose(worldDD,gravityVec3.scale(-maxUpStep)));
            if (!isLanding)    aabb2 = aabb2.expandTowards(gravityVec3.scale(Mth.EPSILON));

            List<VoxelShape> list1 = collectColliders(self, self.level(), list, aabb2);
            float[] afloat = undertale$collectCandidateStepUpHeights(aabb1, list1, maxUpStep, gravity, (float) collidedWorldDDGravityPortion.length());
            log.debug("可上升的高度列表：{}", afloat);
            for (float f1 : afloat) {
                // 尝试抬升能走过的最大高度，检测水平是否可以移动，如果可以则上升走过去
//                Vec3 vec3 = new Vec3(gravityVec3.x == 0 ? worldDD.x : -f1 * gravityVec3.x, gravityVec3.y == 0 ? worldDD.y : -f1 * gravityVec3.y, gravityVec3.z == 0 ? worldDD.z : -f1 * gravityVec3.z);
                Vec3 vec3 = GravityUtils.compose(gravityVec3.scale(-f1),worldDD);

                Vec3 shapeCollideWorldDD = collideWithShapes(vec3, aabb1, list1);
                Vec3 shapeCollideLogicDD = GravityUtils.worldToLocal(gravity,shapeCollideWorldDD);
                // KEY 台阶检测的精度要小些
                if (Mth.abs((float) (shapeCollideLogicDD.horizontalDistanceSqr()-collidedLogicDD.horizontalDistanceSqr())) >= FLOOR_EPSILON ) {
                    // 这里没测出来，放在原版里感觉-d0 = vec3.y 感觉直接用vec3.y也对，也就是直接用检测出的重力方向上可移动的位移
                    // 这一步是为了检测，对于不同面有不同的高度的方块
//                    double d0 = aabb.minY - aabb1.minY;
//                    log.debug("偏移：{}",fx_undertale$getOffsetVec(aabb,aabb1,gravity));
//                    log.debug("最后计算的速度：{}",shapeCollideLogicDD.add(fx_undertale$getOffsetVec(aabb,aabb1,gravity)));
                    cir.setReturnValue(shapeCollideLogicDD.add(fx_undertale$getOffsetVec(aabb,aabb1,gravity)));
                    return;
                }
            }
        }
        cir.setReturnValue(collidedLogicDD);
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
                    case DOWN -> f = (float) (d0 - aabb.minY);
                    case UP -> f = (float) (aabb.maxY - d0);
                    case EAST -> f = (float) (aabb.maxX - d0);
                    case WEST -> f = (float) (d0 - aabb.minX);
                    case SOUTH -> f = (float) (aabb.maxZ - d0);
                    case NORTH -> f = (float) (d0 - aabb.minZ);
                }
                log.info("当前重力方向：{}，高度f：{}",gravity,f);
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

    /**
     * 优先检测重力轴方向，代替原版的  collideWithShapes方法
     */
    @Unique
    private Vec3 undertale$gravityCollideWithShapes(Vec3 movement, AABB bb, List<VoxelShape> shapes, Direction gravity) {
        if (shapes.isEmpty()) return movement;
        Direction.Axis axis = gravity.getAxis();
        double x = movement.x, y = movement.y, z = movement.z;
        AABB aabb = bb;

        if (axis == Direction.Axis.Y) {
            if (y != 0.0) {
                y = Shapes.collide(Direction.Axis.Y, aabb, shapes, y);
                if (y != 0.0) aabb = aabb.move(0, y, 0);
            }
            boolean flag = Math.abs(x) < Math.abs(z);
            if (flag && z != 0.0) {
                z = Shapes.collide(Direction.Axis.Z, aabb, shapes, z);
                if (z != 0.0) aabb = aabb.move(0, 0, z);
            }
            if (x != 0.0) {
                x = Shapes.collide(Direction.Axis.X, aabb, shapes, x);
                if (!flag && x != 0.0) aabb = aabb.move(x, 0, 0);
            }
            if (!flag && z != 0.0) {
                z = Shapes.collide(Direction.Axis.Z, aabb, shapes, z);
            }
            return new Vec3(x, y, z);
        } else if (axis == Direction.Axis.X) {
            if (x != 0.0) {
                x = Shapes.collide(Direction.Axis.X, aabb, shapes, x);
                if (x != 0.0) aabb = aabb.move(x, 0, 0);
            }
            boolean flag = Math.abs(y) < Math.abs(z);
            if (flag && z != 0.0) {
                z = Shapes.collide(Direction.Axis.Z, aabb, shapes, z);
                if (z != 0.0) aabb = aabb.move(0, 0, z);
            }
            if (y != 0.0) {
                y = Shapes.collide(Direction.Axis.Y, aabb, shapes, y);
                if (!flag && y != 0.0) aabb = aabb.move(0, y, 0);
            }
            if (!flag && z != 0.0) {
                z = Shapes.collide(Direction.Axis.Z, aabb, shapes, z);
            }
            return new Vec3(x, y, z);
        } else { // Axis.Z
            if (z != 0.0) {
                z = Shapes.collide(Direction.Axis.Z, aabb, shapes, z);
                if (z != 0.0) aabb = aabb.move(0, 0, z);
            }
            boolean flag = Math.abs(x) < Math.abs(y);
            if (flag && y != 0.0) {
                y = Shapes.collide(Direction.Axis.Y, aabb, shapes, y);
                if (y != 0.0) aabb = aabb.move(0, y, 0);
            }
            if (x != 0.0) {
                x = Shapes.collide(Direction.Axis.X, aabb, shapes, x);
                if (!flag && x != 0.0) aabb = aabb.move(x, 0, 0);
            }
            if (!flag && y != 0.0) {
                y = Shapes.collide(Direction.Axis.Y, aabb, shapes, y);
            }
            return new Vec3(x, y, z);
        }
    }
    @Unique
    private Vec3 fx_undertale$getOffsetVec(AABB oldAABB, AABB newAABB, Direction gravity) {
        return switch (gravity) {
            case DOWN -> new Vec3(0, oldAABB.minY - newAABB.minY, 0);
            case UP -> new Vec3(0, -(oldAABB.maxY - newAABB.maxY), 0);
            case EAST -> new Vec3(-(oldAABB.maxX - newAABB.maxX), 0, 0);
            case WEST -> new Vec3(oldAABB.minX - newAABB.minX, 0, 0);
            case SOUTH -> new Vec3(0, 0, -(oldAABB.maxZ - newAABB.maxZ));
            case NORTH -> new Vec3(0, 0, oldAABB.minZ - newAABB.minZ);
        };
    }



















//    @Inject(method = "move", at = @At(value = "HEAD"))
//    private void onHead(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("----------------------tick：{}，move: 输入的movement{},位置：{}，碰撞箱：{}",tickCount,movement,position,getBoundingBox());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V",shift =  At.Shift.BEFORE))
//    private void onBeforeSetOnGroundWithMovement(MoverType moverType, Vec3 movement, CallbackInfo ci, @Local(ordinal = 1) Vec3 vec3) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: before setOnGroundWithMovement, verticalCollision={}, verticalCollisionBelow={}, 碰撞后的 vec3={}",
//                    self.verticalCollision, self.verticalCollisionBelow, vec3);
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
//    private void onAfterSetOnGroundWithMovement(MoverType moverType, Vec3 movement, CallbackInfo ci,  @Local(ordinal = 1) Vec3 vec3) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: after setOnGroundWithMovement, onGround={}, 碰撞后的vec3：{}，deltaMovement={}", self.onGround(),vec3, self.getDeltaMovement());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.BEFORE))
//    private void onBeforeUpdateEntityAfterFallOn(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: before updateEntityAfterFallOn, deltaMovement={}, onGround={},", self.getDeltaMovement(), self.onGround());
//        }
//    }
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
//    private void onAfterUpdateEntityAfterFallOn(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: after updateEntityAfterFallOn, deltaMovement={}, onGround={},", self.getDeltaMovement(), self.onGround());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tryCheckInsideBlocks()V", shift = At.Shift.BEFORE))
//    private void onBeforeTryCheckInsideBlocks(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: before tryCheckInsideBlocks, deltaMovement={}, position={},blockPosition：{}", self.getDeltaMovement(), self.position(),self.blockPosition());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tryCheckInsideBlocks()V", shift = At.Shift.AFTER))
//    private void onAfterTryCheckInsideBlocks(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: after tryCheckInsideBlocks, deltaMovement={}, onGround={}", self.getDeltaMovement(), self.onGround());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.BEFORE, ordinal = 0))
//    private void onBeforeApplySpeedFactor(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: before applying block speed factor, deltaMovement={}", self.getDeltaMovement());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER, ordinal = 0))
//    private void onAfterApplySpeedFactor(MoverType moverType, Vec3 movement, CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("move: after applying block speed factor, deltaMovement={}", self.getDeltaMovement());
//        }
//    }
//
//    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V", shift = At.Shift.BEFORE, ordinal = 1))
//    private void onSetPosInMove(MoverType moverType, Vec3 movement, CallbackInfo ci,@Local(ordinal = 1) Vec3 vec3) {
//        Entity self = (Entity) (Object) this;
//        if (self instanceof IronGolem) {
//            log.info("返回的碰撞位移大于1.E-7进入设置位置：位置之前：{}，局部位移为：{}，y轴相加：{}",position,vec3,position.y+vec3.y);
//        }
//    }
//    @Inject(method = "setPosRaw", at = @At("HEAD"))
//    private void onSetPosRawHead(double x, double y, double z, CallbackInfo ci) {
//        Entity self = (Entity)(Object)this;
//        if(self instanceof IronGolem) {
//            log.info("设置位置前：原始位置：{}，碰撞箱：{}，要设置的位置：({},{},{})",position,self.getBoundingBox(),x,y,z);
//        }
//    }
//    @Inject(method = "setPosRaw", at = @At("RETURN"))
//    private void onSetPosRawReturn(double x, double y, double z, CallbackInfo ci) {
//        Entity self = (Entity)(Object)this;
//        if(self instanceof IronGolem) {
//            log.info("设置位置后的位置：{}，碰撞箱：{}",position,getBoundingBox());
//            // 避免在构造函数或未完全初始化时检测
//            if (self.level() == null || !self.isAddedToLevel()) return;
//            if (self.level().isClientSide) return;
//            AABB bb = self.getBoundingBox();
//            boolean overlapping = self.level().getBlockCollisions(self, bb).iterator().hasNext();
//            if (overlapping) {
//                log.error("setPosRaw 后出现重叠: pos={}, bb={}", self.position(), bb);
//            }
//        }
//    }
}
