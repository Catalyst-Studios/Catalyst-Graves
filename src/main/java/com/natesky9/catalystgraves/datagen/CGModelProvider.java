package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Init.CGBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

@SuppressWarnings("null")
public class CGModelProvider extends BlockStateProvider
{
    public CGModelProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, CatalystGraves.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels()
    {

        registerGrave(CGBlocks.DECORATIVE_GRAVE_0.get(), "grave_1");
        registerGrave(CGBlocks.DECORATIVE_GRAVE_1.get(), "grave_2");
        registerGrave(CGBlocks.DECORATIVE_GRAVE_2.get(), "grave_3");
        registerGrave(CGBlocks.DECORATIVE_GRAVE_3.get(), "grave_4");
    }

    private void registerGrave(Block block, String modelName)
    {
        ModelFile graveModel = models().getExistingFile(modLoc("block/" + modelName));

        horizontalBlock(block, graveModel);

        simpleBlockItem(block, graveModel);
    }
}