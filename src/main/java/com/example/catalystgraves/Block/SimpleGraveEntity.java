package com.example.catalystgraves.Block;

import com.example.catalystgraves.Init.ModBlockEntities;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimpleGraveEntity extends BlockEntity {
    private NonNullList<ItemStack> items;
    private UUID uuid;
    private String name;
    private Boolean glowing;
    public SimpleGraveEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SIMPLE_GRAVE.get(), pos, blockState);
        items = NonNullList.create();
        name = "Johnny";
        glowing = false;
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
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("count",this.items.size());
        if (uuid != null)
            tag.putString("uuid",uuid.toString());
        if (name != null)
            tag.putString("name",name);
        tag.putBoolean("glowing",glowing);
        ContainerHelper.saveAllItems(tag,this.items,registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int count = tag.getInt("count");
        items = NonNullList.create();
        if (tag.contains("uuid"))
            uuid = UUID.fromString(tag.getString("uuid"));
        if (tag.contains("name"))
            name = tag.getString("name");
        glowing = tag.contains("glowing") && tag.getBoolean("glowing");
        NonNullList<ItemStack> temp = NonNullList.withSize(count, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag,temp,registries);
        items.addAll(temp);
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
    public void dropItems()
    {
        SimpleContainer inventory = new SimpleContainer(items.size());
        for (ItemStack item:items)
            inventory.addItem(item);
        Containers.dropContents(getLevel(),getBlockPos(),inventory);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        //we need this and #getUpdateTag to send blockentity data to the client
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
