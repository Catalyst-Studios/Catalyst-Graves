package com.natesky9.catalystgraves.client.menus;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.systems.RenderSystem;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.client.screen.CatalogueScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

@SuppressWarnings("null")
public class CatalogueButton extends AbstractButton
{

    private static final ResourceLocation BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/button.png");
    private static final ResourceLocation BUTTON_DISABLED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/button_disabled.png");

    public final ResourceLocation advancement;
    final CatalogueScreen catalogueScreen;

    int cost;
    double mousePressedTime;

    public CatalogueButton(CatalogueScreen screen, int x, int y, int width, int height, ResourceLocation resourceLocation)
    {
        super(x, y, width, height, Component.translatable(resourceLocation.toString()));
        setMessage(Component.translatable(resourceLocation.toLanguageKey()));
        catalogueScreen = screen;
        advancement = resourceLocation;
        this.setTooltip(getTooltipDepending());
    }

    public CatalogueButton(CatalogueScreen screen, int x, int y, ResourceLocation resourceLocation)
    {
        this(screen, x, y, 100, 16, resourceLocation);
    }

    public CatalogueButton setCost(int value)
    {
        cost = value;
        return this;
    }

    public int getCost()
    {
        return cost;
    }

    @Override
    public void onPress()
    {
        this.setFocused(true);
        this.mousePressedTime = Blaze3D.getTime();

        int currentLevel = Minecraft.getInstance().player.experienceLevel;
        SoundManager manager = Minecraft.getInstance().getSoundManager();

        if(currentLevel >= cost)
        {
            PacketDistributor.sendToServer(new CataloguePacket(advancement.toString(), cost));
            manager.play(SimpleSoundInstance.forUI(SoundEvents.ENDER_CHEST_OPEN, .5f));
        }
        else
        {
            manager.play(SimpleSoundInstance.forUI(SoundEvents.CHEST_LOCKED, .5f));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.setFocused(false);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        double elapsed = isFocused() ? Blaze3D.getTime() - mousePressedTime : 0;

        boolean canAfford = catalogueScreen.player.experienceLevel >= cost;
        ResourceLocation texture = (this.active && canAfford) ? BUTTON_TEXTURE : BUTTON_DISABLED_TEXTURE;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);

        if(isFocused() && canAfford)
        {
            float progressFactor = Mth.clamp((float)elapsed / 2.0F, 0, 1);
            int maxProgress = (int)(progressFactor * width);

            if(maxProgress > 0)
            {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                guiGraphics.blit(BUTTON_TEXTURE, getX(), getY(), 0, 0, maxProgress, height, width, height);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        guiGraphics.drawCenteredString(minecraft.font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFF);

        if(active)
        {
            int color = canAfford ? 0x32D552 : 0xFF0000;
            guiGraphics.drawCenteredString(minecraft.font, String.valueOf(cost),
                                           getX() + width / 2, getY() + height + 2, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput)
    {
    }

    private Tooltip getTooltipDepending()
    {
        String text = "advancement.catalystgraves." + this.advancement.getPath() + ".description";
        return Tooltip.create(Component.translatable(text));
    }
}
