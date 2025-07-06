package com.example.catalystgraves.Init;

import com.example.catalystgraves.Block.SimpleGraveEntity;
import com.example.catalystgraves.CatalystGraves;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CatalystGraves.MODID);
    //
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleGraveEntity>> SIMPLE_GRAVE =
            BLOCK_ENTITIES.register("simple_grave",
                    () -> BlockEntityType.Builder.of(SimpleGraveEntity::new,
                            ModBlocks.SIMPLE_GRAVE.get()).build(null));
    //
    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITIES.register(eventBus);
    }
}
