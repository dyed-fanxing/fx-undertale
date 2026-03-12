package com.fanxing.fx_undertale.utils;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * @author FanXing
 * @since 2025-11-27 20:01
 */
public class LevelUtils {
    public static boolean addFreshEntity(Level level, Entity entity,  Vec3 targetPos){
        entity.lookAt(EntityAnchorArgument.Anchor.FEET,targetPos);
        return level.addFreshEntity(entity);
    }


    public static boolean addFreshProjectile(Level level, Projectile projectile,Entity target){
        RotUtils.lookAtShoot(projectile, target);
        return level.addFreshEntity(projectile);
    }
    /**
     * 添加弹射物到当前世界中，并看向目标实体
     * @param level 世界
     * @param projectile 弹射物
     * @param spawnPos 出生点
     * @param target 目标实体
     */
    public static boolean addFreshProjectile(Level level, Projectile projectile, Vec3 spawnPos, Entity target){
        projectile.setPos(spawnPos);
        RotUtils.lookAtShoot(projectile, target);
        return level.addFreshEntity(projectile);
    }

    /**
     * 添加弹射物到当前世界中，并看向目标实体
     * @param level 世界
     * @param projectile 弹射物
     * @param x,y,z 出生点
     * @param target 目标实体
     */
    public static boolean addFreshProjectile(Level level, Projectile projectile, double x,double y,double z, Entity target){
        projectile.setPos(x,y,z);
        RotUtils.lookAtShoot(projectile, target);
        return level.addFreshEntity(projectile);
    }
    public static boolean addFreshProjectile(Level level, Projectile projectile,double x,double y,double z){
        RotUtils.lookAtShoot(projectile,x,y,z);
        return level.addFreshEntity(projectile);
    }
    /**
     * 添加弹射物到当前世界中，并看向目标位置
     * @param level 世界
     * @param projectile 弹射物
     * @param spawnPos 出生点
     * @param x,y,z 目标位置
     */
    public static boolean addFreshProjectile(Level level, Projectile projectile, Vec3 spawnPos, double x,double y,double z){
        projectile.setPos(spawnPos);
        RotUtils.lookAtShoot(projectile,x,y,z);
        return level.addFreshEntity(projectile);
    }
    /**
     * 添加弹射物到当前世界中，并看向目标位置
     * @param level 世界
     * @param projectile 弹射物
     * @param spawnX,spawnY,spawnZ 出生点
     * @param targetX,targetY,targetZ 目标位置
     */
    public static boolean addFreshProjectile(Level level, Projectile projectile, double spawnX,double spawnY,double spawnZ, double targetX,double targetY,double targetZ){
        projectile.setPos(spawnX,spawnY,spawnZ);
        RotUtils.lookAtShoot(projectile,targetX,targetY,targetZ);
        return level.addFreshEntity(projectile);
    }
    /**
     * 添加弹射物到当前世界中，并看向目标位置
     * @param level 世界
     * @param projectile 弹射物
     * @param spawnPos 出生点
     * @param targetPos 目标位置
     */
    public static boolean addFreshProjectile(Level level, Projectile projectile, Vec3 spawnPos, Vec3 targetPos){
        projectile.setPos(spawnPos);
        RotUtils.lookAtShoot(projectile, targetPos);
        return level.addFreshEntity(projectile);
    }



    /**
     * 添加弹射物到当前世界中，并看向矢量方向
     * @param level 世界
     * @param projectile 弹射物
     * @param x,y,z 出生点
     * @param moveVector 移动向量
     */
    public static boolean addFreshProjectileByVec3(Level level, Projectile projectile, double x,double y,double z, Vec3 moveVector){
        projectile.setPos(x,y,z);
        RotUtils.lookVecShoot(projectile, moveVector);
        return level.addFreshEntity(projectile);
    }
    /**
     * 添加弹射物到当前世界中，并看向矢量方向
     * @param level 世界
     * @param projectile 弹射物
     * @param spawnPos 出生点
     * @param moveVector 移动向量
     */
    public static boolean addFreshProjectileByVec3(Level level, Projectile projectile, Vec3 spawnPos, Vec3 moveVector){
        projectile.setPos(spawnPos);
        RotUtils.lookVecShoot(projectile, moveVector);
        return level.addFreshEntity(projectile);
    }
}
