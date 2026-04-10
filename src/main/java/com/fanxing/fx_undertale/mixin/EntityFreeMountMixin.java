package com.fanxing.fx_undertale.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class EntityFreeMountMixin {

    @Shadow
    @Nullable
    private Entity vehicle;

//    @Unique
//    public boolean fx_undertale$mount(Entity vehicle) {
//        Entity self = (Entity) (Object) this;
//        if (self == this.vehicle) {
//            return false;
//        } else {
//            for (Entity entity = vehicle; entity.getVehicle() != null; entity = entity.getVehicle()) {
//                if (entity.getVehicle() == self) {
//                    return false;
//                }
//            }
//            this.vehicle = vehicle;
//            return true;
//        }
//    }
//
//    @Inject(method = "rideTick",at = @At("HEAD"))
//    public void rideTick(CallbackInfo ci) {
//        Entity self = (Entity) (Object) this;
//        if (!EventHooks.fireEntityTickPre(self).isCanceled()) {
//            self.tick();
//            EventHooks.fireEntityTickPost(self);
//        }
//        if (self.isPassenger()) {
//            self.getVehicle().positionRider(self);
//        }
//
//    }
}
