package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.block.DecorativeGrave;
import com.natesky9.catalystgraves.init.CGBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;

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
        List<DeferredBlock<DecorativeGrave>> graves = CGBlocks.getDecorativeGraves();
        for (int i = 0; i < graves.size(); i++)
        {
            registerGrave(graves.get(i).get(), "grave_" + (i + 1));
        }
    }

    private void registerGrave(Block block, String modelName)
    {
        ModelFile graveModel = models().getExistingFile(modLoc("block/" + modelName));
        horizontalBlock(block, graveModel);
        simpleBlockItem(block, graveModel);
    }
}