package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.block.DecorativeGrave;
import com.natesky9.catalystgraves.init.CGBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class CGBlockLootTableProvider extends BlockLootSubProvider
{
    public CGBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate()
    {
        add(CGBlocks.SIMPLE_GRAVE.get(), noDrop());

        for (DeferredBlock<DecorativeGrave> block : CGBlocks.getDecorativeGraves())
        {
            dropSelf(block.get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return CGBlocks.BLOCKS.getEntries().stream().map(Holder::value).toList();
    }

    public static class ModLootTableProvider
    {
        public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> provider)
        {
            return new LootTableProvider(output, Set.of(),
                                         List.of(new LootTableProvider.SubProviderEntry(
                                             CGBlockLootTableProvider::new, LootContextParamSets.BLOCK)),
                                         provider);
        }
    }
}
