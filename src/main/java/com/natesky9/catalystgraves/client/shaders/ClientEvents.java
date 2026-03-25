package com.natesky9.catalystgraves.client.shaders;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGMenus;
import com.natesky9.catalystgraves.client.screen.BindingScreen;
import com.natesky9.catalystgraves.client.screen.CatalogueScreen;
import com.natesky9.catalystgraves.client.tooltip.CatalogueClientTooltipComponent;
import com.natesky9.catalystgraves.client.tooltip.CatalogueTooltipData;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = CatalystGraves.MODID, value = Dist.CLIENT)
public class ClientEvents
{
    private static ShaderInstance starrySkyShader;
    private static ShaderInstance bindingShader;

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(CGMenus.CATALOGUE.get(), CatalogueScreen::new);
        event.register(CGMenus.BINDING.get(), BindingScreen::new);
    }

    @SuppressWarnings("null")
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException
    {
        // Shader adapted from here
        // https://godotshaders.com/shader/abstract-3d/
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                                                ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "starry_sky"),
                                                DefaultVertexFormat.POSITION),
                             shader -> starrySkyShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                                                ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "binding_bg"),
                                                DefaultVertexFormat.POSITION),
                             shader -> bindingShader = shader);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event)
    {
        // Le decimos a NeoForge: "Cuando veas un CatalogueTooltipData, dibújalo usando CatalogueClientTooltipComponent"
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