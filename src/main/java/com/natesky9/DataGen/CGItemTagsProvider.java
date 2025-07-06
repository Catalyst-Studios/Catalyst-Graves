package com.natesky9.DataGen;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class CGItemTagsProvider extends ItemTagsProvider {
    public static TagKey<Item> SOULBOUND_APPLICABLE = ItemTags.create(ResourceLocation
            .fromNamespaceAndPath(CatalystGraves.MODID,"soulbound_applicable"));
    public CGItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
        super(output, lookupProvider,blockTags,CatalystGraves.MODID,helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(SOULBOUND_APPLICABLE)
                .addTag(Tags.Items.ENCHANTABLES)
                .addTag(Tags.Items.SHULKER_BOXES)
                .add(Items.BUNDLE);
    }
}
