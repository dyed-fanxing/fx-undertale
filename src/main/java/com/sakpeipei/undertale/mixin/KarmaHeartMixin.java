package com.sakpeipei.undertale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sakpeipei.undertale.client.gui.KaramHeartType;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import com.sakpeipei.undertale.registry.MobEffectTypes;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.*;


@Mixin(Gui.class)
public abstract class KarmaHeartMixin {
    @Final
    @Shadow
    private RandomSource random;
    @Shadow
    private int tickCount;
    @Shadow
    private int lastHealth;
    @Shadow
    private int displayHealth;
    @Shadow
    private long lastHealthTime;
    @Shadow
    private long healthBlinkTime;
    @Shadow
    public int leftHeight;
    @Mutable
    @Shadow
    @Final
    private final Minecraft minecraft;

    protected KarmaHeartMixin(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Shadow
    protected abstract void renderHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean isHardcore, boolean isBlinking, boolean isHalf);
    @Shadow
    protected abstract Player getCameraPlayer();

    /**
     * 重载renderHealthLevel方法，添加KARMA效果支持
     * @author Sakpeipei
     * @reason 复制原版渲染逻辑，添加自定义的渲染KR心（包括右半心）
     */
    @Overwrite
    private void renderHealthLevel(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            int health = Mth.ceil(player.getHealth());
            boolean flag = this.healthBlinkTime > (long)this.tickCount && (this.healthBlinkTime - (long)this.tickCount) / 3L % 2L == 1L;
            long j = Util.getMillis();
            if (health < this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 20;
            } else if (health > this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 10;
            }

            if (j - this.lastHealthTime > 1000L) {
                this.lastHealth = health;
                this.displayHealth = health;
                this.lastHealthTime = j;
            }

            this.lastHealth = health;
            int k = this.displayHealth;
            this.random.setSeed(this.tickCount * 312871L);
            int l = p_283143_.guiWidth() / 2 - 91;
            int i1 = p_283143_.guiWidth() / 2 + 91;
            int j1 = p_283143_.guiHeight() - this.leftHeight;
            float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(k, health));
            int absorptionAmount = Mth.ceil(player.getAbsorptionAmount());
            int l1 = Mth.ceil((f + (float)absorptionAmount) / 2.0F / 10.0F);
            int i2 = Math.max(10 - (l1 - 2), 3);
            int j2 = j1 - 10;
            this.leftHeight += (l1 - 1) * i2 + 10;
            int k2 = -1;
            if (player.hasEffect(MobEffects.REGENERATION)) {
                k2 = this.tickCount % Mth.ceil(f + 5.0F);
            }
            // KARMA效果值
            byte karmaValue = 0;
            if(player.hasEffect(MobEffectTypes.KARMA)){
                karmaValue = player.getData(AttachmentTypes.KARMA_MOB_EFFECT).getValue();
            }
            this.minecraft.getProfiler().push("health");
            this.undertale$renderHearts(p_283143_, player, l, j1, i2, k2, f, health, k, absorptionAmount,karmaValue, flag);
            this.minecraft.getProfiler().pop();
        }

    }
    /**
     * 重载renderHearts方法，添加KARMA效果支持
     * @author Sakpeipei
     * @reason 复制原版渲染逻辑，添加自定义的渲染KR心（包括右半心）
     */
    @Unique
    private void undertale$renderHearts(GuiGraphics guiGraphics, Player player, int startX, int startY, int rowSpacing,
                                        int highlightedHeartIndex, float maxHealth, int currentHealth,
                                        int displayHealth, int absorptionAmount, int karmaValue, boolean shouldRenderHighlight) {

        Gui.HeartType heartType = undertale$getHeartTypeForPlayer(player);
        boolean isHardcore = player.level().getLevelData().isHardcore();
        int maxHealthHearts = Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = Mth.ceil(absorptionAmount / 2.0F);
        int maxHealthHalfHearts = maxHealthHearts * 2;

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