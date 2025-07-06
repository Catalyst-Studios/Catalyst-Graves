package com.example.DataGen;

import com.example.catalystgraves.CatalystGraves;
import com.example.catalystgraves.Init.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public static TagKey<Enchantment> VANISHING_EXCLUSIVE = TagKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,"exclusive_set/vanishing"));

    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(VANISHING_EXCLUSIVE)
                .add(Enchantments.VANISHING_CURSE)
                .addOptional(ModEnchantments.SOULBOUND.location());
    }
}
