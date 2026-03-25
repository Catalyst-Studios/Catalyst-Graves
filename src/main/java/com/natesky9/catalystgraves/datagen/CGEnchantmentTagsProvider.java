package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class CGEnchantmentTagsProvider extends EnchantmentTagsProvider
{
    public static TagKey<Enchantment> VANISHING_EXCLUSIVE = TagKey.create(Registries.ENCHANTMENT,
                                                                          ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "exclusive_set/vanishing"));

    public CGEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        tag(VANISHING_EXCLUSIVE)
            .add(Enchantments.VANISHING_CURSE)
            .addOptional(CGEnchantments.SOULBOUND.location());
    }
}
