package com.natesky9.catalystgraves.Screen;

import com.natesky9.DataGen.CGItemTagsProvider;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Menus.BindingMenu;
import com.natesky9.catalystgraves.Menus.BindingPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class BindingScreen extends AbstractContainerScreen<BindingMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,"textures/gui/binding.png");
    boolean leftDown = false;
    Slot clickedSlot = null;

    public BindingScreen(BindingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderTexture(0,TEXTURE);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE,x,y,0,0,imageWidth,imageHeight);

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
        {
            leftDown = false;
            Slot slot = getSlotUnderMouse();
            if (slot != clickedSlot) return false;

            if (menu.windup > 80)
                PacketDistributor.sendToServer(new BindingPacket(clickedSlot.index));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderTooltip(guiGraphics,mouseX,mouseY);


        menu.windup += (leftDown && hoveredSlot == clickedSlot) ? 1:-1;
        menu.windup = Mth.clamp(menu.windup,0,90);

        menu.clock += getMenu().windup;
        int rotation = ((int)minecraft.level.getGameTime() % 360) + getMenu().clock/2;

        if (hoveredSlot == null) return;
        if (hoveredSlot.getItem().isEmpty()) return;

        ItemStack stack = hoveredSlot.getItem();
        boolean canEnchant = stack.is(CGItemTagsProvider.SOULBOUND_APPLICABLE);

        PoseStack poseStack = guiGraphics.pose();

        //render the spinny item
        poseStack.pushPose();
        poseStack.translate(leftPos+imageWidth/2f,topPos+40,32);
        poseStack.scale(2,2,2);
        if (canEnchant)
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-8,-8,-150);
        guiGraphics.renderItem(stack, 0,0,0);
        poseStack.popPose();

        int x = leftPos + imageWidth/2;
        int y = topPos + imageHeight/2-24;
        if (!stack.is(CGItemTagsProvider.SOULBOUND_APPLICABLE))
            guiGraphics.drawCenteredString(font,"Item can't accept soulbound",x, y,13107200);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (mouseButton == 0)
            leftDown = true;
        clickedSlot = slot;
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics,slot);
    }
}
