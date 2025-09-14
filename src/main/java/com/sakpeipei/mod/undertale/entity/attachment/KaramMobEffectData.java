package com.sakpeipei.mod.undertale.entity.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.network.KaramPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:12
 */
public class KaramMobEffectData {
    public static final Codec<KaramMobEffectData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BYTE.fieldOf("value").forGetter(KaramMobEffectData::getValue),
            Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).fieldOf("attacks").forGetter(KaramMobEffectData::getAttacks)
    ).apply(instance, KaramMobEffectData::new));


    public static final byte MAX = 40;
    private byte value;
    private final HashSet<String> attacks;

    public KaramMobEffectData() {
        this.value = 0;
        this.attacks = new HashSet<>();
    }

    public KaramMobEffectData(byte value, HashSet<String> attacks) {
        this.value = value;
        this.attacks = attacks;
    }


    public byte getValue() {
        return value;
    }

    public void addValue(LivingEntity entity, int value) {
        if(this.value >= MAX) {
            return;
        }else if(this.value + value >= MAX) {
            this.value = MAX;
        }else {
            this.value += (byte) value;
        }
//        sendPacket(entity);
    }
    public void subValue(LivingEntity entity){
        if(this.value == 0) {
            return;
        }
        this.value--;
//        sendPacket(entity);
    }

    public void setValue(byte value) {
        this.value = value;
    }
    public HashSet<String> getAttacks() {
        return attacks;
    }

    private void sendPacket(LivingEntity entity){
        if(entity instanceof ServerPlayer player){
            PacketDistributor.sendToPlayer(player,new KaramPacket(entity.getId(),this.value));
        }else{
            PacketDistributor.sendToPlayersTrackingEntity(entity,new KaramPacket(entity.getId(),this.value));
        }
    }
}
