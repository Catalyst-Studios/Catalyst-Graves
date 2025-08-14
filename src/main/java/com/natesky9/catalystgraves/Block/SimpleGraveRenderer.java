package com.natesky9.catalystgraves.Block;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGBlocks;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class SimpleGraveRenderer implements BlockEntityRenderer<SimpleGraveEntity> {
    protected final BlockEntityRenderDispatcher renderDispatcher;

    public SimpleGraveRenderer(BlockEntityRendererProvider.Context context)
    {
        renderDispatcher = context.getBlockEntityRenderDispatcher();
    }
    @Override
    public void render(SimpleGraveEntity grave, float v, @NotNull PoseStack poseStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        if (grave.getLevel() == null) return;
        BlockState state = grave.getLevel().getBlockState(grave.getBlockPos());
        if (!state.is(CGBlocks.SIMPLE_GRAVE)) return;

        renderName(poseStack, multiBufferSource, light, grave);
        //
        if (CGConfig.DISPLAYED_ITEMS.get() > 0)
            renderItems(poseStack, multiBufferSource, light, overlay, grave);
        //endregion item

        if (state.getValue(SimpleGrave.GLOWING))
            renderSpectralCube(poseStack, multiBufferSource, grave);
    }
    void renderName(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, SimpleGraveEntity grave)
    {
        poseStack.pushPose();

        BlockState state = grave.getLevel().getBlockState(grave.getBlockPos());
        String name = grave.getName();

        Font font = Minecraft.getInstance().font;
        int color = 0;
        float width = .4f/font.width(name);
        //case for the tier 0 grave, to rotate the text
        if (state.getValue(SimpleGrave.STYLE) == 0)
        {
            poseStack.mulPose(new Quaternionf(0,0,-.22,1));
            poseStack.translate(-1f/16,0,0);
            poseStack.translate(0,-5f/16,-.02);
        }
        if (state.getValue(SimpleGrave.STYLE) == 2)
        {
            poseStack.translate(0,-3/16f,0);
        }
        //the -.03 is needed to have the text above the model
        poseStack.translate(10.7f/16,14.5f/16,10.3f/16-.03);
        poseStack.scale(-width,-width,width);

        font.drawInBatch(name,0,0,color,
                false, poseStack.last().pose(),multiBufferSource,
                Font.DisplayMode.NORMAL,0,light);
        poseStack.popPose();
    }
    void renderItems(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, SimpleGraveEntity grave)
    {

        //region render a floating item
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        NonNullList<ItemStack> graveItems = grave.getItems();
        if (graveItems.isEmpty()) return;
        poseStack.pushPose();

        poseStack.translate(.4f, 1f/16, .4f);
        poseStack.scale(1f/4, 1f/4, 1f/4);
        poseStack.mulPose(Axis.XN.rotationDegrees(-90));
        //TODO:implement the config option here
        ItemStack stack = grave.getItems().getFirst();
        renderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                poseStack, multiBufferSource, grave.getLevel(), 0);
        if (graveItems.size() > 2)
        {
            stack = grave.getItems().getLast();
            poseStack.translate(-.5,-.5,0);
            renderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                    poseStack, multiBufferSource, grave.getLevel(), 0);
        }
        poseStack.popPose();
    }
    void renderSpectralCube(PoseStack poseStack, MultiBufferSource multiBufferSource, SimpleGraveEntity grave)
    {
        VertexConsumer consumer = multiBufferSource.getBuffer(CatalystGraves.SPECTRAL);
        //
        Shapes.block().forAllEdges(
                (p_323073_, p_323074_, p_323075_, p_323076_, p_323077_, p_323078_) -> {
                    consumer.addVertex(poseStack.last(), (float)(p_323073_), (float)(p_323074_), (float)(p_323075_))
                            .setColor(255, 0, 255, 255)
                            .setNormal(poseStack.last(), 0,0,0);
                    consumer.addVertex(poseStack.last(), (float)(p_323076_), (float)(p_323077_), (float)(p_323078_))
                            .setColor(255, 0, 255, 255)
                            .setNormal(poseStack.last(), 0, 0, 0);
                }
        );
    }

}
