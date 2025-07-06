package com.example.DataGen;

import com.example.catalystgraves.CatalystGraves;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DataGenerators {
    public static void gatherData(GatherDataEvent event)
    {
        //datagen is a lifesaver
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(true, new AdvancementProvider(packOutput, lookupProvider, helper, List.of(new ModAdvancementProvider())));
        generator.addProvider(true, new ModModelProvider(packOutput,helper));
        generator.addProvider(true, ModBlockLootTableProvider.ModLootTableProvider.create(packOutput,lookupProvider));

        event.createProvider(ModDatapackProvider::Make);
        BlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(packOutput,lookupProvider,helper);
        generator.addProvider(true, blockTagsProvider);
        generator.addProvider(true, new ModItemTagsProvider(packOutput,lookupProvider,
                blockTagsProvider.contentsGetter(),helper));
        event.createProvider(ModEnchantmentTagsProvider::new);
    }
}
