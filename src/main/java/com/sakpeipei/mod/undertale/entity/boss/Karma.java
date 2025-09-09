package com.sakpeipei.mod.undertale.entity.boss;

import java.util.Map;

/**
 * @author Sakqiongzi
 * @since 2025-09-09 18:33
 * 业力
 */
public interface Karma {
    // 获取攻击实体到拥有KR攻击类型的映射，Map<实体UUID，攻击类型UUID>
    Map<String,String> getKarmaAttackType();
    // 获取攻击类型到第一次攻击增长的KR值的映射，Map<攻击类型UUID，KR值>
    Map<String,Byte> getKarmaAttackTypeValue();
}
