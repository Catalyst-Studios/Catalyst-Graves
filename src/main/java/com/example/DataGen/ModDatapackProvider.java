package com.example.DataGen;

import com.example.catalystgraves.CatalystGraves;
import com.example.catalystgraves.Init.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDatapackProvider {
    public static DatapackBuiltinEntriesProvider Make(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {

        final RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
        return new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, builder, Set.of(CatalystGraves.MODID));
    }
}
