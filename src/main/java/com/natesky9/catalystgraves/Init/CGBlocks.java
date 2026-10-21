package com.natesky9.catalystgraves.init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.block.DecorativeGrave;
import com.natesky9.catalystgraves.block.GraveBlock;
import com.natesky9.catalystgraves.block.GraveStyles;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class CGBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(CatalystGraves.MODID);

    public static final DeferredBlock<GraveBlock> SIMPLE_GRAVE = BLOCKS.register("simple_grave",
        () -> new GraveBlock(BlockBehaviour.Properties.of()
            .noOcclusion()
            .lightLevel(state -> state.getValue(GraveBlock.GLOWING) ? 15 : 0)
            .strength(-1.0F, 3600000.0F)));

    private static final List<DeferredBlock<DecorativeGrave>> DECORATIVE_GRAVES = new ArrayList<>();
    public static final DeferredBlock<DecorativeGrave> DECORATIVE_GRAVE_0;
    public static final DeferredBlock<DecorativeGrave> DECORATIVE_GRAVE_1;
    public static final DeferredBlock<DecorativeGrave> DECORATIVE_GRAVE_2;
    public static final DeferredBlock<DecorativeGrave> DECORATIVE_GRAVE_3;

    static
    {
        for (GraveStyles.StyleDefinition style : GraveStyles.getStyles())
        {
            String registryName = "decorative_grave_" + style.name();
            final int styleId = style.id();
            DeferredBlock<DecorativeGrave> block = BLOCKS.register(
                registryName,
                () -> new DecorativeGrave(BlockBehaviour.Properties.of().noOcclusion().strength(2.0f), styleId)
            );
            DECORATIVE_GRAVES.add(block);
        }

        DECORATIVE_GRAVE_0 = DECORATIVE_GRAVES.get(0);
        DECORATIVE_GRAVE_1 = DECORATIVE_GRAVES.get(1);
        DECORATIVE_GRAVE_2 = DECORATIVE_GRAVES.get(2);
        DECORATIVE_GRAVE_3 = DECORATIVE_GRAVES.get(3);
    }

    public static List<DeferredBlock<DecorativeGrave>> getDecorativeGraves()
    {
        return Collections.unmodifiableList(DECORATIVE_GRAVES);
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
