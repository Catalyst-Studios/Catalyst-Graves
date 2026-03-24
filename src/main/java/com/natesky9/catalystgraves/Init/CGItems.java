package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Item.GraveCatalogue;
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
    //
    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
