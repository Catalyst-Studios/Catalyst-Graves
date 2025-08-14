package com.natesky9.DataGen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CGDataGenerators {
    public static void gatherData(GatherDataEvent.Client event)
    {
        //datagen is a lifesaver
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, new AdvancementProvider(packOutput, lookupProvider, List.of(new CGAdvancementProvider())));
        generator.addProvider(true, CGBlockLootTableProvider.ModLootTableProvider.create(packOutput,lookupProvider));

        event.createProvider(CGDatapackProvider::Make);
        BlockTagsProvider blockTagsProvider = new CGBlockTagsProvider(packOutput,lookupProvider);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new CGItemTagsProvider(packOutput,lookupProvider,
                blockTagsProvider.contentsGetter()));
        event.createProvider(CGEnchantmentTagsProvider::new);
    }
}
