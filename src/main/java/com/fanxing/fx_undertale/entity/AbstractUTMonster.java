package com.fanxing.fx_undertale.entity;

import com.fanxing.fx_undertale.registry.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;


public abstract class AbstractUTMonster extends Monster {
    public AbstractUTMonster(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.level().isClientSide){
            if(this.isDeadOrDying()){
                float progress = (float) this.deathTime / getDeathTime(); // 死亡时间
                spawnDeathParticles(this,progress);
            }
        }
    }
    /**
     * 生成死亡消散粒子
     *
     * @param animatable  实体
     * @param progress    消散进度 0~1
     */
    private void spawnDeathParticles(AbstractUTMonster animatable, float progress) {
        // 控制生成频率，避免每帧生成过多（比如每2帧生成一次）
        Level world = animatable.level();

        RandomSource random = animatable.getRandom();
        float bbWidth = animatable.getBbWidth();
        // 粒子数量随进度增加（例如 0~20 个/帧），但限制最高数量避免卡顿
        int count = Mth.ceil(bbWidth / 0.333334F);
        if (count <= 0) return;
        // 模型包围盒信息
        double x = animatable.getX();
        double z = animatable.getZ();
        double halfWidth = bbWidth / 2.0;
        double height = animatable.getBbHeight();
        // 根据进度确定有效生成区域：只从已经透明的部分生成粒子（顶部区域）
        // 透明区域是从顶部开始，即 Y 坐标大于 (y + height * (1 - progress)) 的部分
        double y = animatable.getY() + height * (1.0 - progress);
        for (int i = 0; i < count; i++) {
            // 在透明区域内随机生成位置
            double px = x + (random.nextDouble() - 0.5) * 2 * halfWidth;
            double pz = z + (random.nextDouble() - 0.5) * 2 * halfWidth;
            // 粒子速度：轻微随机，以向上为主
            double vx = (random.nextDouble() - 0.5) * 0.1;
            double vy = 0.03f; // 向上速度
            double vz = (random.nextDouble() - 0.5) * 0.1;
            // 可以选择多种粒子类型，这里先用默认的烟雾
            world.addParticle(ParticleTypes.CUSTOM_WHITE_ASH.get(), px, y, pz, vx, vy, vz);
        }
    }
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= getDeathTime() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }

    public int getDeathTime(){
        return 20;
    }
}