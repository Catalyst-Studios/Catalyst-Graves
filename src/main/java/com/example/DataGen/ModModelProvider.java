package com.example.DataGen;

import com.example.catalystgraves.CatalystGraves;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModModelProvider extends BlockStateProvider {
    public ModModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CatalystGraves.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        //this is where you can generate blockstates and models through datagen
        //not necessary if you generate assets with blockbench
        //simpleBlock(ModBlocks.SIMPLE_GRAVE.get());
    }
}
