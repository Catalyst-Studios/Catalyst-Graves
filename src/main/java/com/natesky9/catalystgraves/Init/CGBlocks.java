package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.Block.SimpleGrave;
import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class CGBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(CatalystGraves.MODID);
    //
    public static final DeferredBlock<Block> SIMPLE_GRAVE = BLOCKS.register("simple_grave",
                                                                            () -> new SimpleGrave(BlockBehaviour.Properties.of().noOcclusion().lightLevel(state -> state.getValue(SimpleGrave.GLOWING) ? 15 : 0).strength(-1.0F, 3600000.0F)));
    //
    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
