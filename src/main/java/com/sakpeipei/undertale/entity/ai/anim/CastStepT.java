package com.sakpeipei.undertale.entity.ai.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;


public record CastStepT(Byte id, Predicate<Integer> canHit, ToIntFunction<LivingEntity> onHit, CanNext canNext, Consumer<Integer> onNext, int duration, int cooldown, int timeout,
                        Consumer<LivingEntity> timeoutHandler) {

    public static final CanNext DEFAULT_CAN_NEXT = (tick,hitTick,duration) -> tick>=duration;
    public static final Consumer<Integer> DEFAULT_ON_NEXT = (step)->{};

    public CastStepT(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown, int timeout, Consumer<LivingEntity> timeoutHandler){
        this(id,(t)->{
            for (int hitTick : hitTicks) {
                if(hitTick == t) return true;
            }
            return false;
        },onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,timeout,timeoutHandler);
    }
    public CastStepT(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(id,(t)->{
            for (int hitTick : hitTicks) {
                if(hitTick == t) return true;
            }
            return false;
        },onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStepT(int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(null,(t)->{
            for (int hitTick : hitTicks) {
                if(hitTick == t) return true;
            }
            return false;
        },onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }




    // 单次
    public CastStepT(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, CanNext next, int duration, int cooldown){
        this(id,(t)-> t == hitTick,onHit,next,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStepT(int hitTick, ToIntFunction<LivingEntity> onHit, CanNext next, int duration, int cooldown){
        this(null,(t)-> t == hitTick,onHit,next,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStepT(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(id,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStepT(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, int duration){
        this(id,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,0,0,null);
    }
    public CastStepT(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, Consumer<Integer> onNext, int duration){
        this(id,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,onNext,duration,0,0,null);
    }
    public CastStepT(int hitTick, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(null,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStepT(int hitTick, ToIntFunction<LivingEntity> onHit, Consumer<Integer> onNext, int duration, int cooldown){
        this(null,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,onNext,duration,cooldown,0,null);
    }
    public CastStepT(int hitTick, ToIntFunction<LivingEntity> onHit, Consumer<Integer> onNext, int duration){
        this(null,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,onNext,duration,0,0,null);
    }
    public CastStepT(int hitTick, ToIntFunction<LivingEntity> onHit, int duration){
        this(null,(t)-> t == hitTick,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,0,0,null);
    }


    public static List<CastStepT> create(int round, byte id, int hitTick, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        List<CastStepT>  steps = new ArrayList<>();
        steps.add(new CastStepT(id,hitTick,onHit,duration));
        for (int i = 0; i < round-1; i++) {
            steps.add(new CastStepT(null,hitTick,onHit,duration,cooldown));
        }
        return steps;
    }

}
