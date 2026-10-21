package com.natesky9.catalystgraves.compat.client;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.block.GraveBlockRenderer;
import com.natesky9.catalystgraves.compat.client.screen.BindingScreen;
import com.natesky9.catalystgraves.compat.client.screen.CatalogueScreen;
import com.natesky9.catalystgraves.compat.client.tooltip.CatalogueClientTooltipComponent;
import com.natesky9.catalystgraves.compat.client.tooltip.CatalogueTooltipData;
import com.natesky9.catalystgraves.init.CGBlockEntities;
import com.natesky9.catalystgraves.init.CGMenus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = CatalystGraves.MODID, value = Dist.CLIENT)
@SuppressWarnings("null")
public class CGClientEvents
{
    private static ShaderInstance starrySkyShader;
    private static ShaderInstance bindingShader;

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(CGMenus.CATALOGUE.get(), CatalogueScreen::new);
        event.register(CGMenus.BINDING.get(), BindingScreen::new);
    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(CGBlockEntities.SIMPLE_GRAVE.get(), GraveBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException
    {
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(),
                               ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "starry_sky"),
                               DefaultVertexFormat.POSITION),
            shader -> starrySkyShader = shader);

        event.registerShader(
            new ShaderInstance(event.getResourceProvider(),
                               ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "binding_bg"),
                               DefaultVertexFormat.POSITION),
            shader -> bindingShader = shader);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(CatalogueTooltipData.class, CatalogueClientTooltipComponent::new);
    }

    public static ShaderInstance getStarrySkyShader()
    {
        return starrySkyShader;
    }

    public static ShaderInstance getBindingShader()
    {
        return bindingShader;
    }
}
