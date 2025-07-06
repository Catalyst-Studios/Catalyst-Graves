package com.example.catalystgraves.Init;

import com.example.catalystgraves.CatalystGraves;
import com.example.catalystgraves.Menus.BindingMenu;
import com.example.catalystgraves.Menus.CatalogueMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CatalystGraves.MODID);
    //
    public static final DeferredHolder<MenuType<?>,MenuType<CatalogueMenu>> CATALOGUE =
            MENUS.register("catalogue",
                    () -> IMenuTypeExtension.create(CatalogueMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<BindingMenu>> BINDING =
            MENUS.register("binding",
                    () -> IMenuTypeExtension.create(BindingMenu::new));
    //
    public static void register(IEventBus eventBus)
    {
        MENUS.register(eventBus);
    }
}
