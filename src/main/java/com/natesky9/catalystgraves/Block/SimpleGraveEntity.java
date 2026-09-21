package com.natesky9.catalystgraves.Block;

import com.natesky9.catalystgraves.Init.CGBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimpleGraveEntity extends BlockEntity
{
    private NonNullList<ItemStack> items = NonNullList.create();
    private NonNullList<ItemStack> curiosItems = NonNullList.create();
    private UUID uuid;
    private String name = "Johnny";
    private boolean glowing = false;
    private boolean vitalityClaimed = false;

    public SimpleGraveEntity(BlockPos pos, BlockState blockState)
    {
        super(CGBlockEntities.SIMPLE_GRAVE.get(), pos, blockState);
    }

    public synchronized void setUuid(UUID inputUUID, String inputName)
    {
        this.uuid = inputUUID;
        this.name = inputName;
        this.setChanged();
    }

    public synchronized UUID getUuid()
    {
        return uuid;
    }

    public synchronized String getName()
    {
        return name;
    }

    public synchronized boolean isVitalityClaimed()
    {
        return vitalityClaimed;
    }

    public synchronized void setVitalityClaimed(boolean claimed)
    {
        this.vitalityClaimed = claimed;
        this.setChanged();
    }

    public synchronized boolean isGlowing()
    {
        return glowing;
    }

    public synchronized void setGlowing(boolean glowing)
    {
        this.glowing = glowing;
        this.setChanged();
    }

    @SuppressWarnings("null")
    @Override
    protected synchronized void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt("count", this.items.size());
        tag.putBoolean("VitalityClaimed", this.vitalityClaimed);
        if(uuid != null) tag.putUUID("uuid", uuid);
        if(name != null) tag.putString("name", name);
        tag.putBoolean("glowing", glowing);
        ContainerHelper.saveAllItems(tag, this.items, registries);

        if(!curiosItems.isEmpty())
        {
            CompoundTag curiosTag = new CompoundTag();
            curiosTag.putInt("curios_count", curiosItems.size());
            ContainerHelper.saveAllItems(curiosTag, curiosItems, registries);
            tag.put("curios_data", curiosTag);
        }
    }

    @SuppressWarnings("null")
    @Override
    protected synchronized void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.vitalityClaimed = tag.getBoolean("VitalityClaimed");
        int count = tag.getInt("count");
        this.items = NonNullList.create();

        if(tag.hasUUID("uuid"))
        {
            this.uuid = tag.getUUID("uuid");
        }
        else if(tag.contains("uuid"))
        {
            this.uuid = UUID.fromString(tag.getString("uuid"));
        }

        if(tag.contains("name"))
            this.name = tag.getString("name");

        this.glowing = tag.getBoolean("glowing");

        if(count > 0)
        {
            NonNullList<ItemStack> temp = NonNullList.withSize(count, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, temp, registries);
            this.items.addAll(temp);
        }

        this.curiosItems = NonNullList.create();
        if(tag.contains("curios_data"))
        {
            CompoundTag curiosTag = tag.getCompound("curios_data");
            int cCount = curiosTag.getInt("curios_count");
            if(cCount > 0)
            {
                NonNullList<ItemStack> tempCurios = NonNullList.withSize(cCount, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(curiosTag, tempCurios, registries);
                this.curiosItems.addAll(tempCurios);
            }
        }
    }

    public synchronized NonNullList<ItemStack> getItems()
    {
        return items;
    }

    public synchronized void add(ItemStack stack)
    {
        if(!stack.isEmpty())
        {
            items.add(stack);
            setChanged();
        }
    }

    public synchronized ItemStack remove()
    {
        if(items.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.removeFirst();
        setChanged();
        return removed;
    }

    public synchronized NonNullList<ItemStack> getCuriosItems()
    {
        return curiosItems;
    }

    public synchronized void addCurio(ItemStack stack)
    {
        if(!stack.isEmpty())
        {
            this.curiosItems.add(stack);
            setChanged();
        }
    }

    public synchronized void setGraveContents(NonNullList<ItemStack> mainInv, NonNullList<ItemStack> curiosInv)
    {
        this.items = mainInv;
        this.curiosItems = curiosInv;
        this.setChanged();
    }

    @SuppressWarnings("null")
    public synchronized void dropItems()
    {
        if(getLevel() == null || getLevel().isClientSide) return;

        int totalSize = items.size() + curiosItems.size();
        if(totalSize == 0) return;

        SimpleContainer inventory = new SimpleContainer(totalSize);
        for(ItemStack item : items)
        {
            if(!item.isEmpty()) inventory.addItem(item.copy());
        }
        for(ItemStack curio : curiosItems)
        {
            if(!curio.isEmpty()) inventory.addItem(curio.copy());
        }

        Containers.dropContents(getLevel(), getBlockPos(), inventory);
        items.clear();
        curiosItems.clear();
        setChanged();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        // we need this and #getUpdateTag to send blockentity data to the client
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("null")
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        return saveWithoutMetadata(registries);
    }
}
