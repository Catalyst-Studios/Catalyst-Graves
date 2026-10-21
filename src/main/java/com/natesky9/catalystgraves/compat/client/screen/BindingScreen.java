package com.natesky9.catalystgraves.compat.client.screen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.compat.client.CGClientEvents;
import com.natesky9.catalystgraves.compat.client.menus.BindingMenu;
import com.natesky9.catalystgraves.compat.client.menus.BindingPacket;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.server.GraveLogic;
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

@SuppressWarnings("null")
public class BindingScreen extends AbstractContainerScreen<BindingMenu>
{
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/binding.png");

    private static final int MAX_WINDUP = 40;
    private static final int ERROR_DISPLAY_TICKS = 100;

    private boolean leftDown = false;
    private Slot clickedSlot = null;
    private float colorTransition = 0.0f;

    private int errorTimer = 0;
    private Component errorMessage = null;

    private boolean enchantedThisClick = false;

    public BindingScreen(BindingMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    private boolean isSoulboundApplicable(ItemStack stack)
    {
        return !stack.isEmpty() && !GraveLogic.isBlacklistedFromSoulbound(stack);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        boolean isEnchantable = false;

        if (hoveredSlot != null && isSoulboundApplicable(hoveredSlot.getItem()))
        {
            isEnchantable = true;
        }

        colorTransition = isEnchantable 
            ? Math.min(1.0f, colorTransition + 0.05f) 
            : Math.max(0.0f, colorTransition - 0.05f);

        ShaderInstance shader = CGClientEvents.getBindingShader();
        if (shader != null)
        {
            if (shader.getUniform("Time") != null)
            {
                float totalTime = (minecraft.level.getGameTime() + partialTick) / 20.0F;
                shader.getUniform("Time").set(totalTime);
            }
            if (shader.getUniform("IsValid") != null)
            {
                shader.getUniform("IsValid").set(colorTransition);
            }

            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            float u1 = 0.0F;
            float u2 = (float) imageWidth / 256.0F;
            float v1 = 0.0F;
            float v2 = (float) imageHeight / 256.0F;

            bufferbuilder.addVertex(matrix4f, leftPos, topPos + imageHeight, 0.0F).setUv(u1, v2);
            bufferbuilder.addVertex(matrix4f, leftPos + imageWidth, topPos + imageHeight, 0.0F).setUv(u2, v2);
            bufferbuilder.addVertex(matrix4f, leftPos + imageWidth, topPos, 0.0F).setUv(u2, v1);
            bufferbuilder.addVertex(matrix4f, leftPos, topPos, 0.0F).setUv(u1, v1);

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }
        else
        {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        int levelCost = CGConfig.SOULBOUND_COST.get();
        int centerX = this.imageWidth / 2;

        // 1. Título principal (y = 6)
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 6, 0xFFFFFF);

        // 2. Coste de Experiencia (y = 14)
        boolean hasEnoughXp = minecraft.player != null && minecraft.player.experienceLevel >= levelCost;
        Component costText = Component.translatable("gui.catalystgraves.binding.cost", levelCost);
        int costColor = hasEnoughXp ? 0x80FF20 : 0xFF5555;
        guiGraphics.drawCenteredString(this.font, costText, centerX, 14, costColor);

        // 3. Texto inferior (y = 63)
        if (errorMessage != null && errorTimer > 0)
        {
            guiGraphics.drawCenteredString(this.font, errorMessage, centerX, 63, 0xFFFF5555);
        }
        else if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && !isSoulboundApplicable(hoveredSlot.getItem()))
        {
            Component invalidText = Component.translatable("gui.catalystgraves.cannot_soulbound");
            guiGraphics.drawCenteredString(this.font, invalidText, centerX, 63, 0xFFFF5555);
        }
        else
        {
            Component helpText = Component.translatable("gui.catalystgraves.binding.help");
            guiGraphics.drawCenteredString(this.font, helpText, centerX, 63, 0xAAAAAA);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 91, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Previsualización central del ítem flotante/giratorio
        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty())
        {
            ItemStack stack = hoveredSlot.getItem();
            boolean canEnchant = isSoulboundApplicable(stack);

            float rotation = ((minecraft.level.getGameTime() + partialTick) * 3.0f) + (menu.clock * 2.0f);

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(leftPos + imageWidth / 2.0f, topPos + 40, 32);
            poseStack.scale(2.0f, 2.0f, 2.0f);

            if (canEnchant)
            {
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            }

            poseStack.translate(-8, -8, -150);
            guiGraphics.renderItem(stack, 0, 0, 0);
            poseStack.popPose();
        }

        // Indicador circular de carga sobre el slot
        if (leftDown && clickedSlot != null)
        {
            float progress = (float) menu.windup / MAX_WINDUP;
            renderEnchantProgress(guiGraphics, clickedSlot, progress);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderEnchantProgress(GuiGraphics guiGraphics, Slot slot, float progress)
    {
        int slotX = leftPos + slot.x;
        int slotY = topPos + slot.y;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(slotX + 8, slotY + 8, 300);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, 0, 0, 0).setColor(0.2f, 0.8f, 1.0f, 0.5f);

        int segments = 32;
        float maxAngle = progress * (float) Math.PI * 2.0f;
        float radius = 9.0f;

        for (int i = 0; i <= segments; i++)
        {
            float currentProgress = (float) i / segments;
            if (currentProgress * (float) Math.PI * 2.0f > maxAngle)
            {
                float angle = -((float) Math.PI / 2.0f) + maxAngle;
                buffer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0)
                    .setColor(0.1f, 0.6f, 1.0f, 0.8f);
                break;
            }

            float angle = -((float) Math.PI / 2.0f) + (currentProgress * (float) Math.PI * 2.0f);
            buffer.addVertex(matrix, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0)
                .setColor(0.1f, 0.6f, 1.0f, 0.8f);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        if (errorTimer > 0)
        {
            errorTimer--;
            if (errorTimer <= 0)
            {
                errorMessage = null;
            }
        }

        if (leftDown && clickedSlot != null)
        {
            if (hoveredSlot != clickedSlot || !isSoulboundApplicable(clickedSlot.getItem()))
            {
                resetEnchant();
                return;
            }

            if (!enchantedThisClick)
            {
                menu.windup = Math.min(MAX_WINDUP, menu.windup + 1);
                menu.clock += menu.windup;

                if (menu.windup >= MAX_WINDUP)
                {
                    completeEnchant();
                }
            }
            else
            {
                menu.clock += MAX_WINDUP;
            }
        }
        else if (menu.windup > 0)
        {
            menu.windup--;
            menu.clock += menu.windup;
        }
    }

    private void completeEnchant()
    {
        if (clickedSlot != null)
        {
            PacketDistributor.sendToServer(new BindingPacket(clickedSlot.getSlotIndex()));
        }
        enchantedThisClick = true;
    }

    private void resetEnchant()
    {
        leftDown = false;
        clickedSlot = null;
        menu.windup = 0;
        enchantedThisClick = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            Slot slot = getSlotUnderMouse();
            if (slot != null && !slot.getItem().isEmpty())
            {
                if (!isSoulboundApplicable(slot.getItem()))
                {
                    return true;
                }

                int levelCost = CGConfig.SOULBOUND_COST.get();
                if (minecraft.player.experienceLevel < levelCost)
                {
                    errorMessage = Component.translatable("gui.catalystgraves.insufficient_levels", levelCost);
                    errorTimer = ERROR_DISPLAY_TICKS;
                    return true;
                }

                leftDown = true;
                clickedSlot = slot;
                menu.windup = 0;
                enchantedThisClick = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            resetEnchant();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
    {
    }
}