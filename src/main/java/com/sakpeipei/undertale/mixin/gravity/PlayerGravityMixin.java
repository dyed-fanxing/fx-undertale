package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
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

    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    protected void canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        Vec3 position = self.position();
        EntityDimensions dimensions = self.getDimensions(pose);
        cir.setReturnValue(switch (data.getGravity()) {
            case DOWN -> null;
            case UP -> self.level().noCollision(self, dimensions.makeBoundingBox(position.x, position.y - dimensions.height(), position.z).deflate(1.0E-7));
            case NORTH -> null;
            case SOUTH -> null;
            case WEST -> null;
            case EAST -> null;
        });
    }
    @Inject(method = "canFallAtLeast", at = @At("HEAD"), cancellable = true)
    protected void canFallAtLeast(double dx, double dz, float maxUpStep, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) (this);
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return;
        cir.cancel();
        Vec3 worldDD = CoordsUtils.transform(dx, -maxUpStep-1.0E-5F, dz, data.getLogicToWorld());
        AABB aabb = self.getBoundingBox();
        cir.setReturnValue(switch (data.getGravity()) {
            case DOWN -> false;
            case UP -> self.level().noCollision(self, new AABB(aabb.minX + worldDD.x, aabb.maxY + worldDD.y, aabb.minZ + worldDD.z, aabb.maxX + worldDD.x, aabb.maxY, aabb.maxZ + worldDD.z));
            case NORTH -> null;
            case SOUTH -> null;
            case WEST -> null;
            case EAST -> null;
        });
    }
}
