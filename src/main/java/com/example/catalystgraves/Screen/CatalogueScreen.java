package com.example.catalystgraves.Screen;

import com.example.DataGen.ModAdvancementProvider;
import com.example.catalystgraves.CatalystGraves;
import com.example.catalystgraves.Init.ModConfig;
import com.example.catalystgraves.Menus.CatalogueButton;
import com.example.catalystgraves.Menus.CatalogueMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class CatalogueScreen extends AbstractContainerScreen<CatalogueMenu> implements ClientAdvancements.Listener{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,"textures/gui/catalogue.png");
    private static final ResourceLocation XP_BAR =
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,"textures/gui/xp_bar.png");
    public Player player;
    ClientAdvancements clientAdvancements;
    CatalogueButton desecrate;
    CatalogueButton organization;
    CatalogueButton vitality;
    CatalogueButton greaterVitality;
    CatalogueButton illuminate;
    CatalogueButton recall;

    public CatalogueScreen(CatalogueMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        player = playerInventory.player;
        clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
    }

    @Override
    protected void init() {
        imageWidth = 256;
        imageHeight = 166;
        super.init();
        titleLabelX = leftPos;
        desecrate = addRenderableWidget(new CatalogueButton(this,getButtonX(1),getButtonY(1),
                100 ,16, ModAdvancementProvider.DESECRATE));
        desecrate.setCost(ModConfig.DESECRATE_COST.get());
        organization = addRenderableWidget(new CatalogueButton(this,getButtonX(2),getButtonY(2),
                100,16,ModAdvancementProvider.ORGANIZATION));
        organization.setCost(ModConfig.ORGANIZATION_COST.get());
        vitality = addRenderableWidget(new CatalogueButton(this,getButtonX(3),getButtonY(3),
                100,16,ModAdvancementProvider.LESSER_VITALITY));
        vitality.setCost(ModConfig.LESSER_VITALITY_COST.get());
        greaterVitality = addRenderableWidget(new CatalogueButton(this,getButtonX(4),getButtonY(4),
                100,16,ModAdvancementProvider.GREATER_VITALITY));
        greaterVitality.setCost(ModConfig.GREATER_VITALITY_COST.get());
        illuminate = addRenderableWidget(new CatalogueButton(this,getButtonX(5),getButtonY(5),
                100,16,ModAdvancementProvider.ILLUMINATION));
        illuminate.setCost(ModConfig.ILLUMINATION_COST.get());
        recall = addRenderableWidget(new CatalogueButton(this,getButtonX(6),getButtonY(6),
                100,16,ModAdvancementProvider.CORPOREAL_RECALL));
        recall.setCost(ModConfig.CORPOREAL_RECALL.get());

        clientAdvancements.setListener(this);
    }
    int getButtonX(int index)
    {
        return leftPos + ((index-1) % 2) * 124 + 16;
    }
    int getButtonY(int index)
    {
        return topPos + ((index-1)/2) * 32 + 32;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, this.title, imageWidth/2, this.titleLabelY, 16777215);


        int x = leftPos;
        int y = topPos;

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderTexture(0,TEXTURE);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE,x,y,0,0,imageWidth,imageHeight);

        //
        x = leftPos + imageWidth/2;
        y = topPos + imageHeight;
        int level = player.experienceLevel;
        String levelString = String.valueOf(level);
        guiGraphics.blit(XP_BAR,x-16,y-16,0,0,32,8,32,8);
        guiGraphics.drawCenteredString(font,levelString,x,y-20,16777215);
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode advancementNode) {

    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode advancementNode) {

    }

    @Override
    public void onAddAdvancementTask(AdvancementNode advancementNode) {

    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode advancementNode) {

    }

    @Override
    public void onAdvancementsCleared() {

    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode advancementNode, AdvancementProgress advancementProgress) {
        for (Renderable renderable:renderables)
        {
            if (!(renderable instanceof CatalogueButton button)) continue;

            if (button.getCost() == 0)
                button.active = false;

            if (advancementNode.holder().id().equals(button.advancement))
                button.active = !advancementProgress.isDone();
        }
        //replaced with loop
        //System.out.println("updated: " + advancementNode.holder().id());
        //if (advancementNode.holder().equals(ModAdvancementProvider.desecrate))
        //    desecrate.active = !advancementProgress.isDone();
        //if (advancementNode.holder().equals(ModAdvancementProvider.lesser_vitality))
        //    vitality.active = !advancementProgress.isDone();
        //if (advancementNode.holder().equals(ModAdvancementProvider.greater_vitality))
        //    greaterVitality.active = !advancementProgress.isDone() && !vitality.active ;
        //if (advancementNode.holder().equals(ModAdvancementProvider.recall))
        //    recall.active = !advancementProgress.isDone() ;

    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder advancementHolder) {

    }
}
