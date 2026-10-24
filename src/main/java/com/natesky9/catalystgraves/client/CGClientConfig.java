package com.natesky9.catalystgraves.client;

import com.natesky9.catalystgraves.CatalystGraves;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

@SuppressWarnings("null")
@Mod(value = CatalystGraves.MODID, dist = Dist.CLIENT)
public class CGClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> DISPLAYED_ITEMS =
        BUILDER.comment("Number of items to display on the grave (0 to disable)")
            .defineInRange("itemDisplayCount", 3, 0, 10);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public CGClientConfig(ModContainer container)
    {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
