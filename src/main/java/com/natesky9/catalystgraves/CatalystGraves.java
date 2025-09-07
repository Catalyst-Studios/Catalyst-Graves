package com.natesky9.catalystgraves;

import com.natesky9.catalystgraves.Init.*;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.OptionalDouble;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CatalystGraves.MODID)
public class CatalystGraves {

    public static final String MODID = "catalystgraves";

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CatalystGraves(IEventBus modEventBus, ModContainer modContainer) {

        CGBlocks.register(modEventBus);
        CGItems.register(modEventBus);
        CGMenus.register(modEventBus);
        CGCreativeTabs.register(modEventBus);
        CGBlockEntities.register(modEventBus);

        //

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, CGConfig.SPEC);
    }
}
