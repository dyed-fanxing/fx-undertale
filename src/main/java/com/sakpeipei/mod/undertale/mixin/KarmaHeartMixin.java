package com.sakpeipei.mod.undertale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.gui.EnumParameters;
import com.sakpeipei.mod.undertale.client.gui.KaramHeartType;
import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;

@Mixin(Gui.class)
public abstract class KarmaHeartMixin {
    @Final
    @Shadow
    private RandomSource random;
    @Unique
    private static final Gui.HeartType KARMA_HEART = Gui.HeartType.valueOf(EnumParameters.KARMA_HEART);
    @Shadow
    private int tickCount;
    @Unique
    private byte lastKarma;     // 上一次的KARMA值
    @Unique
    private int lastTotalHearts; // 上一次总心数量


    @Shadow
    protected abstract void renderHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean isHardcore, boolean isBlinking, boolean isHalf);
    /**
     * 重载renderHearts方法，添加KARMA效果支持
     */
    @Overwrite
    private void renderHearts(GuiGraphics guiGraphics, Player player, int startX, int startY, int rowSpacing,
                              int highlightedHeartIndex, float maxHealth, int currentHealth,
                              int displayHealth, int absorptionAmount, boolean shouldRenderHighlight) {

        Gui.HeartType heartType = getHeartTypeForPlayer(player);
        boolean isHardcore = player.level().getLevelData().isHardcore();
        int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = Mth.ceil(absorptionAmount / 2.0F);
        int maxHealthHalfHearts = maxHealthHearts * 2;

        // KARMA效果值
        byte karmaValue = 0;
        if(player.hasEffect(MobEffectRegistry.KARMA)){
            karmaValue = player.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT).getValue();
        }
        int totalHearts = currentHealth + absorptionAmount;
        LogUtils.getLogger().info("玩家受伤时间{},玩家最近一次伤害来源{}",player.hurtTime,player.getLastDamageSource());
        if(player.hurtTime ==  0){
            karmaValue = lastKarma;
        }else{
            if(Objects.requireNonNull(player.getLastDamageSource()).is(DamageTypes.KARMA)){
                lastKarma = karmaValue;
            }
        }
        LogUtils.getLogger().info("渲染KR{},总血量{}",karmaValue,totalHearts);
        boolean krO =  karmaValue%2 == 0;
        int diff =  (absorptionAmount - karmaValue) % 2;
        boolean diffB = diff == 1;
        for(int heartIndex = maxHealthHearts + absorptionHearts - 1 , i = 0; heartIndex >= 0; --heartIndex , i++) {
            int row = heartIndex / 10;
            int column = heartIndex % 10;
            int heartX = startX + column * 8;
            int heartY = startY - row * rowSpacing;

            // 心形数量少时添加随机偏移
            if (currentHealth + absorptionAmount <= 4) {
                heartY += this.random.nextInt(2);
            }

            // 高亮心形向上偏移
            if (heartIndex < maxHealthHearts && heartIndex == highlightedHeartIndex) {
                heartY -= 2;
            }

            // 渲染心形容器背景
            this.renderHeart(guiGraphics, Gui.HeartType.CONTAINER, heartX, heartY, isHardcore, shouldRenderHighlight, false);

            int halfHeartIndex = heartIndex * 2;
            int reverseHalfHeartIndex =  ( maxHealthHearts + absorptionHearts - 1 ) * 2 - halfHeartIndex;
            boolean isAbsorptionHeart = heartIndex >= maxHealthHearts;


            // 渲染吸收护甲心形
            if (isAbsorptionHeart) {
                int absorptionHalfHearts = halfHeartIndex - maxHealthHalfHearts;
                if (absorptionHalfHearts < absorptionAmount) {
                    boolean isHalf = absorptionHalfHearts + 1 == absorptionAmount;
                    Gui.HeartType renderType = (heartType == Gui.HeartType.WITHERED) ? heartType : Gui.HeartType.ABSORBING;
                    this.renderHeart(guiGraphics, renderType, heartX, heartY, isHardcore, false, isHalf);

                    RenderSystem.enableBlend();
                    // KARMA效果逻辑：替换吸收护甲心形
                    if(karmaValue != 0){
                        if(diff < 0) {
                            if( reverseHalfHeartIndex < karmaValue){
                                guiGraphics.blitSprite(KaramHeartType.getSprite(isHardcore, false, isHalf, false), heartX, heartY, 9, 9);
                            }
                        }else{
                            if(diffB){
                                if(krO){
                                    if( reverseHalfHeartIndex < karmaValue + 1){
                                        boolean isRight = !isHalf&& (reverseHalfHeartIndex == karmaValue);
                                        guiGraphics.blitSprite(KaramHeartType.getSprite(isHardcore, false, isHalf, isRight),heartX,heartY,9,9);
                                    }
                                }else{
                                    if( reverseHalfHeartIndex < karmaValue ){
                                        boolean isRight = !isHalf&& (reverseHalfHeartIndex + 1 == karmaValue);
                                        guiGraphics.blitSprite(KaramHeartType.getSprite(isHardcore, false, isHalf, isRight),heartX,heartY,9,9);
                                    }
                                }
                            }else{
                                if( reverseHalfHeartIndex < karmaValue ) {
                                    guiGraphics.blitSprite(KaramHeartType.getSprite(isHardcore, false, isHalf, false), heartX, heartY, 9, 9);
                                }
                            }
                        }
                    }
                    RenderSystem.disableBlend();
                }
            }

            Gui.HeartType renderType =  heartType;
//             KARMA效果逻辑：替换普通生命值心形
            if(karmaValue != 0 && reverseHalfHeartIndex - ( maxHealth - currentHealth) <= karmaValue){
                renderType = KARMA_HEART;
            }

            // 渲染高亮闪烁的心形（受伤时）
            if (shouldRenderHighlight && halfHeartIndex < displayHealth) {
                boolean isHalf = halfHeartIndex + 1 == displayHealth;
                this.renderHeart(guiGraphics, renderType, heartX, heartY, isHardcore, true, isHalf);
            }

            // 渲染普通生命值心形
            if (halfHeartIndex < currentHealth) {
                boolean isHalf = halfHeartIndex + 1 == currentHealth;
                this.renderHeart(guiGraphics, renderType, heartX, heartY, isHardcore, false, isHalf);
            }
        }
    }


    @Unique
    private Gui.HeartType getHeartTypeForPlayer(Player player) {
        Gui.HeartType gui$hearttype;
        if (player.hasEffect(MobEffects.POISON)) {
            gui$hearttype = Gui.HeartType.POISIONED;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            gui$hearttype = Gui.HeartType.WITHERED;
        } else if (player.isFullyFrozen()) {
            gui$hearttype = Gui.HeartType.FROZEN;
        } else {
            gui$hearttype = Gui.HeartType.NORMAL;
        }

        gui$hearttype = EventHooks.firePlayerHeartTypeEvent(player, gui$hearttype);
        return gui$hearttype;
    }
}