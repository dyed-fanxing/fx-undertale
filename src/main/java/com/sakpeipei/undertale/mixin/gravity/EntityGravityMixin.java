package com.sakpeipei.undertale.mixin.gravity;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.IRollable;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
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


    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
        if(gravity != Direction.DOWN){
            ci.cancel();
            switch(gravity) {
                case UP -> {
                    // 对于非默认重力，使用不同的限制或取消限制
                    float pitch = (float)-pitchDelta * 0.15F;
                    float yaw = (float)-yawDelta * 0.15F;
                    self.setXRot(self.getXRot() + pitch);
                    self.setYRot(self.getYRot() + yaw);
                    self.setXRot(Mth.clamp(self.getXRot(), -90f, 90.0F));
                    self.xRotO += pitch;
                    self.yRotO += yaw;
                    self.xRotO = Mth.clamp(self.xRotO, -90.0F, 90.0F);
                }
                case SOUTH -> {
                    IRollable rollable = ((IRollable)self);
                    // 鼠标左右移动 → roll（绕Z轴）
                    float roll = (float)yawDelta * 0.15F;
                    float pitch = (float)pitchDelta * 0.15F;
                    rollable.undertale$setRoll(roll);
                    self.setXRot(Mth.clamp(self.getXRot() + pitch, -90f, 90.0F));

                    self.xRotO += pitch;
                    rollable.undertale$setRollO(rollable.undertale$getRollO() + roll);
                    self.xRotO = Mth.clamp(self.xRotO, -90.0F, 90.0F);
                }
                case NORTH -> {
                }
            }

            if (this.vehicle != null) {
                this.vehicle.onPassengerTurned(self);
            }
        }
    }
    /**
     * moveRelative 根据当前身体角度处理向前移动时的在X和Z轴上的速度增量
     * 根据重力方向修正yRot角度：由于速度存储的是局部，所以需要yRot由世界转局部
     */
    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    public float getInputVector(Entity instance) {
        GravityData data = instance.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();

        return switch (gravity) {
            case DOWN -> yRot;
            case UP -> -yRot;
            case NORTH -> 0.0F;
            case SOUTH -> ((IRollable)instance).undertale$getRoll();
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
//
//    @Inject(method = "lookAt", at = @At(value = "HEAD"), cancellable = true)
//    private void onLookAt(EntityAnchorArgument.Anchor anchor, Vec3 target, CallbackInfo ci) {
//        Entity self = (Entity)(Object)this;
//        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
//        Direction gravity = data.getGravity();
//        if(gravity != Direction.DOWN){
//            ci.cancel();
//            Vec3 eyePos = anchor.apply(self);
//            double dx = target.x - eyePos.x;
//            double dy = target.y - eyePos.y;
//            double dz = target.z - eyePos.z;
//            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
//            dx /= len; dy /= len; dz /= len;
//            float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
//            float pitch = (float) Math.toDegrees(Math.asin(-dy));
//            float roll = (float) Math.toDegrees(Math.atan2(-dx * dy, Math.sqrt(1 - dy * dy)));
//            // 设置角度
//            self.setYRot(yaw);
//            self.setXRot(pitch);
//            GravityAccessor self1 = (GravityAccessor) self;
//            self1.setRoll(roll);
//            switch (gravity){
//                case SOUTH -> self.setYHeadRot(roll);
//            }
//            self.xRotO = self.getXRot();
//            self.yRotO = self.getYRot();
//            self1.setRollO(self1.getRoll());
//        }
//    }
//
//    @Inject(method = "lerpHeadTo", at = @At("HEAD"), cancellable = true)
//    private void onLerpHeadTo(float yHeadRot, int steps, CallbackInfo ci) {
//        Entity self = (Entity)(Object)this;
//        GravityData gravity = self.getData(AttachmentTypeRegistry.GRAVITY);
//
//        if (gravity.getGravity() == Direction.SOUTH) {
//            // 网络同步来的头部旋转，直接设置给 roll
//            ((GravityAccessor)self).setRoll(yHeadRot);
//            ((GravityAccessor)self).setRollO(yHeadRot);
//            self.setYHeadRot(yHeadRot);  // 同步回去
//            ci.cancel();
//        }
//    }




    /**
     * 根据重力方向重新返回Dimensions
     */
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        cir.setReturnValue(switch (data.getGravity()) {
            case DOWN, UP -> self.getType().getDimensions();
            case EAST, WEST, SOUTH, NORTH -> self.getType().getDimensions().scale(1.0f, 0.5f);
        });
    }

    /**
     * 根据重力方向重新构建碰撞箱
     */
    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    protected void makeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        double halfWidth = this.dimensions.width() * 0.5f;
        cir.setReturnValue(switch (data.getGravity()) {
            case DOWN -> this.dimensions.makeBoundingBox(position.x, position.y, position.z);
            case UP -> this.dimensions.makeBoundingBox(position.x, position.y - self.getBbHeight(), position.z);
            case EAST -> this.dimensions.makeBoundingBox(position.x - halfWidth, position.y, position.z);
            case WEST -> this.dimensions.makeBoundingBox(position.x + halfWidth, position.y, position.z);
            case SOUTH -> new AABB(position.x-halfWidth, position.y-halfWidth, position.z, position.x+halfWidth, position.y+halfWidth, position.z - self.getBbHeight());
            case NORTH -> new AABB(position.x-halfWidth, position.y-halfWidth, position.z, position.x+halfWidth, position.y+halfWidth, position.z + self.getBbHeight());
        });
    }

    /**
     * 根据重力方向返回眼睛Y轴位置
     */
    @Inject(method = "getEyeY", at = @At(value = "HEAD"), cancellable = true)
    public void getEyeY(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        cir.setReturnValue(switch (data.getGravity()) {
            case UP -> this.position.y - eyeHeight;
            case DOWN -> this.position.y + eyeHeight;
            case EAST, WEST, SOUTH, NORTH -> this.position.y;
        });
    }

    /**
     * 根据重力方向返回眼睛位置
     */
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"), cancellable = true)
    public void getEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        cir.setReturnValue(switch (data.getGravity()) {
            case DOWN, UP -> new Vec3(self.getX(), self.getEyeY(), self.getZ());
            case EAST -> new Vec3(self.getX() - eyeHeight, self.getEyeY(), self.getZ());
            case WEST -> new Vec3(self.getX() + eyeHeight, self.getEyeY(), self.getZ());
            case SOUTH -> new Vec3(self.getX(), self.getEyeY(), self.getZ() - eyeHeight);
            case NORTH -> new Vec3(self.getX(), self.getEyeY(), self.getZ() + eyeHeight);
        });
    }

    /**
     * 根据重力方向返回眼睛位置
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"), cancellable = true)
    public void getEyePositionLerp(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        cir.setReturnValue(switch (data.getGravity()) {
            case UP -> new Vec3(
                    Mth.lerp(partialTick, self.xo, self.getX()),
                    Mth.lerp(partialTick, self.yo, self.getY()) - this.eyeHeight,
                    Mth.lerp(partialTick, self.zo, self.getZ())
            );
            case DOWN -> new Vec3(
                    Mth.lerp(partialTick, self.xo, self.getX()),
                    Mth.lerp(partialTick, self.yo, self.getY()) + this.eyeHeight,
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
        });
    }

    /**
     * 根据重力方向设置自身所在的方块位置blockPosition
     * 默认坐标系下为向下对齐，所以需要修正
     */
    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    public final void setPosRaw(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity == Direction.DOWN) return;
        ci.cancel();
        Vec3i normal = gravity.getNormal();
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            // blockPosition 是玩家下身所在的那个方块，由于方块是以默认世界坐标系的点来取的，所以需要进行调整
            int i = Mth.floor(x);
            int j = normal.getY() < 0 ? Mth.floor(y) : Mth.floor(y - 1.0E-6); // 以方块底部坐标代表这个方块
            int k = Mth.floor(z);
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
        GravityData gravityData = self.getData(AttachmentTypeRegistry.GRAVITY);
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
                    Vec3 worldExpand = CoordsUtils.transform(logicExpand, gravityData.getLogicToWorld());
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
        GravityData gravityData = self.getData(AttachmentTypeRegistry.GRAVITY);
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
                        normal.getX() == 0 ? blockpos.getX() : Mth.floor(offsetPos.x),
                        normal.getY() == 0 ? blockpos.getY() : Mth.floor(offsetPos.y),
                        normal.getZ() == 0 ? blockpos.getZ() : Mth.floor(offsetPos.z)
                ) : blockpos);
            }
        } else {
            Vec3 offsetPos = this.position.add(gravityVec3.scale(dy));
            int i = Mth.floor(offsetPos.x);
            int j = Mth.floor(offsetPos.y);
            int k = Mth.floor(offsetPos.z);
            cir.setReturnValue(new BlockPos(i, j, k));
        }
    }

    /**
     * 将局部速度转换为世界速度加到世界位置
     */
    @ModifyArgs(method = "move",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void setPosM(Args args) {
        Entity self = (Entity) (Object) this;
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity == Direction.DOWN) return;
        double posX = self.getX();
        double posY = self.getY();
        double posZ = self.getZ();
        // 减出增量，对增量进行转换
        double[] worldDD = CoordsUtils.transformArray((double) args.get(0) - posX, (double) args.get(1) - posY, (double) args.get(2) - posZ, data.getLogicToWorld());
        args.set(0, posX+worldDD[0]);
        args.set(1, posY+worldDD[1]);
        args.set(2, posZ+worldDD[2]);
    }

    /**
     * 根据重力方向重写碰撞检测
     */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void collide(Vec3 logicDD, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (((Object) this instanceof Player)) {
            GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY.get());
            Direction gravity = data.getGravity();
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
                worldDD = CoordsUtils.transform(logicDD, data.getWorldToLogic());
                // 1. 将位移转换为全局，用于碰撞检测判断
                list = self.level().getEntityCollisions(self, aabb.expandTowards(worldDD));
                // 检测出来的可以进行移动的世界位移
                collidedWorldDD = collideBoundingBox(self, worldDD, aabb, self.level(), list);
            }
            // 3. 将世界位移转换到局部位移系用于逻辑判断
            Vec3 collidedLogicDD = CoordsUtils.transform(collidedWorldDD, data.getWorldToLogic());
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
                    Vec3 shapeCollideLogicDD = CoordsUtils.transform(shapeCollideWorldDD, data.getWorldToLogic());
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
    @Shadow
    private static List<VoxelShape> collectColliders(@org.jetbrains.annotations.Nullable Entity p_344804_, Level p_345583_, List<VoxelShape> p_345198_, AABB p_345837_) {
        return null;
    }
    @Shadow
    private static Vec3 collideWithShapes(Vec3 p_198901_, AABB p_198902_, List<VoxelShape> p_198903_) {
        return null;
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


    @Inject(method = "absRotateTo", at = @At("HEAD"), cancellable = true)
    private void absRotateTo(float p_348662_, float p_348500_, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravity = self.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
        if(gravity != Direction.DOWN){
            ci.cancel();
            switch(gravity) {
            }
        }
    }


    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setYBodyRot(F)V",shift = At.Shift.AFTER))
    private void setHeadAndBodyRotInLoadAfterSetYBodyRot(CallbackInfo ci){
        Entity self = (Entity)(Object)this;
        Direction gravity = self.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
        if (gravity != Direction.DOWN) {
            IRollable rollable = (IRollable)self;
            switch (gravity) {
                case SOUTH -> {
                    self.setYHeadRot(rollable.undertale$getRoll());
                    self.setYBodyRot(rollable.undertale$getRoll());
                }
            }
        }
    }


    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    public void getViewVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity)(Object)this;
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if(gravity != Direction.DOWN){
            cir.cancel();
            IRollable rollable = (IRollable)self;
            cir.setReturnValue(undertale_calculateViewVector(rollable.undertale$getViewRoll(partialTicks),self.getViewXRot(partialTicks),self.getViewYRot(partialTicks)));
        }
    }


    @Inject(method = "getUpVector", at = @At("HEAD"), cancellable = true)
    public final void getUpVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity)(Object)this;
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if(gravity != Direction.DOWN){
            cir.cancel();
            IRollable rollable = (IRollable)self;
            cir.setReturnValue(undertale_calculateUpVector(rollable.undertale$getViewRoll(partialTicks),self.getViewXRot(partialTicks),self.getViewYRot(partialTicks)));
        }
    }
    @Unique
    protected Vec3 undertale_calculateUpVector(float roll,float pitch, float yaw) {
        return undertale_calculateViewVector(roll,pitch- 90f,yaw);
    }
    @Unique
    protected Vec3 undertale_calculateViewVector(float roll,float pitch, float yaw) {
        float rollRad = roll * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        float yawRad = -yaw * Mth.DEG_TO_RAD;

        float sinP = Mth.sin(pitchRad);
        float cosP = Mth.cos(pitchRad);
        float sinY = Mth.sin(yawRad);
        float cosY = Mth.cos(yawRad);
        float sinR = Mth.sin(rollRad);
        float cosR = Mth.cos(rollRad);

        float x = cosY * sinR * sinP + sinY * cosR;
        float y = -cosP * sinR;
        float z = cosY * cosR - sinY * sinR * sinP;
        return new Vec3(x, y, z);
    }
}
