package com.sakpeipei.mod.undertale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sakpeipei.mod.undertale.client.gui.EnumParameters;
import com.sakpeipei.mod.undertale.client.gui.KaramHeartType;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.MobEffectRegistry;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.*;


@Mixin(Gui.class)
public abstract class KarmaHeartMixin {
    @Final
    @Shadow
    private RandomSource random;
    @Unique
    private static final Gui.HeartType KARMA_HEART = Gui.HeartType.valueOf(EnumParameters.KARMA_HEART);

    @Shadow
    protected abstract void renderHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean isHardcore, boolean isBlinking, boolean isHalf);
    /**
     * 重载renderHearts方法，添加KARMA效果支持
     * @author Sakpeipei
     * @reason 复制原版渲染逻辑，添加自定义的渲染KR心（包括右半心）
     */
    @Overwrite
    private void renderHearts(GuiGraphics guiGraphics, Player player, int startX, int startY, int rowSpacing,
                              int highlightedHeartIndex, float maxHealth, int currentHealth,
                              int displayHealth, int absorptionAmount, boolean shouldRenderHighlight) {

        Gui.HeartType heartType = undertale$getHeartTypeForPlayer(player);
        boolean isHardcore = player.level().getLevelData().isHardcore();
        int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = Mth.ceil(absorptionAmount / 2.0F);
        int maxHealthHalfHearts = maxHealthHearts * 2;

        // KARMA效果值
        byte karmaValue = 0;
        if(player.hasEffect(MobEffectRegistry.KARMA)){
            karmaValue = player.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT).getValue();
        }
//        LogUtils.getLogger().info("渲染KR{}",karmaValue);
        boolean karamEven =  karmaValue%2 == 0; // kr值是否偶数
        int absorptionDiff = absorptionAmount - karmaValue; // 吸收值和kr值的差值
        boolean absorptionOdd = absorptionDiff % 2 == 1 ; //吸收差值是否为奇数

        boolean karamHealthEven = (-absorptionDiff) %2 ==0;
        int healthDiff = currentHealth + absorptionDiff;
        boolean healthOdd = healthDiff %2 == 1; //血量差值
        for(int heartIndex = maxHealthHearts + absorptionHearts - 1 , i = 0; heartIndex >= 0; --heartIndex , i++) {
            int row = heartIndex / 10;
            int column = heartIndex % 10;
            int heartX = startX + column * 8;
            int heartY = startY - row * rowSpacing;

            if (currentHealth + absorptionAmount <= 4) {
                heartY += this.random.nextInt(2);
            }

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


                    /*
                        KARMA效果逻辑：替换吸收护甲心形，判断当前索引的半心是否应该渲染为Karma心
                        吸收值小于等于KR值，判断索引在KR值范围内，全部渲染成KR心
                        吸收值大于KR值，则多出来的KR值需要渲染到生命值上
                            差值为偶数，则代表覆盖后的最后一颗KR心没有右半心，判断索引是否在KR值范围内，走原版逻辑渲染
                            差值为奇数，则代表覆盖后的KR心最后一颗是右半心，需要自定义渲染右半心的逻辑
                                当KR值是偶数时，是否渲染成KR心的判断条件为 反转索引 < KR值 + 1
                                当KR值是奇数时，是否渲染成KR心的判断条件为 反转索引 + 1 < KR值
                     */
                    if(karmaValue > 0) {
                        // 以下为上方判断的数学计算简化版
                        int renderThreshold = karmaValue + ((absorptionDiff > 0 && absorptionOdd && karamEven) ? 1 : 0);
                        if (reverseHalfHeartIndex < renderThreshold) {
                            boolean isRight = absorptionDiff > 0 && absorptionOdd && reverseHalfHeartIndex + (karamEven?0:1) == karmaValue;
                            undertale$renderKaramHeart(guiGraphics,heartX,heartY,isHardcore,false,isHalf,isRight);
                        }
                    }
                }
            }

            // 渲染高亮闪烁的心形（受伤时）
            if (shouldRenderHighlight && halfHeartIndex < displayHealth) {
                boolean isHalf = halfHeartIndex + 1 == displayHealth;
                this.renderHeart(guiGraphics, heartType, heartX, heartY, isHardcore, true, isHalf);
            }

            // 渲染普通生命值心形
            // 渲染普通生命值心形
            if (halfHeartIndex < currentHealth) {
                boolean isHalf = halfHeartIndex + 1 == currentHealth;
                this.renderHeart(guiGraphics, heartType, heartX, heartY, isHardcore, false, isHalf);

                // KARMA效果逻辑：完全复制吸收心的复杂判断
                if(absorptionDiff < 0) {
                    int healthIndex =  currentHealth- halfHeartIndex - (currentHealth%2==0?2:1);
                    int renderThreshold = (-absorptionDiff) + ((healthDiff > 0 && healthOdd && karamHealthEven) ? 1 : 0);
                    if (healthIndex < renderThreshold) {
                        boolean isRight = healthDiff > 0 && healthOdd && healthIndex + (karamHealthEven ? 0 : 1) == (-absorptionDiff);
                        undertale$renderKaramHeart(guiGraphics, heartX, heartY, isHardcore, false, isHalf, isRight);
                    }
                }
            }
        }
    }
    @Unique
    private void undertale$renderKaramHeart(GuiGraphics guiGraphics, int x, int y, boolean isHardcore, boolean isBlinking, boolean isHalf, boolean isRight) {
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(KaramHeartType.getSprite(isHardcore, false, isHalf, isRight), x, y, 9, 9);
        RenderSystem.disableBlend();
    }

    @Unique
    private Gui.HeartType undertale$getHeartTypeForPlayer(Player player) {
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