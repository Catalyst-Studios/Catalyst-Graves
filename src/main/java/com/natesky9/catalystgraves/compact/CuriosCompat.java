package com.natesky9.catalystgraves.compact;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("null")
public class CuriosCompat
{
    public static final String MODID = "curios";

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded(MODID);
    }

    public static void restoreCurios(Player player, NonNullList<ItemStack> savedItems)
    {
        if(!isLoaded() || savedItems.isEmpty()) return;

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
                            stackHandler.setStackInSlot(i, stack.copy());
                        }
                    }
                }
            });
        });
    }
}