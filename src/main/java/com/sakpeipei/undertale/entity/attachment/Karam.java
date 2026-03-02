package com.sakpeipei.undertale.entity.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sakpeipei.undertale.net.packet.KaramPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:12
 * KR 数据，附着在被KR攻击物攻击的实体身上
 */
public class Karam {
    public static final Codec<Karam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BYTE.fieldOf("value").forGetter(Karam::getValue),
            Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).fieldOf("attacks").forGetter(Karam::getAttacks)
    ).apply(instance, Karam::new));


    public static final byte MAX = 40;
    private byte value;
    private final HashSet<String> attacks;

    public Karam() {
        this.value = 0;
        this.attacks = new HashSet<>();
    }

    public Karam(byte value, HashSet<String> attacks) {
        this.value = value;
        this.attacks = attacks;
    }


    public byte getValue() {
        return value;
    }

    public void addValue(int value) {
        if(this.value + value >= MAX) {
            this.value = MAX;
        }else if(this.value < MAX) {
            this.value += (byte) value;
        }
    }
    public void subValue(LivingEntity entity){
        if(this.value == 0) {
            return;
        }
        this.value--;
    }

    public void setValue(byte value) {
        this.value = value;
    }
    public HashSet<String> getAttacks() {
        return attacks;
    }

    public void sendPacket(LivingEntity entity,float absorptionAmount){
        if(entity instanceof ServerPlayer player){
            PacketDistributor.sendToPlayer(player,new KaramPacket(entity.getId(),this.value,absorptionAmount));
        }else{
            PacketDistributor.sendToPlayersTrackingEntity(entity,new KaramPacket(entity.getId(),this.value,absorptionAmount));
        }
    }
}
