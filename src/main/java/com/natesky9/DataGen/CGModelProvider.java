package com.natesky9.DataGen;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

public class CGModelProvider extends ModelProvider {
    public CGModelProvider(PackOutput output) {
        super(output, CatalystGraves.MODID);
    }
}
