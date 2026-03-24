package com.natesky9.catalystgraves.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class CGDataGenerators
{
    public static void gatherData(GatherDataEvent event)
    {
        // datagen is a lifesaver
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(true, new AdvancementProvider(packOutput, lookupProvider, helper, List.of(new CGAdvancementProvider())));
        generator.addProvider(true, new CGModelProvider(packOutput, helper));
        generator.addProvider(true, CGBlockLootTableProvider.ModLootTableProvider.create(packOutput, lookupProvider));

        event.createProvider(CGDatapackProvider::Make);
        BlockTagsProvider blockTagsProvider = new CGBlockTagsProvider(packOutput, lookupProvider, helper);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new CGItemTagsProvider(packOutput, lookupProvider,
                                                           blockTagsProvider.contentsGetter(), helper));
        event.createProvider(CGEnchantmentTagsProvider::new);
    }
}
