package com.example.DataGen;

import com.example.catalystgraves.Init.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        //dropself adds the item version of the block to the loot table
        //any new block added has to have a loot table or else
        //dropSelf(ModBlocks.BLOCK.get());

        add(ModBlocks.SIMPLE_GRAVE.get(),noDrop());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        //this is the bit of code that will yell at you for forgetting to include a loot table
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value).toList();
    }
    //boilerplate code, can safely be ignored
    public static class ModLootTableProvider{
        public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> provider)
        {
            return new LootTableProvider(output, Set.of(),
                    List.of(new LootTableProvider.SubProviderEntry(
                            ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), provider);
        }
    }
}
