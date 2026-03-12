package com.fanxing.fx_undertale.entity.attachment;

import com.fanxing.fx_undertale.FxUndertale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.fanxing.fx_undertale.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerSoul {

    public static final Codec<PlayerSoul> PLAYER_SOUL_DATA = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("determination").forGetter(PlayerSoul::getDetermination),
                    Codec.INT.fieldOf("totalExp").forGetter(PlayerSoul::getTotalExp),
                    Codec.INT.fieldOf("level").forGetter(PlayerSoul::getLevel),

                    Codec.INT.fieldOf("availablePoints").forGetter(PlayerSoul::getAvailablePoints),
                    Codec.INT.fieldOf("strength").forGetter(PlayerSoul::getStrength),
                    Codec.INT.fieldOf("toughness").forGetter(PlayerSoul::getToughness),
                    Codec.INT.fieldOf("health").forGetter(PlayerSoul::getHealth),

                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("entityKillCounts")
                            .forGetter(s -> new HashMap<>(s.getKillCounts())),
                    Codec.INT.listOf()
                            .xmap(
                                    list -> list.stream().mapToInt(i -> i).toArray(),
                                    arr -> java.util.Arrays.stream(arr).boxed().toList()
                            )
                            .fieldOf("abilityLevels")
                            .forGetter(PlayerSoul::getAbilityLevels)
            ).apply(instance, PlayerSoul::new)
    );
    private int determination;
    private int totalExp;
    private int level;                  // LV等级，初始1
    private int availablePoints;         // 可用属性点
    private int strength;                // 力量点数
    private int toughness;               // 坚韧点数
    private int health;                  // 生命点数
    private final Map<String, Integer> entityKillCounts; // 实体ID -> 击杀次数
    private final int[] abilityLevels;    // 7个角色的能力等级，索引0=Toriel,1=Papyrus,2=Undyne,3=Alphys,4=Mettaton,5=Sans,6=Asgore

    // 默认构造
    public PlayerSoul() {
        this(100, 0, 0, 0, 0, 0, 0, new HashMap<>(), new int[7]);
    }

    // 全参构造
    public PlayerSoul(int determination, int totalExp, int level, int availablePoints,
                      int strength, int toughness, int health,
                      Map<String, Integer> entityKillCounts,
                      int[] abilityLevels) {
        this.determination = determination;
        this.totalExp = totalExp;
        this.level = level;
        this.availablePoints = availablePoints;
        this.strength = strength;
        this.toughness = toughness;
        this.health = health;
        this.entityKillCounts = entityKillCounts;
        this.abilityLevels = abilityLevels.length == 7 ? abilityLevels : new int[7];
    }

    // ----- 经验与等级 -----
    public void addExp(int amount) {
        this.totalExp += amount;
        int newLevel = calculateLevel();
        if (newLevel > this.level) {
            int gained = (newLevel - this.level) * Config.SERVER.pointsPerLevel.get();
            this.availablePoints += gained;
            this.level = newLevel;
        }
    }

    private int calculateLevel() {
        int base = Config.SERVER.expPerLv.get();
        double expo = Config.SERVER.expCurveExponent.get();
        int max = Config.SERVER.maxLv.get();
        int lv = 1;
        long cumulative = 0;
        while (true) {
            long required = (long) (base * Math.pow(lv, expo));
            if (cumulative + required > this.totalExp) break;
            cumulative += required;
            lv++;
            if (max > 0 && lv > max) break;
        }
        return Math.min(lv, max > 0 ? max : lv);
    }

    // ----- 击杀计数 -----
    public int getKillCount(String entityId) {
        return entityKillCounts.getOrDefault(entityId, 0);
    }

    public void incrementKillCount(String entityId) {
        entityKillCounts.put(entityId, getKillCount(entityId) + 1);
    }

    // ----- 能力等级管理 -----
    public int getAbilityLevel(int index) {
        if (index < 0 || index >= 7) return 0;
        return abilityLevels[index];
    }

    public void increaseAbilityLevel(int index) {
        if (index >= 0 && index < 7 && abilityLevels[index] < 5) {
            abilityLevels[index]++;
        }
    }

    public boolean isAbilityMaxed(int index) {
        return abilityLevels[index] >= 5;
    }

    // ----- 属性加点（返回成功与否）-----
    public boolean addStrength(int amount) {
        if (this.availablePoints < amount) return false;
        this.strength += amount;
        this.availablePoints -= amount;
        return true;
    }

    public boolean addToughness(int amount) {
        if (this.availablePoints < amount) return false;
        this.toughness += amount;
        this.availablePoints -= amount;
        return true;
    }

    public boolean addHealth(int amount) {
        if (this.availablePoints < amount) return false;
        this.health += amount;
        this.availablePoints -= amount;
        return true;
    }

    private static final ResourceLocation STRENGTH_ID = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"strength");
    private static final ResourceLocation TOUGHNESS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"toughness_armor");
    private static final ResourceLocation TOUGHNESS_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"toughness_toughness");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"health");

    public void applyAttributes(Player player) {
        if (player.level().isClientSide) return;

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance armorToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (attack == null || armor == null || armorToughness == null || maxHealth == null) return;

        // 移除旧修饰符
        attack.removeModifier(STRENGTH_ID);
        armor.removeModifier(TOUGHNESS_ARMOR_ID);
        armorToughness.removeModifier(TOUGHNESS_TOUGHNESS_ID);
        maxHealth.removeModifier(HEALTH_ID);

        // 计算加成
        double strengthBonus = this.strength * Config.SERVER.strengthPerPoint.get() / 100.0;
        double armorBonus = this.toughness * Config.SERVER.toughnessArmorPerPoint.get();
        double toughnessBonus = this.toughness * Config.SERVER.toughnessToughnessPerPoint.get();
        double healthBonus = this.health * Config.SERVER.healthPerPoint.get();

        // 添加新修饰符
        if (strengthBonus != 0) {
            attack.addTransientModifier(new AttributeModifier(STRENGTH_ID, strengthBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (armorBonus != 0) {
            armor.addTransientModifier(new AttributeModifier(TOUGHNESS_ARMOR_ID, armorBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (toughnessBonus != 0) {
            armorToughness.addTransientModifier(new AttributeModifier(TOUGHNESS_TOUGHNESS_ID, toughnessBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (healthBonus != 0) {
            double oldMax = player.getMaxHealth();
            maxHealth.addTransientModifier(new AttributeModifier(HEALTH_ID, healthBonus, AttributeModifier.Operation.ADD_VALUE));
            // 调整当前生命比例
            double newMax = player.getMaxHealth();
            if (newMax > 0 && oldMax > 0) {
                double newHealth = player.getHealth() * (newMax / oldMax);
                player.setHealth((float) newHealth);
            }
        }
    }

    // ----- Getters & Setters -----
    public int getDetermination() { return determination; }
    public int getTotalExp() { return totalExp; }
    public int getLevel() { return level; }
    public int getAvailablePoints() { return availablePoints; }
    public int getStrength() { return strength; }
    public int getToughness() { return toughness; }
    public int getHealth() { return health; }
    public Map<String, Integer> getKillCounts() { return entityKillCounts; }
    public int[] getAbilityLevels() { return abilityLevels; }

    public void addDetermination(int amount) { this.determination += amount; }
    public void spendDetermination(int amount) { this.determination = Math.max(0, this.determination - amount); }
}