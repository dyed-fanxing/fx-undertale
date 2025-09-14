package com.sakpeipei.mod.undertale.entity.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Sakqiongzi
 * @since 2025-09-14 10:51
 * KR攻击数据，附着在拥有KR攻击的攻击物上
 */
public class KaramAttackData {

    public static final Codec<KaramAttackData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("attacks").forGetter(KaramAttackData::getUUID),
            Codec.BYTE.fieldOf("value").forGetter(KaramAttackData::getValue),
            Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).optionalFieldOf("attackedEntities",new HashSet<>()).forGetter(KaramAttackData::getAttackedEntities)
    ).apply(instance, KaramAttackData::new));

    private String uuid; // 招式类型，判断重复
    private byte value;  // 首次造成的KR值
    private HashSet<String> attackedEntities;   //攻击过的实体，用于当自身消亡时，将攻击过的实体的判重删除

    public KaramAttackData() {
    }
    public KaramAttackData(String uuid, byte value) {
        this(uuid,value,new HashSet<>());
    }
    public KaramAttackData(String uuid, byte value,HashSet<String> attackedEntities) {
        this.uuid = uuid;
        this.value = value;
        this.attackedEntities = attackedEntities;
    }
    public String getUUID() {
        return uuid;
    }
    public byte getValue() {
        return value;
    }
    public HashSet<String> getAttackedEntities() {
        return attackedEntities;
    }
}
