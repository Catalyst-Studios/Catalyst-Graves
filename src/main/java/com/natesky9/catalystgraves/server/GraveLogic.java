package com.natesky9.catalystgraves.server;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.block.GraveBlock;
import com.natesky9.catalystgraves.block.GraveBlockEntity;
import com.natesky9.catalystgraves.compat.curios.CuriosCompat;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.natesky9.catalystgraves.init.CGBlocks;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.init.CGEnchantments;
import com.natesky9.catalystgraves.init.CGItems;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("null")
public class GraveLogic extends SavedData
{
    public static GraveLogic instance;
    public static final Map<UUID, List<ItemStack>> soulboundItems = new ConcurrentHashMap<>();
    public static final Map<UUID, List<ItemStack>> deathSnapshot = new ConcurrentHashMap<>();
    public static final Map<UUID, List<ItemStack>> curiosSnapshot = new ConcurrentHashMap<>();
    public static final Map<UUID, List<GlobalPos>> activeGraves = new ConcurrentHashMap<>();
    public static final Map<GlobalPos, Integer> graveXp = new ConcurrentHashMap<>();

    public static GraveLogic create()
    {
        instance = new GraveLogic();
        return instance;
    }

    static int getGraveTier(ServerPlayer player)
    {
        if(RoyalGrave.contains(player)) return 3;
        int graveTier = 0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ORGANIZATION) ? 1 : 0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.GREATER_VITALITY) ? 1 : 0;
        return graveTier;
    }

    public static int calculateTotalXp(Player player)
    {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        int base;
        if(level <= 16)
        {
            base = level * level + 6 * level;
        }
        else if(level <= 31)
        {
            base = (int)(2.5 * level * level - 40.5 * level + 360);
        }
        else
        {
            base = (int)(4.5 * level * level - 162.5 * level + 2220);
        }

        int xpForNextLevel = player.getXpNeededForNextLevel();
        return base + Math.round(progress * xpForNextLevel);
    }
    
    public static int calculateRetainedXp(ServerPlayer player)
    {
        int totalXp = calculateTotalXp(player);
        double retentionRate = player.experienceLevel <= 100 ? 0.75 : 0.90;
        return (int)Math.round(totalXp * retentionRate);
    }

    public static boolean isSoulbound(ItemStack stack)
    {
        if(stack == null || stack.isEmpty()) return false;
        if(isBlacklistedFromSoulbound(stack)) return false;

        return stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(CGEnchantments.SOULBOUND));
    }

    public static boolean isBlacklistedFromSoulbound(ItemStack stack)
    {
        try
        {
            List<? extends String> blacklist = CGConfig.SOULBOUND_BLACKLIST.get();
            if(blacklist == null || blacklist.isEmpty()) return false;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String itemIdStr = itemId.toString();

            for(String entry : blacklist)
            {
                if(entry == null || entry.isBlank()) continue;
                String trimmed = entry.trim();
                if(trimmed.startsWith("#"))
                {
                    ResourceLocation tagLoc = ResourceLocation.tryParse(trimmed.substring(1));
                    if(tagLoc != null)
                    {
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                        if(stack.is(tagKey)) return true;
                    }
                }
                else
                {
                    if(trimmed.equalsIgnoreCase(itemIdStr)) return true;
                }
            }
        }
        catch(Exception e)
        {
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider)
    {
        ListTag snapshotListTag = new ListTag();
        deathSnapshot.forEach((key, value) -> {
            try
            {
                CompoundTag snapshotCompound = new CompoundTag();
                snapshotCompound.putUUID("uuid", key);
                NonNullList<ItemStack> list = NonNullList.copyOf(value);
                ContainerHelper.saveAllItems(snapshotCompound, list, provider);
                snapshotListTag.add(snapshotCompound);
            }
            catch(Exception ignored)
            {
            }
        });
        compoundTag.put("Grave Snapshots", snapshotListTag);

        ListTag soulboundListTag = new ListTag();
        soulboundItems.forEach((key, value) -> {
            try
            {
                CompoundTag soulboundCompound = new CompoundTag();
                soulboundCompound.putUUID("uuid", key);
                NonNullList<ItemStack> list = NonNullList.copyOf(value);
                ContainerHelper.saveAllItems(soulboundCompound, list, provider);
                soulboundListTag.add(soulboundCompound);
            }
            catch(Exception ignored)
            {
            }
        });
        compoundTag.put("Soulbounds", soulboundListTag);

        ListTag gravesListTag = new ListTag();
        activeGraves.forEach((key, positions) -> {
            try
            {
                CompoundTag playerGravesTag = new CompoundTag();
                playerGravesTag.putUUID("uuid", key);

                ListTag posList = new ListTag();
                synchronized(positions)
                {
                    for(GlobalPos pos : positions)
                    {
                        CompoundTag posTag = new CompoundTag();
                        posTag.putString("dimension", pos.dimension().location().toString());
                        posTag.putInt("x", pos.pos().getX());
                        posTag.putInt("y", pos.pos().getY());
                        posTag.putInt("z", pos.pos().getZ());
                        posList.add(posTag);
                    }
                }
                playerGravesTag.put("positions", posList);
                gravesListTag.add(playerGravesTag);
            }
            catch(Exception ignored)
            {
            }
        });
        compoundTag.put("ActiveGraves", gravesListTag);

        ListTag xpListTag = new ListTag();
        graveXp.forEach((pos, xp) -> {
            try
            {
                CompoundTag xpTag = new CompoundTag();
                xpTag.putString("dimension", pos.dimension().location().toString());
                xpTag.putInt("x", pos.pos().getX());
                xpTag.putInt("y", pos.pos().getY());
                xpTag.putInt("z", pos.pos().getZ());
                xpTag.putInt("xp", xp);
                xpListTag.add(xpTag);
            }
            catch(Exception ignored)
            {
            }
        });
        compoundTag.put("GraveXp", xpListTag);

        return compoundTag;
    }

    public static GraveLogic load(CompoundTag tag, HolderLookup.Provider lookupProvider)
    {
        GraveLogic graveLogic = GraveLogic.create();

        if(tag.contains("Grave Snapshots"))
        {
            ListTag graveList = tag.getList("Grave Snapshots", Tag.TAG_COMPOUND);
            for(int i = 0; i < graveList.size(); i++)
            {
                try
                {
                    CompoundTag compoundTag = graveList.getCompound(i);
                    ListTag listTag = compoundTag.contains("Items", Tag.TAG_LIST) ? compoundTag.getList("Items", Tag.TAG_COMPOUND) : new ListTag();
                    NonNullList<ItemStack> nonNullList = NonNullList.withSize(listTag.size(), ItemStack.EMPTY);
                    ContainerHelper.loadAllItems(compoundTag, nonNullList, lookupProvider);
                    UUID uuid = compoundTag.getUUID("uuid");
                    deathSnapshot.put(uuid, List.copyOf(nonNullList));
                }
                catch(Exception ignored)
                {
                }
            }
        }

        if(tag.contains("Soulbounds"))
        {
            ListTag soulboundList = tag.getList("Soulbounds", Tag.TAG_COMPOUND);
            for(int i = 0; i < soulboundList.size(); i++)
            {
                try
                {
                    CompoundTag compoundTag = soulboundList.getCompound(i);
                    ListTag listTag = compoundTag.contains("Items", Tag.TAG_LIST) ? compoundTag.getList("Items", Tag.TAG_COMPOUND) : new ListTag();
                    NonNullList<ItemStack> nonNullList = NonNullList.withSize(listTag.size(), ItemStack.EMPTY);
                    ContainerHelper.loadAllItems(compoundTag, nonNullList, lookupProvider);
                    UUID uuid = compoundTag.getUUID("uuid");
                    soulboundItems.put(uuid, new CopyOnWriteArrayList<>(nonNullList));
                }
                catch(Exception ignored)
                {
                }
            }
        }

        if(tag.contains("ActiveGraves"))
        {
            ListTag gravesListTag = tag.getList("ActiveGraves", Tag.TAG_COMPOUND);
            for(int i = 0; i < gravesListTag.size(); i++)
            {
                try
                {
                    CompoundTag playerGravesTag = gravesListTag.getCompound(i);
                    UUID uuid = playerGravesTag.getUUID("uuid");
                    ListTag posList = playerGravesTag.getList("positions", Tag.TAG_COMPOUND);

                    List<GlobalPos> positions = new CopyOnWriteArrayList<>();
                    for(int j = 0; j < posList.size(); j++)
                    {
                        CompoundTag posTag = posList.getCompound(j);
                        ResourceKey<Level> dim = ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(posTag.getString("dimension")));
                        BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                        positions.add(GlobalPos.of(dim, pos));
                    }
                    activeGraves.put(uuid, positions);
                }
                catch(Exception ignored)
                {
                }
            }
        }

        if(tag.contains("GraveXp"))
        {
            ListTag xpListTag = tag.getList("GraveXp", Tag.TAG_COMPOUND);
            for(int i = 0; i < xpListTag.size(); i++)
            {
                try
                {
                    CompoundTag xpTag = xpListTag.getCompound(i);
                    ResourceKey<Level> dim = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(xpTag.getString("dimension")));
                    BlockPos pos = new BlockPos(xpTag.getInt("x"), xpTag.getInt("y"), xpTag.getInt("z"));
                    int xp = xpTag.getInt("xp");
                    graveXp.put(GlobalPos.of(dim, pos), xp);
                }
                catch(Exception ignored)
                {
                }
            }
        }

        return graveLogic;
    }

    public static void SnapshotInventory(LivingDeathEvent event)
    {
        try
        {
            if(!(event.getEntity() instanceof Player player)) return;
            UUID uuid = player.getUUID();
            Inventory inventory = player.getInventory();

            if(inventory.getContainerSize() == 0) return;

            List<ItemStack> itemStacks = new ArrayList<>();
            for(int i = 0; i < inventory.getContainerSize(); i++)
            {
                ItemStack item = inventory.getItem(i);
                itemStacks.add(item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy());
            }

            deathSnapshot.put(uuid, List.copyOf(itemStacks));

            List<ItemStack> curiosList = new ArrayList<>();
            if(ModList.get().isLoaded("curios"))
            {
                try
                {
                    CuriosCompat.snapCurios(player, curiosList);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                curiosSnapshot.put(uuid, List.copyOf(curiosList));
            }

            if(player instanceof ServerPlayer serverPlayer)
            {
                try
                {
                    GraveBackupManager.createBackup(serverPlayer, itemStacks, curiosList);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            if(instance != null) instance.setDirty();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static List<ItemStack> getSnapshot(ServerPlayer player)
    {
        List<ItemStack> list = deathSnapshot.get(player.getUUID());
        return list == null ? List.of() : list;
    }

    public static void removeGrave(UUID playerUUID, GlobalPos gravePos)
    {
        List<GlobalPos> graves = activeGraves.get(playerUUID);
        if(graves != null)
        {
            graves.remove(gravePos);
        }
        if(gravePos != null)
        {
            graveXp.remove(gravePos);
        }
        if(instance != null) instance.setDirty();
    }

    public static void RestoreContents(Level level, Player player, List<ItemStack> items)
    {
        RestoreContents(level, player, items, null);
    }

    public static void RestoreContents(Level level, Player player, List<ItemStack> items, GlobalPos gravePos)
    {
        try
        {
            if(gravePos != null && graveXp.containsKey(gravePos))
            {
                int xp = graveXp.remove(gravePos);
                if(xp > 0 && player != null)
                {
                    player.giveExperiencePoints(xp);
                }
                if(instance != null) instance.setDirty();
            }

            List<ItemStack> toFill = new ArrayList<>();
            List<ItemStack> chests = new ArrayList<>();
            ItemStack chest = new ItemStack(Items.CHEST);
            for(ItemStack item : items)
            {
                if(item != null && !item.isEmpty())
                {
                    toFill.add(item.copy());
                }

                if(toFill.size() == 27)
                {
                    ItemContainerContents contents = ItemContainerContents.fromItems(toFill);
                    chest.set(DataComponents.CONTAINER, contents);
                    chests.add(chest);
                    toFill.clear();
                    chest = new ItemStack(Items.CHEST);
                }
            }
            if(!toFill.isEmpty())
            {
                ItemContainerContents contents = ItemContainerContents.fromItems(toFill);
                chest.set(DataComponents.CONTAINER, contents);
                chests.add(chest);
            }
            for(ItemStack filledChest : chests)
            {
                Containers.dropItemStack(level, player.getX(), player.getY() + 1, player.getZ(), filledChest);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void LivingDropsEvent(LivingDropsEvent event, ServerPlayer player)
    {
        try
        {
            Level level = player.level();
            BlockPos pos = player.blockPosition();

            if(event.getDrops().isEmpty()) return;

            int minSafeHeight = level.getMinBuildHeight() + 1;
            int maxSafeHeight = level.getMaxBuildHeight() - 1;

            if(pos.getY() < minSafeHeight)
            {
                pos = new BlockPos(pos.getX(), minSafeHeight, pos.getZ());
            }
            else if(pos.getY() > maxSafeHeight)
            {
                pos = new BlockPos(pos.getX(), maxSafeHeight, pos.getZ());
            }

            BlockPos search = pos;
            while(search.getY() > level.getMinBuildHeight() + 1)
            {
                Iterator<BlockPos.MutableBlockPos> iterator = BlockPos.spiralAround(pos, 8, Direction.NORTH, Direction.EAST).iterator();
                search = iterator.next();

                while((!player.level().getBlockState(search).canBeReplaced() || player.level().isFluidAtPosition(search, FluidState::isSource)) && iterator.hasNext())
                    search = iterator.next();
                if(player.level().getBlockState(search).canBeReplaced() && !player.level().isFluidAtPosition(search, FluidState::isSource))
                    break;
                pos = pos.above(1);
            }

            while(!level.getFluidState(search).isEmpty() && search.getY() < level.getMaxBuildHeight())
            {
                search = search.above();
            }

            if(level.getBlockState(search.below()).isAir())
            {
                BlockPos originalSearch = search;
                boolean foundHole = false;

                while(search.getY() > level.getMinBuildHeight())
                {
                    if(isValid3x3x3Hole(level, search))
                    {
                        foundHole = true;
                        break;
                    }
                    if(!level.getBlockState(search.below()).isAir() && !level.getBlockState(search.below()).canBeReplaced())
                    {
                        break;
                    }
                    search = search.below();
                }

                if(foundHole)
                {
                    clear3x3x3Area(level, search);
                }
                else
                {
                    search = originalSearch;
                }
            }

            if(!AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.SIMPLE_GRAVE))
                return;

            int tier = getGraveTier(player);

            Direction facing = player.getDirection().getOpposite();

            BlockState state = CGBlocks.SIMPLE_GRAVE.get().defaultBlockState().setValue(GraveBlock.GLOWING, AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ILLUMINATION)).setValue(GraveBlock.STYLE, tier).setValue(GraveBlock.FACING, facing);

            level.setBlock(search, state, 3);
            generateGravePlatform(level, search, tier);

            BlockEntity blockEntity = level.getBlockEntity(search);
            if(!(blockEntity instanceof GraveBlockEntity grave))
            {
                level.removeBlock(search, false);
                return;
            }

            int xpToSave = calculateRetainedXp(player);
            GlobalPos gravePos = GlobalPos.of(level.dimension(), search);
            if(xpToSave > 0)
            {
                graveXp.put(gravePos, xpToSave);
            }

            List<ItemStack> curiosToCheck = new ArrayList<>();

            if(ModList.get().isLoaded("curios"))
            {
                try
                {
                    CuriosCompat.saveCurios(curiosSnapshot, player, curiosToCheck, grave);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            player.setLastDeathLocation(Optional.of(gravePos));
            activeGraves.computeIfAbsent(player.getUUID(), k -> new CopyOnWriteArrayList<>()).add(gravePos);

            List<ItemEntity> items = event.getDrops().stream().toList();
            UUID uuid = player.getUUID();
            List<ItemStack> freshItems = new ArrayList<>();
            grave.setUuid(uuid, player.getScoreboardName());

            int invSize = player.getInventory().getContainerSize();
            grave.getItems().clear();
            for(int i = 0; i < invSize; i++)
            {
                grave.getItems().add(ItemStack.EMPTY);
            }

            List<ItemStack> snapshot = new ArrayList<>();
            List<ItemStack> currentSnapshot = deathSnapshot.get(uuid);
            if(currentSnapshot != null)
            {
                for(ItemStack s : currentSnapshot)
                {
                    snapshot.add(s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy());
                }
            }

            List<ItemEntity> toRemoveFromDrops = new ArrayList<>();

            for(ItemEntity entity : items)
            {
                if(entity == null) continue;
                ItemStack stack = entity.getItem();
                if(stack == null || stack.isEmpty()) continue;

                if(isSoulbound(stack))
                {
                    toRemoveFromDrops.add(entity);
                    freshItems.add(stack.copy());
                    continue;
                }

                boolean isCurioDrop = false;
                for(int i = 0; i < curiosToCheck.size(); i++)
                {
                    ItemStack curio = curiosToCheck.get(i);
                    if(!curio.isEmpty() && ItemStack.isSameItemSameComponents(curio, stack))
                    {
                        isCurioDrop = true;
                        curiosToCheck.set(i, ItemStack.EMPTY);
                        break;
                    }
                }

                if(isCurioDrop)
                {
                    toRemoveFromDrops.add(entity);
                    continue;
                }

                int originalSlot = -1;
                for(int i = 0; i < snapshot.size(); i++)
                {
                    ItemStack snapStack = snapshot.get(i);
                    if(!snapStack.isEmpty() && ItemStack.isSameItemSameComponents(snapStack, stack))
                    {
                        if(snapStack.getCount() >= stack.getCount())
                        {
                            originalSlot = i;
                            snapStack.shrink(stack.getCount());
                            if(snapStack.isEmpty())
                            {
                                snapshot.set(i, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }

                if(originalSlot != -1 && originalSlot < invSize)
                {
                    ItemStack currentInSlot = grave.getItems().get(originalSlot);
                    if(currentInSlot.isEmpty())
                    {
                        grave.getItems().set(originalSlot, stack.copy());
                    }
                    else
                    {
                        currentInSlot.grow(stack.getCount());
                    }
                }
                else
                {
                    boolean placed = false;
                    for(int i = 0; i < grave.getItems().size(); i++)
                    {
                        if(grave.getItems().get(i).isEmpty())
                        {
                            grave.getItems().set(i, stack.copy());
                            placed = true;
                            break;
                        }
                    }
                    if(!placed)
                    {
                        grave.getItems().add(stack.copy());
                    }
                }

                toRemoveFromDrops.add(entity);
            }

            event.getDrops().removeAll(toRemoveFromDrops);

            soulboundItems.compute(uuid, (k, existingList) -> {
                List<ItemStack> list = existingList != null ? existingList : new CopyOnWriteArrayList<>();
                list.addAll(freshItems);
                return list;
            });

            grave.setChanged();
            if(instance != null) instance.setDirty();
        }
        catch(Exception e)
        {
        }
    }

    public static void LivingRestoreEvent(PlayerEvent.Clone event)
    {
        try
        {
            if(!event.isWasDeath()) return;
            if(!(event.getEntity() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            List<ItemStack> items = soulboundItems.remove(player.getUUID());

            if(items == null)
            {
                return;
            }

            for(ItemStack stack : items)
            {
                if(stack == null || stack.isEmpty()) continue;

                if(CGConfig.DOES_SOULBOUND_VANISH.get())
                {
                    EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(holder -> holder.is(CGEnchantments.SOULBOUND)));
                }

                player.addItem(stack);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void giveCatalogue(PlayerWakeUpEvent event)
    {
        try
        {
            if(!(event.getEntity() instanceof ServerPlayer player)) return;

            for(InteractionHand hand : InteractionHand.values())
            {
                ItemStack stack = player.getItemInHand(hand);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CGConfig.TRANSMUTE_ITEM.get()));

                if(stack.is(Items.WRITABLE_BOOK))
                {
                    if(stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY) != WritableBookContent.EMPTY)
                    {
                        player.displayClientMessage(Component.translatable("string.catalogue.fail"), true);
                        return;
                    }
                }

                if(!stack.is(item)) continue;

                player.setItemInHand(hand, CGItems.GRAVE_CATALOGUE.toStack());
                player.displayClientMessage(Component.translatable("string.catalogue.aquisition"), true);
                player.playSound(SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
                AdvancementLogic.grant(player, CGAdvancementProvider.SIMPLE_GRAVE);
                return;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static boolean isValid3x3x3Hole(Level level, BlockPos center)
    {
        for(int x = -1; x <= 1; x++)
        {
            for(int y = -1; y <= 1; y++)
            {
                for(int z = -1; z <= 1; z++)
                {
                    BlockPos current = center.offset(x, y, z);
                    if(!level.getBlockState(current).isAir() && !level.getBlockState(current).canBeReplaced())
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void clear3x3x3Area(Level level, BlockPos center)
    {
        for(int x = -1; x <= 1; x++)
        {
            for(int y = -1; y <= 1; y++)
            {
                for(int z = -1; z <= 1; z++)
                {
                    BlockPos current = center.offset(x, y, z);
                    if(level.getBlockState(current).canBeReplaced())
                    {
                        level.setBlock(current, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void generateGravePlatform(Level level, BlockPos gravePos, int tier)
    {
        BlockPos below = gravePos.below();
        if(level.getBlockState(below).isAir() || level.getBlockState(below).canBeReplaced() || level.isFluidAtPosition(below, FluidState::isSource))
        {
            level.setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
        }
    }
}