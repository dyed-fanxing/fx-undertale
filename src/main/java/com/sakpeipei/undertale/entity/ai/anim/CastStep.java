package com.sakpeipei.undertale.entity.ai.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;


public record CastStep(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> onHit, BiFunction<Integer,Integer,Boolean> canNext,Consumer<Integer> onNext,int duration, int cooldown, int timeout,
                       Consumer<LivingEntity> timeoutHandler) {
    
    public static final BiFunction<Integer,Integer,Boolean> DEFAULT_CAN_NEXT = (tick,d) -> tick>=d;
    public static final Consumer<Integer> DEFAULT_ON_NEXT = (step)->{};
    
    public CastStep(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown, int timeout, Consumer<LivingEntity> timeoutHandler){
        this(id,hitTicks,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,timeout,timeoutHandler);
    }
    public CastStep(Byte id, int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(id,hitTicks,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(int[] hitTicks, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(null,hitTicks,onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }





    public CastStep(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit,BiFunction<Integer,Integer,Boolean> next, int duration, int cooldown){
        this(id,new int[]{hitTick},onHit,next,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(int hitTick, ToIntFunction<LivingEntity> onHit,BiFunction<Integer,Integer,Boolean> next, int duration, int cooldown){
        this(null,new int[]{hitTick},onHit,next,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(id,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit, int duration){
        this(id,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,0,0,null);
    }
    public CastStep(Byte id, int hitTick, ToIntFunction<LivingEntity> onHit,Consumer<Integer> onNext, int duration){
        this(id,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,onNext,duration,0,0,null);
    }
    public CastStep(int hitTick, ToIntFunction<LivingEntity> onHit, int duration, int cooldown){
        this(null,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(int hitTick, ToIntFunction<LivingEntity> onHit,Consumer<Integer> onNext, int duration, int cooldown){
        this(null,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,cooldown,0,null);
    }
    public CastStep(int hitTick, ToIntFunction<LivingEntity> onHit,Consumer<Integer> onNext, int duration){
        this(null,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,0,0,null);
    }
    public CastStep(int hitTick, ToIntFunction<LivingEntity> onHit, int duration){
        this(null,new int[]{hitTick},onHit,DEFAULT_CAN_NEXT,DEFAULT_ON_NEXT,duration,0,0,null);
    }


    public static List<CastStep> create(int round,byte id,int hitTick, ToIntFunction<LivingEntity> onHit, int duration){
        List<CastStep>  steps = new ArrayList<>();
        steps.add(new CastStep(id,hitTick,onHit,duration));
        for (int i = 0; i < round-1; i++) {
            steps.add(new CastStep(null,hitTick,onHit,duration));
        }
        return steps;
    }

}
