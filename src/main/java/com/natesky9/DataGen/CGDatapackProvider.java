package com.natesky9.DataGen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CGDatapackProvider {
    public static DatapackBuiltinEntriesProvider Make(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {

        final RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.ENCHANTMENT, CGEnchantments::bootstrap);
        return new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, builder, Set.of(CatalystGraves.MODID));
    }
}
