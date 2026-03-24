package com.natesky9.catalystgraves.Block;

import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.Init.CGBlocks;
import com.natesky9.catalystgraves.Init.CGEnchantments;
import com.natesky9.catalystgraves.Init.CGItems;
import com.natesky9.catalystgraves.compact.CuriosCompat;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;

import top.theillusivec4.curios.api.CuriosApi;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.*;

/**
 * Handles the logic for grave generation, inventory snapshots, 
 * soulbound items, and world-persistent data for Catalyst Graves.
 */
@SuppressWarnings("null")
public class GraveLogic extends SavedData
{
    public static GraveLogic instance;
    static Map<UUID, List<ItemStack>> soulboundItems = new HashMap<>(); 
    public static Map<UUID, List<ItemStack>> deathSnapshot = new HashMap<>();
    public static Map<UUID, List<ItemStack>> curiosSnapshot = new HashMap<>();
    public static Map<UUID, List<GlobalPos>> activeGraves = new HashMap<>();

    /**
     * Factory method to create a new instance of GraveLogic.
     */
    public static GraveLogic create()
    {
        instance = new GraveLogic();
        return instance;
    }

    /**
     * Determines the grave visual tier based on player advancements.
     */
    static int getGraveTier(ServerPlayer player)
    {
        int graveTier = 0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ORGANIZATION) ? 1 : 0;
        graveTier += AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.GREATER_VITALITY) ? 1 : 0;
        return graveTier;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider)
    {
        // Save the death snapshot list to NBT.
        deathSnapshot.forEach((key, value) -> {
            ListTag snapshotList = new ListTag();
            CompoundTag snapshotCompound = new CompoundTag();
            snapshotCompound.putUUID("uuid", key);
            snapshotList.add(snapshotCompound);
            NonNullList<ItemStack> list = NonNullList.copyOf(value);
            ContainerHelper.saveAllItems(snapshotCompound, list, provider);
            compoundTag.put("Grave Snapshots", snapshotList);
        });

        // Save soulbound items to NBT.
        soulboundItems.forEach((key, value) -> {
            ListTag soulboundList = new ListTag();
            CompoundTag soulboundCompound = new CompoundTag();
            soulboundCompound.putUUID("uuid", key);
            soulboundList.add(soulboundCompound);
            NonNullList<ItemStack> list = NonNullList.copyOf(value);
            ContainerHelper.saveAllItems(soulboundCompound, list, provider);
            compoundTag.put("Soulbounds", soulboundList);
        });

        // Save active grave locations to NBT.
        ListTag gravesListTag = new ListTag();
        activeGraves.forEach((key, positions) -> {
            CompoundTag playerGravesTag = new CompoundTag();
            playerGravesTag.putUUID("uuid", key);

            ListTag posList = new ListTag();
            for(GlobalPos pos : positions)
            {
                CompoundTag posTag = new CompoundTag();
                posTag.putString("dimension", pos.dimension().location().toString());
                posTag.putInt("x", pos.pos().getX());
                posTag.putInt("y", pos.pos().getY());
                posTag.putInt("z", pos.pos().getZ());
                posList.add(posTag);
            }
            playerGravesTag.put("positions", posList);
            gravesListTag.add(playerGravesTag);
        });
        compoundTag.put("ActiveGraves", gravesListTag);

        return compoundTag;
    }

    /**
     * Loads the stored GraveLogic data from the world NBT.
     */
    public static GraveLogic load(CompoundTag tag, HolderLookup.Provider lookupProvider)
    {
        // Load the contents of the NBT into the world,
        // so that commands and re-equipping work between world loads/server restarts.
        GraveLogic graveLogic = GraveLogic.create();
        if(tag.contains("Grave Snapshots"))
        {
            ListTag graveList = tag.getList("Grave Snapshots", Tag.TAG_COMPOUND);
            for(int i = 0; i < graveList.size(); i++)
            {
                ListTag listtag = tag.getList("Items", Tag.TAG_COMPOUND);
                CompoundTag compoundTag = graveList.getCompound(i);
                NonNullList<ItemStack> nonNullList = NonNullList.withSize(listtag.size(), ItemStack.EMPTY);
                ContainerHelper.loadAllItems(compoundTag, nonNullList, lookupProvider);
                UUID uuid = compoundTag.getUUID("uuid");
                List<ItemStack> list = nonNullList.stream().toList();
                deathSnapshot.put(uuid, list);
            }
        }

        if(tag.contains("Soulbounds"))
        {
            ListTag soulboundList = tag.getList("Soulbounds", Tag.TAG_COMPOUND);
            for(int i = 0; i < soulboundList.size(); i++)
            {
                ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
                CompoundTag compoundTag = soulboundList.getCompound(i);
                NonNullList<ItemStack> nonNullList = NonNullList.withSize(listTag.size(), ItemStack.EMPTY);
                ContainerHelper.loadAllItems(compoundTag, nonNullList, lookupProvider);
                UUID uuid = compoundTag.getUUID("uuid");
                List<ItemStack> list = nonNullList.stream().toList();
                soulboundItems.put(uuid, list);
            }
        }

        if(tag.contains("ActiveGraves"))
        {
            ListTag gravesListTag = tag.getList("ActiveGraves", Tag.TAG_COMPOUND);
            for(int i = 0; i < gravesListTag.size(); i++)
            {
                CompoundTag playerGravesTag = gravesListTag.getCompound(i);
                UUID uuid = playerGravesTag.getUUID("uuid");
                ListTag posList = playerGravesTag.getList("positions", Tag.TAG_COMPOUND);

                List<GlobalPos> positions = new ArrayList<>();
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
        }
        return graveLogic;
    }

    /**
     * Captures the player's inventory at the moment of death.
     */
    public static void SnapshotInventory(LivingDeathEvent event)
    {
        Player player = (Player)event.getEntity();
        UUID uuid = player.getUUID();
        Inventory inventory = player.getInventory();

        // Get a snapshot of the player's items for command purposes.
        // This also contains soulbound items and Curios as a backup.
        List<ItemStack> itemStacks = new ArrayList<>();

        if(inventory.getContainerSize() == 0)
        {
            return;
        }

        for(int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack item = inventory.getItem(i);
            itemStacks.add(item.copy());
        }

        deathSnapshot.put(uuid, itemStacks);

        if(CuriosCompat.isLoaded())
        {
            List<ItemStack> curiosList = new ArrayList<>();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((id, stacksHandler) -> {
                    for(int i = 0; i < stacksHandler.getStacks().getSlots(); i++)
                    {
                        curiosList.add(stacksHandler.getStacks().getStackInSlot(i).copy());
                    }
                });
            });
            curiosSnapshot.put(uuid, curiosList);
        }

        instance.setDirty();
    }

    public static List<ItemStack> getSnapshot(ServerPlayer player)
    {
        List<ItemStack> list = deathSnapshot.get(player.getUUID());
        if(list == null)
        {
            return List.of();
        }
        return list;
    }

    /**
     * Packs items into chests and drops them at the player's location.
     */
    public static void RestoreContents(Level level, Player player, List<ItemStack> items)
    {
        List<ItemStack> toFill = new ArrayList<>(List.of());
        List<ItemStack> chests = new ArrayList<>(List.of());
        ItemStack chest = new ItemStack(Items.CHEST);
        for(ItemStack item : items)
        {
            if(!item.isEmpty())
            {
                toFill.add(item);
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
            Containers.dropItemStack(level, player.getX(), player.getY() + 1, player.getZ(), filledChest);
    }

    /**
     * Handles the placement of the grave block and transferring dropped items into it.
     */
    public static void LivingDropsEvent(LivingDropsEvent event, ServerPlayer player)
    {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if(event.getDrops().isEmpty()) return;

        // Check for the first free, non-fluid location to place our grave.
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

            // Search downwards until a hole is found or a solid block is hit.
            while(search.getY() > level.getMinBuildHeight())
            {
                // If a 3x3x3 hole is found, stop searching.
                if(isValid3x3x3Hole(level, search))
                {
                    foundHole = true;
                    break;
                }

                // If the block below is not air and not replaceable, we've hit solid ground; stop.
                if(!level.getBlockState(search.below()).isAir() && !level.getBlockState(search.below()).canBeReplaced())
                {
                    break;
                }
                search = search.below(); // Move down one block
            }

            if(foundHole)
            {
                // If a hole was found, clear any obstructing blocks within the 3x3x3 area.
                clear3x3x3Area(level, search);
            }
            else
            {
                // If no hole was found while descending, revert to the original position.
                search = originalSearch;
            }
        }

        if(!AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.SIMPLE_GRAVE))
            return;

        int tier = getGraveTier(player); // Extract the tier into a variable

        BlockState state = CGBlocks.SIMPLE_GRAVE.get().defaultBlockState()
                .setValue(SimpleGrave.GLOWING, AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.ILLUMINATION))
                .setValue(SimpleGrave.STYLE, tier)
                .setValue(SimpleGrave.FACING, Direction.NORTH);

        level.setBlock(search, state, 3);

        generateGravePlatform(level, search, tier);

        BlockEntity blockEntity = level.getBlockEntity(search);
        if(!(blockEntity instanceof SimpleGraveEntity grave)) return;

        List<ItemStack> curiosToCheck = new ArrayList<>();

        if(CuriosCompat.isLoaded())
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

        // Set the player's death location to this position for future reference.
        player.setLastDeathLocation(Optional.of(GlobalPos.of(level.dimension(), search)));
        GlobalPos gravePos = GlobalPos.of(level.dimension(), search);
        activeGraves.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(gravePos);
        instance.setDirty();

        List<ItemEntity> items = event.getDrops().stream().toList();
        UUID uuid = player.getUUID();
        List<ItemStack> freshItems = new ArrayList<>();
        grave.setUuid(uuid, player.getScoreboardName());

        for(ItemEntity entity : items)
        {
            ItemStack stack = entity.getItem();
            boolean soulbound = stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(CGEnchantments.SOULBOUND));
            boolean nativeSoulbound = CGConfig.NATIVE_SOULBOUNDS.get().contains(stack.getItem().toString());
            
            if(nativeSoulbound || soulbound)
            {
                event.getDrops().remove(entity);
                freshItems.add(entity.getItem());
                continue;
            }

            boolean isCurioDrop = false;
            for(int i = 0; i < curiosToCheck.size(); i++)
            {
                ItemStack curio = curiosToCheck.get(i);
                // If the dropped item is the same as the one already added to the grave's curios list.
                if(!curio.isEmpty() && ItemStack.isSameItemSameComponents(curio, stack))
                {
                    isCurioDrop = true;
                    curiosToCheck.set(i, ItemStack.EMPTY); // Clear it to avoid matching it twice.
                    break;
                }
            }

            // If it's an accessory, cancel its normal drop (it's already safely in grave.addCurio()).
            if(isCurioDrop)
            {
                event.getDrops().remove(entity);
                continue;
            }

            grave.add(stack);
            event.getDrops().remove(entity);
        }

        if(soulboundItems.isEmpty())
        {
            soulboundItems.put(uuid, freshItems);
        }
        else
        {
            List<ItemStack> combined = soulboundItems.get(uuid);
            combined.addAll(freshItems);
            soulboundItems.put(uuid, combined);
        }
    }

    /**
     * Handles the restoration of soulbound items when the player respawns.
     */
    public static void LivingRestoreEvent(PlayerEvent.Clone event)
    {
        if(!event.isWasDeath()) return;
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        List<ItemStack> items = soulboundItems.get(player.getUUID());

        if(items == null)
        {
            return;
        }

        for(ItemStack stack : items)
        {
            // If configured, remove the soulbound enchantment from the restored item.
            if(CGConfig.DOES_SOULBOUND_VANISH.get())
            {
                EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(holder -> holder.is(CGEnchantments.SOULBOUND)));
            }

            player.addItem(stack);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
        }
        soulboundItems.remove(player.getUUID());
    }

    /**
     * Transmutes a book into the Grave Catalogue when the player wakes up with the configured item.
     */
    public static void giveCatalogue(PlayerWakeUpEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        for(InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CGConfig.TRANSMUTE_ITEM.get()));
            
            if(stack.is(Items.WRITABLE_BOOK))
            {
                // Exit case for books with content to avoid accidental loss.
                if(stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY) != WritableBookContent.EMPTY)
                {
                    player.displayClientMessage(Component.translatable("string.catalogue.fail"), true);
                    return;
                }
            }

            if(!stack.is(item)) continue;

            // Process the transmutation.
            player.setItemInHand(hand, CGItems.GRAVE_CATALOGUE.toStack());
            player.displayClientMessage(Component.translatable("string.catalogue.aquisition"), true);
            player.playSound(SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
            AdvancementLogic.grant(player, CGAdvancementProvider.SIMPLE_GRAVE);
            return;
        }
    }

    private static void placeSafePlatformBlock(Level level, BlockPos pos, BlockState state)
    {
        BlockState currentState = level.getBlockState(pos);
        if(currentState.canBeReplaced() || currentState.isAir() || level.isFluidAtPosition(pos, FluidState::isSource))
        {
            level.setBlock(pos, state, 3);
        }
    }

    private static BlockState getRandomFlower()
    {
        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};
        int index = new Random().nextInt(flowers.length);
        return flowers[index].defaultBlockState();
    }

    /**
     * Generates a decorative platform under the grave if placed in the air or fluids.
     */
    private static void generateGravePlatform(Level level, BlockPos gravePos, int tier)
    {
        // Check if we are in the Nether, the End, at void level, or in fluid/air.
        boolean isNether = level.dimension() == Level.NETHER;
        boolean isEnd = level.dimension() == Level.END;
        boolean isVoid = gravePos.getY() <= level.getMinBuildHeight() + 5;
        boolean isFluid = !level.getFluidState(gravePos.below()).isEmpty();
        boolean isAir = level.getBlockState(gravePos.below()).isAir();

        if(!isNether && !isEnd && !isVoid && !isFluid && !isAir) return;

        BlockPos base = gravePos.below();

        // Tier 0
        if(tier == 0)
        {
            placeSafePlatformBlock(level, base, Blocks.DIRT.defaultBlockState());
        }
        // Tier 1
        else if(tier == 1)
        {
            placeSafePlatformBlock(level, base, Blocks.STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.north(), Blocks.STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.south(), Blocks.STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.east(), Blocks.STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.west(), Blocks.STONE.defaultBlockState());
        }
        // Tier 2
        else if(tier >= 2)
        {
            // Central cross made of Smooth Stone.
            placeSafePlatformBlock(level, base, Blocks.SMOOTH_STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.north(), Blocks.SMOOTH_STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.south(), Blocks.SMOOTH_STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.east(), Blocks.SMOOTH_STONE.defaultBlockState());
            placeSafePlatformBlock(level, base.west(), Blocks.SMOOTH_STONE.defaultBlockState());

            // Grass Block corners.
            BlockPos ne = base.north().east();
            BlockPos nw = base.north().west();
            BlockPos se = base.south().east();
            BlockPos sw = base.south().west();

            placeSafePlatformBlock(level, ne, Blocks.GRASS_BLOCK.defaultBlockState());
            placeSafePlatformBlock(level, nw, Blocks.GRASS_BLOCK.defaultBlockState());
            placeSafePlatformBlock(level, se, Blocks.GRASS_BLOCK.defaultBlockState());
            placeSafePlatformBlock(level, sw, Blocks.GRASS_BLOCK.defaultBlockState());

            // Flowers.
            placeSafePlatformBlock(level, ne.above(), getRandomFlower());
            placeSafePlatformBlock(level, nw.above(), getRandomFlower());

            // Torches or lanterns.
            Random rand = new Random();
            BlockState lightSource1 = rand.nextBoolean() ? Blocks.TORCH.defaultBlockState() : Blocks.SOUL_LANTERN.defaultBlockState();
            BlockState lightSource2 = rand.nextBoolean() ? Blocks.TORCH.defaultBlockState() : Blocks.SOUL_LANTERN.defaultBlockState();

            placeSafePlatformBlock(level, se.above(), lightSource1);
            placeSafePlatformBlock(level, sw.above(), lightSource2);
        }
    }

    /**
     * Checks if a 3x3x3 area is suitable for grave generation.
     */
    private static boolean isValid3x3x3Hole(Level level, BlockPos pos)
    {
        int airOrReplaceable = 0;
        for(int x = -1; x <= 1; x++)
        {
            for(int y = -1; y <= 1; y++)
            {
                // y=-1 (platform), y=0 (grave), y=1 (air above).
                for(int z = -1; z <= 1; z++)
                {
                    BlockPos target = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(target);

                    if(state.getDestroySpeed(level, target) < 0) return false;

                    if(state.isAir() || state.canBeReplaced())
                    {
                        airOrReplaceable++;
                    }
                }
            }
        }

        return airOrReplaceable >= 14;
    }

    /**
     * Clears blocks in a 3x3x3 area to make room for the grave.
     */
    private static void clear3x3x3Area(Level level, BlockPos pos)
    {
        for(int x = -1; x <= 1; x++)
        {
            for(int y = -1; y <= 1; y++)
            {
                for(int z = -1; z <= 1; z++)
                {
                    BlockPos target = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(target);
                    if(!state.isAir())
                    {
                        level.destroyBlock(target, true);
                    }
                }
            }
        }
    }

    public static void removeGrave(UUID uuid, GlobalPos pos)
    {
        if(activeGraves.containsKey(uuid))
        {
            activeGraves.get(uuid).remove(pos);
            if(instance != null) instance.setDirty();
        }
    }
}