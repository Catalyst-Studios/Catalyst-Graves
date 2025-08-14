package com.natesky9.catalystgraves;

import com.natesky9.DataGen.CGAdvancementProvider;
import com.natesky9.catalystgraves.Block.SimpleGrave;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.Init.CGBlocks;
import com.natesky9.catalystgraves.Block.SimpleGraveEntity;
import com.natesky9.catalystgraves.Init.CGEnchantments;
import com.natesky9.catalystgraves.Init.CGItems;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.*;

public class GraveLogic extends SavedData {
    static GraveLogic instance;
    static Map<UUID,List<ItemStack>> soulboundItems = new HashMap<>();
    public static Map<UUID,List<ItemStack>> deathSnapshot = new HashMap<>();

    public static GraveLogic create()
    {
        //SavedData needs an instance, but we don't
        instance = new GraveLogic();
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        //save the death snapshot list to nbt.
        deathSnapshot.forEach((key, value) -> {
            ListTag snapshotList = new ListTag();
            CompoundTag snapshotCompound = new CompoundTag();
            snapshotCompound.putUUID("uuid",key);
            snapshotList.add(snapshotCompound);
            NonNullList<ItemStack> list = NonNullList.copyOf(value);
            ContainerHelper.saveAllItems(snapshotCompound,list,provider);
            compoundTag.put("Grave Snapshots",snapshotList);
        });
        soulboundItems.forEach((key, value) -> {
            ListTag soulboundList = new ListTag();
            CompoundTag soulboundCompound = new CompoundTag();
            soulboundCompound.putUUID("uuid",key);
            soulboundList.add(soulboundCompound);
            NonNullList<ItemStack> list = NonNullList.copyOf(value);
            ContainerHelper.saveAllItems(soulboundCompound,list,provider);
            compoundTag.put("Soulbounds",soulboundList);
        });
        return compoundTag;
    }
    public static GraveLogic load(CompoundTag tag, HolderLookup.Provider lookupProvider)
    {
        //load the contents of the nbt into the world,
        //so that the commands and even the re-equip will
        //work between world loads/server restarts
        GraveLogic graveLogic = GraveLogic.create();
        if (tag.contains("Grave Snapshots"))
        {
            ListTag graveList = tag.getList("Grave Snapshots", Tag.TAG_COMPOUND);
            for (int i=0;i<graveList.size();i++)
            {
                ListTag listtag = tag.getList("Items", Tag.TAG_COMPOUND);
                CompoundTag compoundTag = graveList.getCompound(i);
                NonNullList<ItemStack> nonNullList = NonNullList.withSize(listtag.size(),ItemStack.EMPTY);
                ContainerHelper.loadAllItems(compoundTag,nonNullList,lookupProvider);
                UUID uuid = compoundTag.getUUID("uuid");
                List<ItemStack> list = nonNullList.stream().toList();
                deathSnapshot.put(uuid,list);
            }
        }
        if (tag.contains("Soulbounds"))
        {
            ListTag soulboundList = tag.getList("Soulbounds", Tag.TAG_COMPOUND);
            for (int i=0;i<soulboundList.size();i++)
            {
                ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
                CompoundTag compoundTag = soulboundList.getCompound(i);
                NonNullList<ItemStack> nonNullList = NonNullList.withSize(listTag.size(),ItemStack.EMPTY);
                ContainerHelper.loadAllItems(compoundTag,nonNullList,lookupProvider);
                UUID uuid = compoundTag.getUUID("uuid");
                List<ItemStack> list = nonNullList.stream().toList();
                soulboundItems.put(uuid,list);
            }
        }
        return graveLogic;
    }


    public static void SnapshotInventory(LivingDeathEvent event)
    {
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUUID();
        Inventory inventory = player.getInventory();

        //get a snapshot of the players items, for command purposes
        //this also contains any soulbound items, just in case
        List<ItemStack> itemStacks = new ArrayList<>();
        if (inventory.getContainerSize() == 0)
        {
            /*System.out.println("Player had zero items! Unusual...");*/
            return;
        }
        for (int i=0; i<inventory.getContainerSize(); i++)
        {
            //
            ItemStack item = inventory.getItem(i);
            itemStacks.add(item.copy());
            //itemStacks.set(i,item);
        }
        //put the list
        deathSnapshot.put(uuid,itemStacks);
        /*System.out.println("stored items: " + itemStacks);*/
        instance.setDirty();
    }
    public static List<ItemStack> getSnapshot(ServerPlayer player)
    {
        List<ItemStack> list = deathSnapshot.get(player.getUUID());
        if (list == null)
            return List.of();
        return list;
    }

    public static void RestoreContents(Level level, Player player, List<ItemStack> items)
    {

        List<ItemStack> toFill = new java.util.ArrayList<>(List.of());
        List<ItemStack> chests = new java.util.ArrayList<>(List.of());
        ItemStack chest = new ItemStack(Items.CHEST);
        for (ItemStack item:items)
        {
            if (!item.isEmpty())
                toFill.add(item);
            if (toFill.size() == 27)
            {
                ItemContainerContents contents = ItemContainerContents.fromItems(toFill);
                chest.set(DataComponents.CONTAINER,contents);
                chests.add(chest);
                toFill.clear();
                chest = new ItemStack(Items.CHEST);
            }
        }
        if (!toFill.isEmpty())
        {
            ItemContainerContents contents = ItemContainerContents.fromItems(toFill);
            chest.set(DataComponents.CONTAINER,contents);
            chests.add(chest);
        }
        for (ItemStack filledChest:chests)
            Containers.dropItemStack(level,player.getX(),player.getY()+1,player.getZ(),filledChest);
    }
    public static void LivingDropsEvent(LivingDropsEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (event.getDrops().isEmpty()) return;

        //check for the first free, non-fluid location to place our grave
        //region broad search
        while (!player.level().isInWorldBounds(pos)) {
            boolean above = pos.getY() >= player.level().getMaxBuildHeight();
            boolean below = pos.getY() < player.level().getMinBuildHeight();
            pos = pos.relative(Direction.Axis.Y, (below ? 1 : 0) - (above ? 1 : 0));
        }
        BlockPos search = pos;
        while (pos.getY() < player.level().getMaxBuildHeight()) {
            Iterator<BlockPos.MutableBlockPos> iterator = BlockPos.spiralAround
                    (pos, 8, Direction.NORTH, Direction.EAST).iterator();

            search = iterator.next();

            while ((!player.level().getBlockState(search).canBeReplaced()
                    || player.level().isFluidAtPosition(search, FluidState::isSource))
                    && iterator.hasNext())
                search = iterator.next();
            if (player.level().getBlockState(search).canBeReplaced()
                    && !player.level().isFluidAtPosition(search, FluidState::isSource))
                break;
            pos = pos.above(1);
        }
        //endregion broad search

        if (!AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.SIMPLE_GRAVE))
            return;

        BlockState state = CGBlocks.SIMPLE_GRAVE.get().defaultBlockState()
                .setValue(SimpleGrave.GLOWING,AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ILLUMINATION))
                .setValue(SimpleGrave.STYLE,getGraveTier(player))
                .setValue(SimpleGrave.FACING,Direction.NORTH);

        level.setBlock(search, state,3);
        BlockEntity blockEntity = level.getBlockEntity(search);
        if (!(blockEntity instanceof SimpleGraveEntity grave)) return;

        //we set the player's actual death location to this position, so we can guaranteed use it later
        player.setLastDeathLocation(Optional.of(GlobalPos.of(level.dimension(),search)));

        List<ItemEntity> items = event.getDrops().stream().toList();
        UUID uuid = player.getUUID();
        List<ItemStack> freshItems = new ArrayList<>();
        grave.setUuid(uuid, player.getScoreboardName());

        for (ItemEntity entity:items)
        {
            //process the soulbound items, removing them from the list
            //before the grave collects them
            ItemStack stack = entity.getItem();
            boolean soulbound = stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(CGEnchantments.SOULBOUND));
            boolean nativeSoulbound = CGConfig.NATIVE_SOULBOUNDS.get().contains(stack.getItem().toString());
            if (nativeSoulbound || soulbound)
            {
                event.getDrops().remove(entity);
                freshItems.add(entity.getItem());
                continue;
            }
            grave.add(stack);
            event.getDrops().remove(entity);
        }
        if (soulboundItems.isEmpty())
            soulboundItems.put(uuid,freshItems);
        else
        {
            //I'm sure there's some edge case where the player might die twice
            //but this should catch that?
            List<ItemStack> combined = soulboundItems.get(uuid);
            combined.addAll(freshItems);
            soulboundItems.put(uuid,combined);
        }
    }
    static int getGraveTier(ServerPlayer player)
    {
        int graveTier = 0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ORGANIZATION) ? 1:0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.GREATER_VITALITY) ? 1:0;
        return graveTier;
    }
    public static void LivingRestoreEvent(PlayerEvent.Clone event)
    {
        //don't ask me why this is named this way
        //it filters out respawn events
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        List<ItemStack> items = soulboundItems.get(player.getUUID());
        if (items == null)
        {
            /*System.out.println("No items to restore!");*/
            return;
        }
        for (ItemStack stack:items)
        {
            //if the config option is enabled, remove the
            //soulbound enchantment from the restored item
            if (CGConfig.DOES_SOULBOUND_VANISH.get())
                EnchantmentHelper.updateEnchantments(stack,mutable ->
                        mutable.removeIf(holder -> holder.is(CGEnchantments.SOULBOUND)));
            player.addItem(stack);
            level.playSound(null,player.blockPosition(),SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
        }
        soulboundItems.remove(player.getUUID());
    }
    public static void giveCatalogue(PlayerWakeUpEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        //give the player the mod book if they have the Config item in their hand
        for (InteractionHand hand:InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CGConfig.TRANSMUTE_ITEM.get()));
            //System.out.println("The sleep item is: " + item);
            if (stack.is(Items.WRITABLE_BOOK))
            {
                //exit case for written books, in case they have contents.
                //you never know who's going to bed with one intentionally
                if (stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY) != WritableBookContent.EMPTY)
                {
                    player.displayClientMessage(Component.translatable("string.catalogue.fail"),true);
                    return;
                }
            }
            if (!stack.is(item)) continue;
            //now we can process the transmutation
            player.setItemInHand(hand, CGItems.GRAVE_CATALOGUE.toStack());
            player.displayClientMessage(Component.translatable("string.catalogue.aquisition"),true);
            player.playSound(SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
            AdvancementLogic.grant(player, CGAdvancementProvider.SIMPLE_GRAVE);
            //player now has the SIMPLE_GRAVE advancement
            return;


        }
    }



}
