package com.example.catalystgraves.Menus;

import com.example.catalystgraves.Init.ModMenus;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CatalogueMenu extends AbstractContainerMenu {
    public CatalogueMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, inventory);
    }
    public CatalogueMenu(int containerId, Inventory inventory)
    {
        super(ModMenus.CATALOGUE.get(), containerId);
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
