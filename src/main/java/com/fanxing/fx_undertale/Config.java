package com.fanxing.fx_undertale;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class Config {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ModConfigSpec.IntValue segments;
        public Common(ModConfigSpec.Builder builder) {
            this.segments = builder
                    .comment("控制渲染圆的段数")
                    .translation(FxUndertale.MOD_ID+".segments")
                    .defineInRange("segments", 32, 4, 128);
        }
        public ModConfigSpec.IntValue getSegments() {
            return segments;
        }
    }
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 可在此添加配置加载后的处理（目前留空）
    }



    //#####################服务端配置#############################//
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }
    public static class Server {
        // ----- 通用设置 -----
        public final ModConfigSpec.IntValue expPerLv;
        public final ModConfigSpec.DoubleValue expCurveExponent;
        public final ModConfigSpec.IntValue maxLv;
        public final ModConfigSpec.ConfigValue<List<? extends String>> entityExpList;
        public final ModConfigSpec.IntValue namedKillExp;
        public final ModConfigSpec.IntValue namedKillDays;
        public final ModConfigSpec.IntValue tamedKillExp;
        public final ModConfigSpec.IntValue tamedKillDays;

        // ----- 属性点设置 -----
        public final ModConfigSpec.IntValue pointsPerLevel;
        public final ModConfigSpec.IntValue strengthPerPoint;
        public final ModConfigSpec.DoubleValue toughnessArmorPerPoint;
        public final ModConfigSpec.DoubleValue toughnessToughnessPerPoint;
        public final ModConfigSpec.IntValue healthPerPoint;

        // ----- 角色实体ID（用于解锁能力）-----
//        public final ModConfigSpec.ConfigValue<String> torielEntityId;
//        public final ModConfigSpec.ConfigValue<String> papyrusEntityId;
//        public final ModConfigSpec.ConfigValue<String> undyneEntityId;
//        public final ModConfigSpec.ConfigValue<String> alphysEntityId;
//        public final ModConfigSpec.ConfigValue<String> mettatonEntityId;
//        public final ModConfigSpec.ConfigValue<String> sansEntityId;
//        public final ModConfigSpec.ConfigValue<String> asgoreEntityId;


        // ----- 每个角色的配置（包括屠杀能力和平线预留）-----
        public final TorielConfig toriel;
        public final PapyrusConfig papyrus;
        public final UndyneConfig undyne;
        public final AlphysConfig alphys;
        public final MettatonConfig mettaton;
        public final SansConfig sans;
        public final AsgoreConfig asgore;
        // ----- 隐藏能力（Chara）-----
        public final ModConfigSpec.DoubleValue charaMaxHpPercent;
        public final ModConfigSpec.BooleanValue charaRemoveBuffs;

        private Map<String, EntityExpEntry> parsedEntityExp = null;

        public Server(ModConfigSpec.Builder builder) {
            // ----- 通用设置 -----
            expPerLv = builder
                    .comment("每级所需经验基数")
                    .translation(FxUndertale.MOD_ID+".configuration.expPerLv")  // 修改
                    .defineInRange("expPerLevel", 100, 1, 10000);
            expCurveExponent = builder
                    .comment("经验曲线指数（1.0=线性，1.5=平缓曲线）")
                    .translation(FxUndertale.MOD_ID+".configuration.expCurveExponent")  // 修改
                    .defineInRange("expCurveExponent", 1.5, 1.0, 3.0);
            maxLv = builder
                    .comment("最大LV")
                    .translation(FxUndertale.MOD_ID+".configuration.maxLv")  // 修改
                    .defineInRange("expPerLevel", 0, 0, 20);


            // ----- 实体经验列表 -----
            entityExpList = builder
                    .comment("实体经验配置列表，格式: 实体ID,经验值,击杀上限 (limit=-1表示无限)")
                    .translation(FxUndertale.MOD_ID+".configuration.entity_exp")  // 修改
                    .defineList("entityExp",
                            List::of,
                            () -> "minecraft:zombie,1,100",
                            s -> s instanceof String
                    );

            // ----- 特殊击杀奖励 -----
            builder.push("special_kills");
            builder.translation(FxUndertale.MOD_ID+".configuration.special_kills");  // 修改
            namedKillExp = builder
//                    .comment("杀死命名且存活足够天数的生物获得的额外经验")
                    .translation(FxUndertale.MOD_ID+".configuration.special_kills.namedKillExp")  // 修改
                    .defineInRange("namedKillExp", 100, 0, 10000);
            namedKillDays = builder
                    .comment("命名生物所需最小存活天数")
                    .translation(FxUndertale.MOD_ID+".configuration.special_kills.namedKillDays")  // 修改
                    .defineInRange("namedKillDays", 30, 1, 365);
            tamedKillExp = builder
                    .comment("杀死驯服且存活足够天数的生物获得的额外经验")
                    .translation(FxUndertale.MOD_ID+".configuration.special_kills.tamedKillExp")  // 修改
                    .defineInRange("tamedKillExp", 300, 0, 10000);
            tamedKillDays = builder
                    .comment("驯服生物所需最小存活天数")
                    .translation(FxUndertale.MOD_ID+".configuration.special_kills.tamedKillDays")  // 修改
                    .defineInRange("tamedKillDays", 30, 1, 365);
            builder.pop();

            // ----- 属性点设置 -----
            builder.push("attributes");
            builder.translation(FxUndertale.MOD_ID+".configuration.attributes");  // 修改
            pointsPerLevel = builder
                    .translation(FxUndertale.MOD_ID+".configuration.attributes.pointsPerLevel")  // 修改
                    .defineInRange("pointsPerLevel", 2, 1, 100);
            strengthPerPoint = builder
                    .translation(FxUndertale.MOD_ID+".configuration.attributes.strengthPerPoint")  // 修改
                    .defineInRange("strengthPerPoint", 1, 0, 100);
            toughnessArmorPerPoint = builder
                    .translation(FxUndertale.MOD_ID+".configuration.attributes.toughnessArmorPerPoint")  // 修改
                    .defineInRange("toughnessArmorPerPoint", 0.5, 0.0, 10.0);
            toughnessToughnessPerPoint = builder
                    .translation(FxUndertale.MOD_ID+".configuration.attributes.toughnessToughnessPerPoint")  // 修改
                    .defineInRange("toughnessToughnessPerPoint", 0.25, 0.0, 10.0);
            healthPerPoint = builder
                    .translation(FxUndertale.MOD_ID+".configuration.attributes.healthPerPoint")  // 修改
                    .defineInRange("healthPerPoint", 1, 0, 100);
            builder.pop();


//
//            // ----- 角色实体ID -----
//            builder.push("character_entities");
//            builder.translation(FxUndertale.MOD_ID+".configuration.character_entities");  // 修改
//            torielEntityId = builder
//                    .comment("Toriel 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.torielEntityId")  // 修改
//                    .define("torielEntityId", FxUndertale.MOD_ID+":toriel");
//            papyrusEntityId = builder
//                    .comment("Papyrus 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.papyrusEntityId")  // 修改
//                    .define("papyrusEntityId", FxUndertale.MOD_ID+":papyrus");
//            undyneEntityId = builder
//                    .comment("Undyne 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.undyneEntityId")  // 修改
//                    .define("undyneEntityId", FxUndertale.MOD_ID+":undyne");
//            alphysEntityId = builder
//                    .comment("Alphys 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.alphysEntityId")  // 修改
//                    .define("alphysEntityId", FxUndertale.MOD_ID+":alphys");
//            mettatonEntityId = builder
//                    .comment("Mettaton 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.mettatonEntityId")  // 修改
//                    .define("mettatonEntityId", FxUndertale.MOD_ID+":mettaton");
//            sansEntityId = builder
//                    .comment("Sans 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.sansEntityId")  // 修改
//                    .define("sansEntityId", FxUndertale.MOD_ID+":sans");
//            asgoreEntityId = builder
//                    .comment("Asgore 实体ID")
//                    .translation(FxUndertale.MOD_ID+".configuration.character_entities.asgoreEntityId")  // 修改
//                    .define("asgoreEntityId", FxUndertale.MOD_ID+":asgore");
//            builder.pop();

            // ----- 每个角色的配置 -----
            toriel = new TorielConfig(builder);
            papyrus = new PapyrusConfig(builder);
            undyne = new UndyneConfig(builder);
            alphys = new AlphysConfig(builder);
            mettaton = new MettatonConfig(builder);
            sans = new SansConfig(builder);
            asgore = new AsgoreConfig(builder);


            // ----- 隐藏能力（Chara）-----
            builder.push("chara_hidden");
            builder.translation(FxUndertale.MOD_ID+".configuration.chara_hidden");
            charaMaxHpPercent = builder
                    .comment("每次攻击削减目标最大生命值百分比（真实伤害）")
                    .translation(FxUndertale.MOD_ID+".configuration.chara_hidden.charaMaxHpPercent")  // 修改
                    .defineInRange("charaMaxHpPercent", 0.01, 0.0, 0.1);
            charaRemoveBuffs = builder
                    .comment("攻击是否移除目标所有正面效果")
                    .translation(FxUndertale.MOD_ID+".configuration.chara_hidden.charaRemoveBuffs")  // 修改
                    .define("charaRemoveBuffs", true);
            builder.pop();
        }

        // ----- 辅助方法：解析实体经验 -----
        public Map<String, EntityExpEntry> getEntityExp() {
            if (parsedEntityExp == null) {
                parsedEntityExp = new HashMap<>();
                for (String entry : entityExpList.get()) {
                    String[] parts = entry.split(",");
                    if (parts.length == 3) {
                        String id = parts[0].trim();
                        try {
                            int exp = Integer.parseInt(parts[1].trim());
                            int limit = Integer.parseInt(parts[2].trim());
                            parsedEntityExp.put(id, new EntityExpEntry(exp, limit));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            return parsedEntityExp;
        }
    }

    // ----- Toriel 配置 -----
    public static class TorielConfig {
        public final ModConfigSpec.DoubleValue damageThreshold;
        public final ModConfigSpec.DoubleValue damageStep;

        private TorielConfig(ModConfigSpec.Builder builder) {
            builder.push("toriel");
            builder.translation(FxUndertale.MOD_ID+".configuration.toriel");
            damageThreshold = builder
                    .translation(FxUndertale.MOD_ID+".configuration.toriel.damageThreshold")  // 统一前缀
                    .defineInRange("damageThreshold", 0.3, 0.0, 1.0);
            damageStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.toriel.damageStep")
                    .defineInRange("damageStep", 0.05, 0.0, 1.0);
            builder.pop();
        }
    }

    // Papyrus 配置
    public static class PapyrusConfig {
        public final ModConfigSpec.DoubleValue critDamageStep;

        private PapyrusConfig(ModConfigSpec.Builder builder) {
            builder.push("papyrus");
            critDamageStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.papyrus.critDamageStep")
                    .defineInRange("critDamageStep", 0.05, 0.0, 1.0);
            builder.pop();
        }
    }

    // Undyne 配置
    public static class UndyneConfig {
        public final ModConfigSpec.DoubleValue attackSpeedStep;

        private UndyneConfig(ModConfigSpec.Builder builder) {
            builder.push("undyne");
            attackSpeedStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.undyne.attackSpeedStep")
                    .defineInRange("attackSpeedStep", 0.18, 0.0, 1.0);
            builder.pop();
        }
    }

    // Alphys 配置
    public static class AlphysConfig {
        public final ModConfigSpec.DoubleValue damageReductionStep;
        public final ModConfigSpec.IntValue durationStep;

        private AlphysConfig(ModConfigSpec.Builder builder) {
            builder.push("alphys");
            damageReductionStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.alphys.damageReductionStep")
                    .defineInRange("damageReductionStep", 0.05, 0.0, 1.0);
            durationStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.alphys.durationStep")
                    .defineInRange("durationStep", 3, 1, 60);
            builder.pop();
        }
    }

    // Mettaton 配置
    public static class MettatonConfig {
        public final ModConfigSpec.DoubleValue damagePerEnemyStep;
        public final ModConfigSpec.IntValue rangeStep;

        private MettatonConfig(ModConfigSpec.Builder builder) {
            builder.push("mettaton");
            damagePerEnemyStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.mettaton.damagePerEnemyStep")
                    .defineInRange("damagePerEnemyStep", 0.01, 0.0, 1.0);
            rangeStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.mettaton.rangeStep")
                    .defineInRange("rangeStep", 4, 1, 64);
            builder.pop();
        }
    }

    // Sans 配置
    public static class SansConfig {
        public final ModConfigSpec.DoubleValue iFrameReductionStep;

        private SansConfig(ModConfigSpec.Builder builder) {
            builder.push("sans");
            iFrameReductionStep = builder
                    .translation(FxUndertale.MOD_ID+".configuration.sans.frameReductionStep")
                    .defineInRange("frameReductionStep", 0.1, 0.0, 1.0);
            builder.pop();
        }
    }

    // Asgore 配置
    public static class AsgoreConfig {
        public final ModConfigSpec.DoubleValue armorPierceStep;
        public final ModConfigSpec.DoubleValue shieldBreakStep;

        private AsgoreConfig(ModConfigSpec.Builder builder) {
            builder.push("asgore");
            armorPierceStep = builder
                    .comment("每级无视护甲增加比例")
                    .translation(FxUndertale.MOD_ID+".configuration.asgore.armorPierceStep")
                    .defineInRange("armorPierceStep", 0.05, 0.0, 1.0);
            shieldBreakStep = builder
                    .comment("每级破盾概率增加")
                    .translation(FxUndertale.MOD_ID+".configuration.asgore.shieldBreakStep")
                    .defineInRange("shieldBreakStep", 0.2, 0.0, 1.0);
            builder.pop();
        }
    }

    public record EntityExpEntry(int exp, int limit) {}
}