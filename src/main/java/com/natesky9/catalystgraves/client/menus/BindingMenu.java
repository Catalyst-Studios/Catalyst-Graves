package com.natesky9.catalystgraves.client.menus;

import com.natesky9.catalystgraves.Init.CGMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class BindingMenu extends AbstractContainerMenu
{
    public int windup = 0;
    public int clock = 0;

    public BindingMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, inventory);
    }

    public BindingMenu(int containerId, Inventory inventory)
    {
        super(CGMenus.BINDING.get(), containerId);
        addPlayerInventory(inventory);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
        return;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    void addPlayerInventory(Inventory playerInventory)
    {
        // something something magic numbers
        for(int i = 0; i < 9; ++i)
        {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        // add the player inventory, only accounting for vanilla size for now
        for(int i = 0; i < 3; ++i)
        {
            for(int l = 0; l < 9; ++l)
            {
                addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }
}
