package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Item.GraveCatalogue;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class CGItems
{
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CatalystGraves.MODID);
    //
    public static final DeferredItem<Item> GRAVE_CATALOGUE = ITEMS.register("catalogue",
                                                                            () -> new GraveCatalogue(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_0_ITEM = ITEMS.registerSimpleBlockItem("decorative_grave_simple", CGBlocks.DECORATIVE_GRAVE_0);

    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_1_ITEM = ITEMS.registerSimpleBlockItem("decorative_grave_basic", CGBlocks.DECORATIVE_GRAVE_1);

    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_2_ITEM = ITEMS.registerSimpleBlockItem("decorative_grave_tombstone", CGBlocks.DECORATIVE_GRAVE_2);

    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_3_ITEM = ITEMS.registerSimpleBlockItem("decorative_grave_royal", CGBlocks.DECORATIVE_GRAVE_3);
    //
    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
