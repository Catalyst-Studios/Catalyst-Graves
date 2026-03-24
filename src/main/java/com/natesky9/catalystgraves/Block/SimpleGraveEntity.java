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
    private Boolean glowing = false;

    public SimpleGraveEntity(BlockPos pos, BlockState blockState)
    {
        super(CGBlockEntities.SIMPLE_GRAVE.get(), pos, blockState);
    }

    public void setUuid(UUID inputUUID, String inputName)
    {
        uuid = inputUUID;
        name = inputName;
    }
    public UUID getUuid()
    {
        return uuid;
    }
    public String getName()
    {
        return name;
    }

    @SuppressWarnings("null")
    @Override
    protected void saveAdditional(@SuppressWarnings("null") CompoundTag tag, @SuppressWarnings("null") HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt("count", this.items.size());
        if(uuid != null) tag.putString("uuid", uuid.toString());
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        int count = tag.getInt("count");
        items = NonNullList.create();

        if(tag.contains("uuid"))
            uuid = UUID.fromString(tag.getString("uuid"));

        if(tag.contains("name"))
            name = tag.getString("name");

        glowing = tag.contains("glowing") && tag.getBoolean("glowing");

        NonNullList<ItemStack> temp = NonNullList.withSize(count, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, temp, registries);
        items.addAll(temp);

        if(tag.contains("curios_data"))
        {
            CompoundTag curiosTag = tag.getCompound("curios_data");
            int cCount = curiosTag.getInt("curios_count");
            this.curiosItems = NonNullList.withSize(cCount, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(curiosTag, curiosItems, registries);
        }
    }

    public NonNullList<ItemStack> getItems()
    {
        return items;
    }

    public void add(ItemStack stack)
    {
        items.add(stack);
    }

    public ItemStack remove()
    {
        return items.removeFirst();
    }

    public NonNullList<ItemStack> getCuriosItems()
    {
        return curiosItems;
    }

    public void addCurio(ItemStack stack)
    {
        this.curiosItems.add(stack);
    }

    public void setGraveContents(NonNullList<ItemStack> mainInv, NonNullList<ItemStack> curiosInv)
    {
        this.items = mainInv;
        this.curiosItems = curiosInv;
        this.setChanged();
    }

    @SuppressWarnings("null")
    public void dropItems()
    {
        SimpleContainer inventory = new SimpleContainer(items.size());
        for(ItemStack item : items)
            inventory.addItem(item);
        Containers.dropContents(getLevel(), getBlockPos(), inventory);
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
