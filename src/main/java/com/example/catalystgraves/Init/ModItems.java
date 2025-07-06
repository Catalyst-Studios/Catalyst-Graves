package com.example.catalystgraves.Init;

import com.example.catalystgraves.CatalystGraves;
import com.example.catalystgraves.Item.GraveCatalogue;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CatalystGraves.MODID);
    //
    public static final DeferredItem<Item> GRAVE_CATALOGUE = ITEMS.register("catalogue",
            () -> new GraveCatalogue(new Item.Properties()
                    .stacksTo(1)));
    //
    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
