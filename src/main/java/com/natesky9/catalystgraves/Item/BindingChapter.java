package com.natesky9.catalystgraves.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import com.natesky9.catalystgraves.client.menus.BindingMenu;

public class BindingChapter implements MenuProvider
{
    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.enchanting");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player)
    {
        return new BindingMenu(i, inventory);
    }
}
