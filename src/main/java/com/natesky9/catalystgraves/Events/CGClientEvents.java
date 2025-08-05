package com.natesky9.catalystgraves.Events;

import com.natesky9.catalystgraves.Block.SimpleGraveRenderer;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGBlockEntities;
import com.natesky9.catalystgraves.Init.CGMenus;
import com.natesky9.catalystgraves.Screen.BindingScreen;
import com.natesky9.catalystgraves.Screen.CatalogueScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CatalystGraves.MODID,value = Dist.CLIENT)
public class CGClientEvents {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(CGMenus.CATALOGUE.get(), CatalogueScreen::new);
        event.register(CGMenus.BINDING.get(), BindingScreen::new);
    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(CGBlockEntities.SIMPLE_GRAVE.get(), SimpleGraveRenderer::new);
    }
}
