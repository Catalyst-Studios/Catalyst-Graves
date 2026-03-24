package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.client.menus.BindingMenu;
import com.natesky9.catalystgraves.client.menus.CatalogueMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class CGMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, CatalystGraves.MODID);
    //
    public static final DeferredHolder<MenuType<?>, MenuType<CatalogueMenu>> CATALOGUE =
        MENUS.register("catalogue",
                       () -> IMenuTypeExtension.create(CatalogueMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BindingMenu>> BINDING =
        MENUS.register("binding",
                       () -> IMenuTypeExtension.create(BindingMenu::new));
    //
    public static void register(IEventBus eventBus)
    {
        MENUS.register(eventBus);
    }
}
