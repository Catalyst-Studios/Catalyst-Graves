package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class CGCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CatalystGraves.MODID);
    //
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
                                                                                                        .title(Component.translatable("itemGroup.catalystgraves"))
                                                                                                        .icon(Items.SKELETON_SKULL::getDefaultInstance)
                                                                                                        .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
                                                                                                        .displayItems(((itemDisplayParameters, output) -> {
                                                                                                            // add in items to display here
                                                                                                            output.accept(CGItems.GRAVE_CATALOGUE);
                                                                                                            output.accept(CGBlocks.DECORATIVE_GRAVE_0.get());
                                                                                                            output.accept(CGBlocks.DECORATIVE_GRAVE_1.get());
                                                                                                            output.accept(CGBlocks.DECORATIVE_GRAVE_2.get());
                                                                                                            output.accept(CGBlocks.DECORATIVE_GRAVE_3.get());
                                                                                                        }))
                                                                                                        .build());
    //
    public static void register(IEventBus eventBus)
    {
        TABS.register(eventBus);
    }
}