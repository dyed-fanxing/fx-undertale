//package com.sakpeipei.mod.undertale.entity;
//
//import com.sakpeipei.mod.undertale.particle.options.BallGrowOptions;
//import com.sakpeipei.mod.undertale.registry.SoundRegistry;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.entity.*;
//import net.minecraft.world.level.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.jetbrains.annotations.NotNull;
//import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//import software.bernie.geckolib.animation.AnimatableManager;
//import software.bernie.geckolib.animation.AnimationController;
//import software.bernie.geckolib.animation.PlayState;
//import software.bernie.geckolib.animation.RawAnimation;
//import software.bernie.geckolib.util.GeckoLibUtil;
//
///**
// * 可变动画时间GB，可调整蓄力时间，射击时间
// */
//public class GasterBlasterVarEntity extends IGasterBlaster {
//
//    private static final Logger log = LogManager.getLogger(GasterBlasterVarEntity.class);
//    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
//    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
//    private static final RawAnimation SHOT_ANIM = RawAnimation.begin().thenPlay("shot");
//    private static final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
//
//    public static final int PHASE_IDLE = 0;    // 闲置阶段
//    public static final int PHASE_CHARGE = 1;   // 蓄力阶段（默认）
//    public static final int PHASE_SHOT = 2;     // 发射阶段
//    public static final int PHASE_DECAY = 3;     // 消退阶段
//    /*
//       由于服务端与客户端有1~3tick延迟，所以阶段必须默认设置成0，不能是1，否则会因为默认为1，可能导致客户端先渲染，在与服务端
//       存在延迟的情况下，客户端蓄力动画已经完成，服务端还没有切换到发射阶段就又会触发蓄力阶段，所以必须由服务端触发蓄力阶段，
//       使客户端比服务端延迟，这样可以保证逻辑不错误，但可能会导致视觉延迟
//    */
//    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GasterBlasterVarEntity.class, EntityDataSerializers.INT);
//    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(GasterBlasterVarEntity.class, EntityDataSerializers.INT);
//    private static final EntityDataAccessor<Integer> SHOT = SynchedEntityData.defineId(GasterBlasterVarEntity.class, EntityDataSerializers.INT);
//
//    // 射线当前长度
//    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GasterBlasterVarEntity.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(GasterBlasterVarEntity.class, EntityDataSerializers.FLOAT);
//    private int total; // 记录 charge+shot的 总 TICK 点，防止tick中一直计算
//    public boolean isGrow;
//
//    public GasterBlasterVarEntity(EntityType<? extends Entity> type, Level level, LivingEntity owner) {
//        this(type, level,owner,16,40);
//    }
//    public GasterBlasterVarEntity(EntityType<? extends Entity> type, Level level, LivingEntity owner, int charge, int shot) {
//        super(type, level,owner);
//        super.entityData.set(CHARGE,charge);
//        super.entityData.set(SHOT,shot);
//        this.total = charge + shot ;
//    }
//
//
//    @Override
//    public void tick(){
//        super.tick();
//        //只在服务端执行攻击逻辑
//        if(!this.level().isClientSide){
//            if(super.tickCount <= getCharge()) {
//                if(getPhase() == PHASE_IDLE) this.entityData.set(PHASE,PHASE_CHARGE);
//                return;
//            }
//            if(super.tickCount > total) {
//                if(getPhase() == PHASE_SHOT) this.entityData.set(PHASE,PHASE_DECAY);
//                else super.discard();
//                return;
//            }
//            if(getPhase() == PHASE_CHARGE) this.entityData.set(PHASE,PHASE_SHOT);
//            checkHits();
//        }
//    }
//
//    @Override
//    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(this, "controller",  state -> {
//            AnimationController<GasterBlasterVarEntity> controller = state.getController();
//            if(controller.getAnimationState() == AnimationController.State.STOPPED){
//                switch (getPhase()){
//                    case PHASE_CHARGE -> {
//                        controller.setAnimation(CHARGE_ANIM);
//                        controller.setAnimationSpeed(20.f/getCharge());
//                        level().addParticle(new BallGrowOptions(getWidth(), (short) getCharge()),false,this.getX(),this.getY() + 0.3,this.getZ(),0,0,0);
//                        level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_CHARGE.get(), SoundSource.NEUTRAL,1,1);
//                    }
//                    case PHASE_SHOT -> {
//                        state.getController().setAnimation(SHOT_ANIM);
//                        controller.setAnimationSpeed(20.f/getShot());
//                        level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_SHOT.get(), SoundSource.NEUTRAL,1,1);
//                    }
//                    case PHASE_DECAY -> {
//                        controller.setAnimation(DECAY_ANIM);
//                        controller.setAnimationSpeed(20.f);
//                    }
//                }
//            }
//            return PlayState.CONTINUE;
//        }));
//    }
//
//    /**
//     * 初始化同步数据
//     */
//    @Override
//    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
//        builder.define(LENGTH, 16f);
//        builder.define(WIDTH, 1.0f);
//        builder.define(PHASE, PHASE_IDLE);
//        builder.define(CHARGE, 0);
//        builder.define(SHOT, 0);
//    }
//
//    @Override
//    protected void readAdditionalSaveData(CompoundTag p_20052_) {
//
//    }
//
//    @Override
//    protected void addAdditionalSaveData(CompoundTag p_20139_) {
//
//    }
//
//
//    @Override
//    public AnimatableInstanceCache getAnimatableInstanceCache() {
//        return this.cache;
//    }
//
//    // 在炮台实体类中添加：
//    @Override
//    public boolean isPushable() {
//        return false; // 完全不会被推动
//    }
//
//    // 客户端可访问方法
//    public float getLength() {
//        return super.entityData.get(LENGTH);
//    }
//    public float getWidth() {
//        return super.entityData.get(WIDTH);
//    }
//    public int getPhase() {
//        return super.entityData.get(PHASE);
//    }
//    public int getCharge() {
//        return super.entityData.get(CHARGE) ;
//    }
//    public int getShot() {
//        return super.entityData.get(SHOT) ;
//    }
//}
