package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MoveControl.class)
public abstract class MoveControlGravityMixin {
//    @Shadow
//    @Final
//    protected Mob mob;
//    @Shadow protected double wantedX;
//    @Shadow protected double wantedY;
//    @Shadow protected double wantedZ;
//
//    @Unique
//    private Vec3 undertale$localDeltaMovement;
//
//    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ai/control/MoveControl;operation:Lnet/minecraft/world/entity/ai/control/MoveControl$Operation;", ordinal = 1, shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
//    private void tick(CallbackInfo ci) {
//        GravityData gravityData = mob.getData(AttachmentTypes.GRAVITY);
//        if (gravityData.getGravity() != Direction.DOWN){
//            undertale$localDeltaMovement = gravityData.worldToLocal(wantedX-mob.getX(), wantedY-mob.getY(), wantedZ-mob.getZ());
//        }
//    }
//
//    @ModifyVariable(method = "tick", at = @At(value = "STORE"),  ordinal = 0)
//    private double d0_dx(double d0) {
//        return undertale$localDeltaMovement != null ? undertale$localDeltaMovement.x : d0;
//    }
//
//    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 1)
//    private double d1_dz(double d1) {
//        return undertale$localDeltaMovement != null ? undertale$localDeltaMovement.z : d1;
//    }
//
//    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 2)
//    private double d2_dy(double d2) {
//        return undertale$localDeltaMovement != null ? undertale$localDeltaMovement.y : d2;
//    }
//
//
//    @Inject(method = "setWantedPosition", at = @At("HEAD"))
//    private void onSetWanted(double x, double y, double z, double speed, CallbackInfo ci) {
//        if(mob instanceof IronGolem){
//            System.out.println("setWantedPosition called: " + x + ", " + y + ", " + z);
//        }
//    }
//
//    @Inject(method = "setWantedPosition", at = @At("RETURN"))
//    private void onSetWantedReturn(double x, double y, double z, double speed, CallbackInfo ci) {
//        if(mob instanceof IronGolem){
//            System.out.println("setWantedPosition called: " + x + ", " + y + ", " + z);
//        }
//    }
}
