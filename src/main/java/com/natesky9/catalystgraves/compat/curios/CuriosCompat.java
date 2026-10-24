package com.natesky9.catalystgraves.compat.curios;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.natesky9.catalystgraves.block.GraveBlockEntity;

@SuppressWarnings("null")
public class CuriosCompat
{
    private static final String TAG_IDENTIFIER = "CG_CurioIdentifier";
    private static final String TAG_SLOT = "CG_CurioSlot";

    private static void attachSlotTags(ItemStack stack, String identifier, int slotIndex)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_IDENTIFIER, identifier);
            tag.putInt(TAG_SLOT, slotIndex);
        });
    }

    private static void removeSlotTags(ItemStack stack)
    {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if(customData != null)
        {
            CompoundTag tag = customData.copyTag();
            tag.remove(TAG_IDENTIFIER);
            tag.remove(TAG_SLOT);

            if(tag.isEmpty())
            {
                stack.remove(DataComponents.CUSTOM_DATA);
            }
            else
            {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    private static String getSlotIdentifier(ItemStack stack)
    {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if(customData != null)
        {
            CompoundTag tag = customData.copyTag();
            if(tag.contains(TAG_IDENTIFIER))
            {
                return tag.getString(TAG_IDENTIFIER);
            }
        }
        return null;
    }

    private static int getSlotIndex(ItemStack stack)
    {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if(customData != null)
        {
            CompoundTag tag = customData.copyTag();
            if(tag.contains(TAG_SLOT))
            {
                return tag.getInt(TAG_SLOT);
            }
        }
        return -1;
    }

    public static void restoreCurios(Player player, NonNullList<ItemStack> savedItems)
    {
        if(savedItems == null || savedItems.isEmpty()) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            Map<String, ICurioStacksHandler> curiosMap = handler.getCurios();

            for(ItemStack savedStack : savedItems)
            {
                if(savedStack == null || savedStack.isEmpty()) continue;

                ItemStack stackToRestore = savedStack.copy();
                String targetIdentifier = getSlotIdentifier(stackToRestore);
                int targetSlot = getSlotIndex(stackToRestore);
                removeSlotTags(stackToRestore);

                ICurioStacksHandler sh0 = targetIdentifier == null ? null : curiosMap.get(targetIdentifier);

                boolean restored = false;

                if(sh0 != null)
                {
                    IDynamicStackHandler h = sh0.getStacks();

                    if(targetSlot >= 0 && targetSlot < h.getSlots() && h.getStackInSlot(targetSlot).isEmpty() && isValidFor(targetIdentifier, player, targetSlot, stackToRestore))
                    {
                        h.setStackInSlot(targetSlot, stackToRestore);
                        restored = true;
                    }

                    if(!restored)
                    {
                        for(int i = 0; i < h.getSlots(); i++)
                        {
                            if(h.getStackInSlot(i).isEmpty() && isValidFor(targetIdentifier, player, i, stackToRestore))
                            {
                                h.setStackInSlot(i, stackToRestore);
                                restored = true;
                                break;
                            }
                        }
                    }
                }

                if(!restored)
                {
                    player.getInventory().add(stackToRestore);
                    if(!stackToRestore.isEmpty())
                    {
                        player.drop(stackToRestore, false);
                    }
                }
            }
        });
    }

    private static boolean isValidFor(String id, Player player, int slot, ItemStack stack)
    {
        try
        {
            SlotContext ctx = new SlotContext(id, (LivingEntity)player, slot, false, true);
            boolean ok = CuriosApi.isStackValid(ctx, stack);
            return ok;
        }
        catch(Throwable t)
        {
            return true;
        }
    }

    public static void snapCurios(Player player, List<ItemStack> curiosList)
    {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((id, stacksHandler) -> {
                IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                for(int i = 0; i < stackHandler.getSlots(); i++)
                {
                    ItemStack s = stackHandler.getStackInSlot(i);
                    if(s != null && !s.isEmpty())
                    {
                        ItemStack copy = s.copy();
                        attachSlotTags(copy, id, i);
                        curiosList.add(copy);
                    }
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
                if(stack != null && !stack.isEmpty())
                {
                    grave.addCurio(stack.copy());

                    ItemStack checkStack = stack.copy();
                    removeSlotTags(checkStack);
                    curiosToCheck.add(checkStack);
                }
            }
        }
        else
        {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((id, stacksHandler) -> {
                    IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                    for(int i = 0; i < stackHandler.getSlots(); i++)
                    {
                        ItemStack stack = stackHandler.getStackInSlot(i);
                        if(stack != null && !stack.isEmpty())
                        {
                            ItemStack tagged = stack.copy();
                            attachSlotTags(tagged, id, i);
                            grave.addCurio(tagged);

                            curiosToCheck.add(stack.copy());
                            stackHandler.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                });
            });
        }
    }
}
