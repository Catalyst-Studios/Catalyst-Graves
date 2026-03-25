package com.natesky9.catalystgraves;

import com.natesky9.catalystgraves.Init.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(CatalystGraves.MODID)
public class CatalystGraves
{
    public static final String MODID = "catalystgraves";
    public CatalystGraves(IEventBus modEventBus, ModContainer modContainer)
    {
        CGBlocks.register(modEventBus);
        CGItems.register(modEventBus);
        CGMenus.register(modEventBus);
        CGCreativeTabs.register(modEventBus);
        CGBlockEntities.register(modEventBus);

        modContainer.registerConfig(Type.COMMON, CGConfig.SPEC);
    }
}
