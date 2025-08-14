package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.Item.GraveCatalogue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CGItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CatalystGraves.MODID);
    //
    public static final DeferredItem<Item> GRAVE_CATALOGUE = ITEMS.register("catalogue",
            () -> new GraveCatalogue(new Item.Properties()
                    .stacksTo(1)
                    .setId(location("catalogue"))));
    //
    static ResourceKey<Item> location(String string)
    {
        return ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,string));
    }
    //
    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
