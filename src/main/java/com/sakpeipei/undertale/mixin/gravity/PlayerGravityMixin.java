package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerGravityMixin {

    /**
     * 玩家能否以当前姿势Pose适应所在方块与实体
     * 根据重力方向重写需要进行碰撞检测的碰撞箱
     */
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    protected void canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) (this);
        Direction gravity = self.getData(AttachmentTypes.GRAVITY).getGravity();
        if (gravity != Direction.DOWN) {
            cir.cancel();
            Vec3 position = self.position();
            EntityDimensions dimensions = self.getDimensions(pose);
            double halfWidth = dimensions.width()*0.5;
            cir.setReturnValue(switch (gravity) {
                case UP -> self.level().noCollision(self, dimensions.makeBoundingBox(position.x, position.y - dimensions.height(), position.z).deflate(1.0E-7));

                case EAST -> self.level().noCollision(self,new AABB(position.x - dimensions.height(), position.y-halfWidth, position.z-halfWidth, position.x, position.y+halfWidth, position.z+halfWidth).deflate(1.0E-7));
                case WEST -> self.level().noCollision(self,new AABB(position.x, position.y-halfWidth, position.z-halfWidth, position.x + dimensions.height(), position.y+halfWidth, position.z+halfWidth).deflate(1.0E-7));

                case SOUTH -> self.level().noCollision(self,new AABB(position.x-halfWidth, position.y-halfWidth, position.z-dimensions.height(), position.x+halfWidth, position.y+halfWidth, position.z).deflate(1.0E-7));
                case NORTH -> self.level().noCollision(self,new AABB(position.x-halfWidth, position.y-halfWidth, position.z, position.x+halfWidth, position.y+halfWidth, position.z + dimensions.height()).deflate(1.0E-7));
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
    }


    /**
     * 对于局部dx和dz位移以及高度大于maxUpStep的方块，会不会掉落下去，用于玩家shift在方块边缘时，高度大于maxUpStep的方块会不会掉落
     * 根据重力方向重写需要进行碰撞检测的碰撞箱
     */
    @Inject(method = "canFallAtLeast", at = @At("HEAD"), cancellable = true)
    protected void canFallAtLeast(double dx, double dz, float maxUpStep, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) (this);
        GravityData data = self.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity != Direction.DOWN){
            cir.cancel();
            AABB aabb = self.getBoundingBox();
            cir.setReturnValue(switch (gravity) {
                case UP -> self.level().noCollision(self, new AABB(aabb.minX + dx, aabb.minY, aabb.minZ + dz, aabb.maxX + dx, aabb.maxY + maxUpStep+1.0E-5F, aabb.maxZ + dz));

                case EAST -> self.level().noCollision(self,new AABB(aabb.minX, aabb.minY+dz, aabb.minZ-dx, aabb.maxX + maxUpStep+1.0E-5F, aabb.maxY+dz, aabb.maxZ-dx));
                case WEST -> self.level().noCollision(self,new AABB(aabb.minX - maxUpStep-1.0E-5F, aabb.minY+dz, aabb.minZ+dx, aabb.maxX, aabb.maxY+dz, aabb.maxZ+dx));

                case SOUTH -> self.level().noCollision(self,new AABB(aabb.minX + dx, aabb.minY+dz, aabb.minZ, aabb.maxX + dx, aabb.maxY+dz, aabb.maxZ + maxUpStep+1.0E-5F));
                case NORTH -> self.level().noCollision(self,new AABB(aabb.minX - dx, aabb.minY+dz, aabb.minZ - maxUpStep-1.0E-5F, aabb.maxX - dx, aabb.maxY+dz, aabb.maxZ));
                default -> throw new IllegalStateException("Unexpected value: " + gravity);
            });
        }
   }
}
