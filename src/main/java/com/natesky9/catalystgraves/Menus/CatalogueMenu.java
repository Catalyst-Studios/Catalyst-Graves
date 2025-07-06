package com.natesky9.catalystgraves.Menus;

import com.natesky9.catalystgraves.Init.CGMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CatalogueMenu extends AbstractContainerMenu {
    public CatalogueMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, inventory);
    }
    public CatalogueMenu(int containerId, Inventory inventory)
    {
        super(CGMenus.CATALOGUE.get(), containerId);
    }


    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
