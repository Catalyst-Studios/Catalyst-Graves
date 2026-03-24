package com.natesky9.catalystgraves.client.screen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.client.menus.CatalogueButton;
import com.natesky9.catalystgraves.client.menus.CatalogueMenu;
import com.natesky9.catalystgraves.client.shaders.ClientEvents;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class CatalogueScreen extends AbstractContainerScreen<CatalogueMenu> implements ClientAdvancements.Listener
{
    private static final ResourceLocation FRAME_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/catalogue_frame.png");
    private static final ResourceLocation XP_BAR =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/xp_bar.png");

    public Player player;
    ClientAdvancements clientAdvancements;

    @SuppressWarnings("null")
    public CatalogueScreen(CatalogueMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
        this.clientAdvancements = Minecraft.getInstance().player.connection.getAdvancements();
    }

    @Override
    protected void init()
    {
        this.imageWidth = 256;
        this.imageHeight = 166;
        super.init();

        addRenderableWidget(new CatalogueButton(this, getX(1), getY(1), CGAdvancementProvider.DESECRATE)).setCost(CGConfig.DESECRATE_COST.get());
        addRenderableWidget(new CatalogueButton(this, getX(2), getY(2), CGAdvancementProvider.ORGANIZATION)).setCost(CGConfig.ORGANIZATION_COST.get());
        addRenderableWidget(new CatalogueButton(this, getX(3), getY(3), CGAdvancementProvider.LESSER_VITALITY)).setCost(CGConfig.LESSER_VITALITY_COST.get());
        addRenderableWidget(new CatalogueButton(this, getX(4), getY(4), CGAdvancementProvider.GREATER_VITALITY)).setCost(CGConfig.GREATER_VITALITY_COST.get());
        addRenderableWidget(new CatalogueButton(this, getX(5), getY(5), CGAdvancementProvider.ILLUMINATION)).setCost(CGConfig.ILLUMINATION_COST.get());
        addRenderableWidget(new CatalogueButton(this, getX(6), getY(6), CGAdvancementProvider.CORPOREAL_RECALL)).setCost(CGConfig.CORPOREAL_RECALL.get());

        clientAdvancements.setListener(this);
    }

    private int getX(int index)
    {
        return leftPos + ((index - 1) % 2) * 124 + 16;
    }
    private int getY(int index)
    {
        return topPos + ((index - 1) / 2) * 32 + 36;
    }

    @SuppressWarnings("null")
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int x = leftPos;
        int y = topPos;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, FRAME_TEXTURE);
        guiGraphics.blit(FRAME_TEXTURE, x - 12, y - 12, 0, 0, 280, 190, 280, 190);

        ShaderInstance shader = ClientEvents.getStarrySkyShader();
        if(shader != null)
        {
            if(shader.getUniform("Time") != null)
            {
                float totalTime = (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0F;
                shader.getUniform("Time").set(totalTime);
            }

            RenderSystem.setShader(() -> shader);
            
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(
                VertexFormat.Mode.QUADS, 
                DefaultVertexFormat.POSITION
            );

            bufferbuilder.addVertex(matrix4f, x, y + imageHeight, 0.0F);
            bufferbuilder.addVertex(matrix4f, x + imageWidth, y + imageHeight, 0.0F);
            bufferbuilder.addVertex(matrix4f, x + imageWidth, y, 0.0F);
            bufferbuilder.addVertex(matrix4f, x, y, 0.0F);

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }


        renderExperienceBar(guiGraphics, x, y);
    }

    @SuppressWarnings("null")
    private void renderExperienceBar(GuiGraphics guiGraphics, int x, int y)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        int xpX = x + imageWidth / 2;
        int xpY = y + imageHeight;
        guiGraphics.blit(XP_BAR, xpX - 16, xpY - 12, 0, 0, 32, 8, 32, 8);
        guiGraphics.drawCenteredString(font, String.valueOf(player.experienceLevel), xpX, xpY - 12, 0x80FF20);
    }

    @SuppressWarnings("null")
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @SuppressWarnings("null")
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawCenteredString(font, this.title, imageWidth / 2, titleLabelY, 0xFFFFFF);
    }

    @Override
    public void onUpdateAdvancementProgress(@SuppressWarnings("null") AdvancementNode node, @SuppressWarnings("null") AdvancementProgress progress)
    {
        for(Renderable r : renderables)
        {
            if(r instanceof CatalogueButton b)
            {
                if(b.getCost() == 0) b.active = false;
                if(node.holder().id().equals(b.advancement)) b.active = !progress.isDone();
            }
        }
    }

    @Override
    public void onAddAdvancementRoot(@SuppressWarnings("null") AdvancementNode n)
    {
    }
    @Override
    public void onRemoveAdvancementRoot(@SuppressWarnings("null") AdvancementNode n)
    {
    }
    @Override
    public void onAddAdvancementTask(@SuppressWarnings("null") AdvancementNode n)
    {
    }
    @Override
    public void onRemoveAdvancementTask(@SuppressWarnings("null") AdvancementNode n)
    {
    }
    @Override
    public void onAdvancementsCleared()
    {
    }
    @Override
    public void onSelectedTabChanged(@SuppressWarnings("null") @Nullable AdvancementHolder h)
    {
    }
}