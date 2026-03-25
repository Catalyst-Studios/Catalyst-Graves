package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class CGBlockTagsProvider extends BlockTagsProvider
{
    public CGBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, CatalystGraves.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        this.tag(BlockTags.WITHER_IMMUNE)
            .add(CGBlocks.SIMPLE_GRAVE.get());

        this.tag(BlockTags.DRAGON_IMMUNE)
            .add(CGBlocks.SIMPLE_GRAVE.get());

        TagKey<Block> cardboardBlacklist = BlockTags.create(ResourceLocation.fromNamespaceAndPath("mekanism", "cardboard_blacklist"));
        
        this.tag(cardboardBlacklist)
            .add(CGBlocks.SIMPLE_GRAVE.get());
    }
}