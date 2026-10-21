package com.natesky9.catalystgraves.init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.block.DecorativeGrave;
import com.natesky9.catalystgraves.block.GraveStyles;
import com.natesky9.catalystgraves.item.GraveCatalogue;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class CGItems
{
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CatalystGraves.MODID);

    public static final DeferredItem<Item> GRAVE_CATALOGUE = ITEMS.register("catalogue",
        () -> new GraveCatalogue(new Item.Properties().stacksTo(1)));

    private static final List<DeferredItem<BlockItem>> DECORATIVE_GRAVE_ITEMS = new ArrayList<>();

    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_0_ITEM;
    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_1_ITEM;
    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_2_ITEM;
    public static final DeferredItem<BlockItem> DECORATIVE_GRAVE_3_ITEM;

    static
    {
        List<DeferredBlock<DecorativeGrave>> decBlocks = CGBlocks.getDecorativeGraves();
        List<GraveStyles.StyleDefinition> styles = GraveStyles.getStyles();

        for (int i = 0; i < styles.size(); i++)
        {
            GraveStyles.StyleDefinition style = styles.get(i);
            DeferredBlock<DecorativeGrave> block = decBlocks.get(i);
            String name = "decorative_grave_" + style.name();
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
            DECORATIVE_GRAVE_ITEMS.add(item);
        }

        DECORATIVE_GRAVE_0_ITEM = DECORATIVE_GRAVE_ITEMS.get(0);
        DECORATIVE_GRAVE_1_ITEM = DECORATIVE_GRAVE_ITEMS.get(1);
        DECORATIVE_GRAVE_2_ITEM = DECORATIVE_GRAVE_ITEMS.get(2);
        DECORATIVE_GRAVE_3_ITEM = DECORATIVE_GRAVE_ITEMS.get(3);
    }

    public static List<DeferredItem<BlockItem>> getDecorativeGraveItems()
    {
        return Collections.unmodifiableList(DECORATIVE_GRAVE_ITEMS);
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
