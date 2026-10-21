package com.natesky9.catalystgraves.compat.curios;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.natesky9.catalystgraves.block.GraveBlockEntity;

@SuppressWarnings("null")
public class CuriosCompat
{

    public static void restoreCurios(Player player, NonNullList<ItemStack> savedItems)
    {
        if(savedItems.isEmpty()) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            AtomicInteger index = new AtomicInteger(0);
            handler.getCurios().forEach((id, stacksHandler) -> {
                IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                for(int i = 0; i < stackHandler.getSlots(); i++)
                {
                    if(index.get() < savedItems.size())
                    {
                        ItemStack stack = savedItems.get(index.getAndIncrement());
                        if(!stack.isEmpty())
                        {
                            ItemStack existing = stackHandler.getStackInSlot(i);

                            if(existing.isEmpty())
                            {
                                stackHandler.setStackInSlot(i, stack.copy());
                            }
                            else
                            {
                                ItemStack restoreStack = stack.copy();
                                player.getInventory().add(restoreStack);

                                if(!restoreStack.isEmpty())
                                {
                                    player.drop(restoreStack, false);
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    public static void snapCurios(Player player, List<ItemStack> curiosList)
    {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((id, stacksHandler) -> {
                for(int i = 0; i < stacksHandler.getStacks().getSlots(); i++)
                {
                    ItemStack s = stacksHandler.getStacks().getStackInSlot(i);
                    curiosList.add(s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy());
                }
            });
        });
    }

    public static void saveCurios(Map<UUID, List<ItemStack>> curiosSnapshot, Player player, List<ItemStack> curiosToCheck, GraveBlockEntity grave)
    {
        List<ItemStack> snappedCurios = curiosSnapshot.remove(player.getUUID());
        if(snappedCurios != null)
        {
            for(ItemStack stack : snappedCurios)
            {
                grave.addCurio(stack.copy());
                if(!stack.isEmpty())
                {
                    curiosToCheck.add(stack.copy());
                }
            }
        }
        else
        {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((id, stacksHandler) -> {
                    for(int i = 0; i < stacksHandler.getStacks().getSlots(); i++)
                    {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        grave.addCurio(stack.copy());
                        if(!stack.isEmpty()) curiosToCheck.add(stack.copy());
                        stacksHandler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                    }
                });
            });
        }
    }
}
