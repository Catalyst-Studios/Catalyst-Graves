package com.natesky9.catalystgraves.Menus;

import com.natesky9.catalystgraves.Screen.CatalogueScreen;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class CatalogueButton extends AbstractButton {
    public final ResourceLocation advancement;
    final CatalogueScreen catalogueScreen;
    int cost;
    double mousePressedTime;
    public CatalogueButton(CatalogueScreen screen,int x, int y, int width, int height, ResourceLocation resourceLocation) {
        super(x, y, width, height, Component.translatable(resourceLocation.toString()));
        setMessage(Component.translatable(resourceLocation.toLanguageKey()));
        catalogueScreen = screen;
        advancement = resourceLocation;
    }
    public void setCost(int value)
    {
        cost = value;
    }
    public int getCost()
    {
        return cost;
    }


    @Override
    public void onPress() {
        setFocused(true);
        catalogueScreen.player.level();
        mousePressedTime = Blaze3D.getTime();
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        int level = Minecraft.getInstance().player.experienceLevel;
        if (!isHovered()) return;
        //System.out.println(advancement);
        setFocused(false);

        double time = Blaze3D.getTime() - mousePressedTime;
        /*System.out.println("time mouse held: " + time);*/
        if (time < 2)
        {
            manager.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.0f));
            return;
        }
        PacketDistributor.sendToServer(new CataloguePacket(advancement.toString(),cost));
        if (level < cost)
            manager.play(SimpleSoundInstance.forUI(SoundEvents.CHEST_LOCKED,.5f));
        else
            manager.play(SimpleSoundInstance.forUI(SoundEvents.ENDER_CHEST_OPEN,.5f));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        double timePressed = Mth.clamp(Blaze3D.getTime() - mousePressedTime,0,2);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SPRITES.get(this.active, false),
                this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.drawCenteredString(minecraft.font,getMessage(),getX()+width/2,getY()+4,16777215);

        float costToLevelRatio = Mth.clamp((float)catalogueScreen.player.experienceLevel/cost,0,1);
        int maxProgress = (int) (Mth.clamp(timePressed/2 * width,0,width*costToLevelRatio));

        if (isFocused())
            guiGraphics.blitSprite(SPRITES.get(this.active, true),
                    getX(),getY(), maxProgress,getHeight());

        if (active)
        {
            int color = catalogueScreen.player.experienceLevel >= cost ? 3328050:16711680;
            guiGraphics.drawCenteredString(minecraft.font,String.valueOf(cost),
                    getX()+width/2,getY()+height,color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
