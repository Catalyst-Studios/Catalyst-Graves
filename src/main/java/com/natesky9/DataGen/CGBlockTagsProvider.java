package com.natesky9.DataGen;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class CGBlockTagsProvider extends BlockTagsProvider {
    public CGBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CatalystGraves.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

    }
}
