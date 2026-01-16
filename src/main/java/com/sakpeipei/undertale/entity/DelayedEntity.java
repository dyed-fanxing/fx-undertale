package com.sakpeipei.undertale.entity;

import net.minecraft.world.entity.Entity;

/**
 * @author yujinbao
 * @since 2026/1/16 15:08
 */
public class DelayedEntity {
    private final Entity entity;
    private int delay;

    public DelayedEntity(Entity entity, int delay) {
        this.entity = entity;
        this.delay = delay;
    }
    public boolean tick(){
        if(delay-- == 0){
            entity.level().addFreshEntity(entity);
            return true;
        }
        return false;
    }

    public Entity getEntity() {
        return entity;
    }
}

