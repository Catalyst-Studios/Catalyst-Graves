package com.natesky9.catalystgraves.Screen;

import com.natesky9.DataGen.CGAdvancementProvider;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.Menus.CatalogueButton;
import com.natesky9.catalystgraves.Menus.CatalogueMenu;
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
import net.minecraft.client.renderer.RenderType;
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
                100 ,16, CGAdvancementProvider.DESECRATE));
        desecrate.setCost(CGConfig.DESECRATE_COST.get());
        organization = addRenderableWidget(new CatalogueButton(this,getButtonX(2),getButtonY(2),
                100,16, CGAdvancementProvider.ORGANIZATION));
        organization.setCost(CGConfig.ORGANIZATION_COST.get());
        vitality = addRenderableWidget(new CatalogueButton(this,getButtonX(3),getButtonY(3),
                100,16, CGAdvancementProvider.LESSER_VITALITY));
        vitality.setCost(CGConfig.LESSER_VITALITY_COST.get());
        greaterVitality = addRenderableWidget(new CatalogueButton(this,getButtonX(4),getButtonY(4),
                100,16, CGAdvancementProvider.GREATER_VITALITY));
        greaterVitality.setCost(CGConfig.GREATER_VITALITY_COST.get());
        illuminate = addRenderableWidget(new CatalogueButton(this,getButtonX(5),getButtonY(5),
                100,16, CGAdvancementProvider.ILLUMINATION));
        illuminate.setCost(CGConfig.ILLUMINATION_COST.get());
        recall = addRenderableWidget(new CatalogueButton(this,getButtonX(6),getButtonY(6),
                100,16, CGAdvancementProvider.CORPOREAL_RECALL));
        recall.setCost(CGConfig.CORPOREAL_RECALL.get());

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
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderTexture(0,TEXTURE);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(RenderType::guiTextured,TEXTURE,x,y,0,0,imageWidth,imageHeight,256,256);

        //
        x = leftPos + imageWidth/2;
        y = topPos + imageHeight;
        int level = player.experienceLevel;
        String levelString = String.valueOf(level);
        guiGraphics.blit(RenderType::guiTextured,XP_BAR,x-16,y-16,0,0,32,8,32,8);
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
