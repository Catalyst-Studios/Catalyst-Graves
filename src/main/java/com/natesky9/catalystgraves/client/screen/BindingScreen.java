package com.natesky9.catalystgraves.client.screen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.client.menus.BindingMenu;
import com.natesky9.catalystgraves.client.menus.BindingPacket;
import com.natesky9.catalystgraves.client.shaders.ClientEvents;
import com.natesky9.catalystgraves.datagen.CGItemTagsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/**
 * Screen for the Soulbinding interface. 
 * Allows players to apply Soulbound enchantment to items by holding down the mouse.
 */
@SuppressWarnings("null")
public class BindingScreen extends AbstractContainerScreen<BindingMenu>
{
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/binding.png");

    private static final ResourceLocation FRAME_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/binding_frame.png");
    
    // Constants for timing
    private static final int MAX_WINDUP = 40; // 2 seconds (20 ticks * 2)
    private static final int ERROR_DISPLAY_TICKS = 100; // 5 seconds (20 ticks * 5)
    private static final int LEVEL_COST = CGConfig.SOULBOUND_COST.get();

    private boolean leftDown = false;
    private Slot clickedSlot = null;
    private float colorTransition = 0.0f;

    // Error message state
    private int errorTimer = 0;
    private Component errorMessage = null;

    public BindingScreen(BindingMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, FRAME_TEXTURE);
        
        // Draw the outer frame
        guiGraphics.blit(FRAME_TEXTURE, leftPos - 12, topPos - 12, 0, 0, imageWidth + 24, imageHeight + 24, 280, 190);

        boolean isEnchantable = false;
        if(hoveredSlot != null && !hoveredSlot.getItem().isEmpty())
        {
            ItemStack stack = hoveredSlot.getItem();
            if(stack.is(CGItemTagsProvider.SOULBOUND_APPLICABLE))
            {
                isEnchantable = true;
            }
        }

        // Handle color transition for the shader based on item validity
        if(isEnchantable)
        {
            colorTransition = Math.min(1.0f, colorTransition + 0.05f);
        }
        else
        {
            colorTransition = Math.max(0.0f, colorTransition - 0.05f);
        }

        ShaderInstance shader = ClientEvents.getBindingShader();
        if(shader != null)
        {
            if(shader.getUniform("Time") != null)
            {
                float totalTime = (minecraft.level.getGameTime() + partialTick) / 20.0F;
                shader.getUniform("Time").set(totalTime);
            }
            if(shader.getUniform("IsValid") != null)
            {
                shader.getUniform("IsValid").set(colorTransition);
            }

            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);

            float u1 = 0.0F;
            float u2 = (float)imageWidth / 256.0F;
            float v1 = 0.0F;
            float v2 = (float)imageHeight / 256.0F;

            // Render the interior background with the custom shader
            bufferbuilder.addVertex(matrix4f, leftPos, topPos + imageHeight, 0.0F).setUv(u1, v2);
            bufferbuilder.addVertex(matrix4f, leftPos + imageWidth, topPos + imageHeight, 0.0F).setUv(u2, v2);
            bufferbuilder.addVertex(matrix4f, leftPos + imageWidth, topPos, 0.0F).setUv(u2, v1);
            bufferbuilder.addVertex(matrix4f, leftPos, topPos, 0.0F).setUv(u1, v1);

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }
        else
        {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, TEXTURE);
            guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if(button == 0)
        {
            leftDown = false;
            Slot slot = getSlotUnderMouse();
            
            if(slot != null && slot == clickedSlot)
            {
                // If the 2-second windup is complete
                if(menu.windup >= MAX_WINDUP - 2) 
                {
                    // Check for player levels before sending the packet
                    if(minecraft.player.experienceLevel < LEVEL_COST)
                    {
                        this.errorMessage = Component.literal("Insufficient Levels! (Requires: " + LEVEL_COST + ")");
                        this.errorTimer = ERROR_DISPLAY_TICKS;
                    }
                    else
                    {
                        PacketDistributor.sendToServer(new BindingPacket(clickedSlot.index));
                    }
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // 1. Render the dark background behind the GUI
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick); 
        
        // 2. Render background texture, slots, and labels
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // --- ANIMATION LOGIC ---
        // Increment windup if mouse is held over the same slot, otherwise decrement
        menu.windup += (leftDown && hoveredSlot == clickedSlot) ? 1 : -1;
        menu.windup = Mth.clamp(menu.windup, 0, MAX_WINDUP);
        menu.clock += getMenu().windup;
        
        int rotation = ((int)minecraft.level.getGameTime() % 360) + getMenu().clock / 2;

        if(hoveredSlot != null && !hoveredSlot.getItem().isEmpty())
        {
            ItemStack stack = hoveredSlot.getItem();
            boolean canEnchant = stack.is(CGItemTagsProvider.SOULBOUND_APPLICABLE);

            // --- RENDER FLOATING ROTATING ITEM ---
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            // Coordinates relative to the screen
            poseStack.translate(leftPos + imageWidth / 2f, topPos + 40, 32);
            poseStack.scale(2, 2, 2);
            if(canEnchant) {
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            }
            poseStack.translate(-8, -8, -150);
            guiGraphics.renderItem(stack, 0, 0, 0);
            poseStack.popPose();

            // --- RENDER ERROR TEXT (Tags check) ---
            if(!canEnchant)
            {
                int x = leftPos + imageWidth / 2;
                int y = topPos + imageHeight / 2 - 24;
                guiGraphics.drawCenteredString(font, "Item can't accept soulbound", x, y, 0xC80000);
            }
        }

        // --- RENDER TEMPORARY ERROR MESSAGE (Level check) ---
        if(errorTimer > 0)
        {
            int x = leftPos + imageWidth / 2;
            int y = topPos + imageHeight / 2 + 10;
            guiGraphics.drawCenteredString(font, errorMessage, x, y, 0xFF5555);
            errorTimer--;
        }

        // 3. Render tooltips last to ensure they are on top
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
    {
        if(mouseButton == 0)
            leftDown = true;
        clickedSlot = slot;
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        // 1. Draw the "SoulBound" title at the top
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, true);

        // 2. Draw the "Inventory" label above player slots
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 90, 0x404040, false);
    }

    
    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot)
    {
        super.renderSlot(guiGraphics, slot);
    }
}