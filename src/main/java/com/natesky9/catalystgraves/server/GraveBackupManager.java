package com.natesky9.catalystgraves.server;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a rolling FIFO history of 10 backups per player UUID.
 * Stored in: <world_dir>/catalystgraves_backups/<uuid>.dat
 */

@SuppressWarnings("null")
public class GraveBackupManager
{
    public static final int MAX_BACKUPS_PER_PLAYER = 10;
    private static final String BACKUP_DIR_NAME = "catalystgraves_backups";

    public record BackupEntry(
        long timestamp,
        String dimension,
        int x,
        int y,
        int z,
        List<ItemStack> items,
        List<ItemStack> curios
    ) {}

    private static final Map<UUID, List<BackupEntry>> BACKUP_CACHE = new ConcurrentHashMap<>();
    private static File backupDirectory;

    public static void initialize(MinecraftServer server)
    {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        backupDirectory = worldDir.resolve(BACKUP_DIR_NAME).toFile();
        if (!backupDirectory.exists())
        {
            backupDirectory.mkdirs();
        }
        BACKUP_CACHE.clear();
        loadAll(server);
    }

    public static synchronized void createBackup(ServerPlayer player, List<ItemStack> items, List<ItemStack> curios)
    {
        if (backupDirectory == null) return;

        UUID uuid = player.getUUID();
        long now = Instant.now().toEpochMilli();
        String dim = player.level().dimension().location().toString();
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        List<ItemStack> itemsCopy = new ArrayList<>();
        for (ItemStack s : items)
        {
            itemsCopy.add(s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }

        List<ItemStack> curiosCopy = new ArrayList<>();
        for (ItemStack c : curios)
        {
            curiosCopy.add(c == null || c.isEmpty() ? ItemStack.EMPTY : c.copy());
        }

        BackupEntry entry = new BackupEntry(now, dim, x, y, z, itemsCopy, curiosCopy);

        List<BackupEntry> list = BACKUP_CACHE.computeIfAbsent(uuid, k -> new ArrayList<>());
        synchronized (list)
        {
            list.add(0, entry); // newest first (index 0)
            while (list.size() > MAX_BACKUPS_PER_PLAYER)
            {
                list.remove(list.size() - 1);
            }
        }

        savePlayerBackups(player.getServer(), uuid);
    }

    public static List<BackupEntry> getBackups(UUID uuid)
    {
        List<BackupEntry> list = BACKUP_CACHE.get(uuid);
        if (list == null) return Collections.emptyList();
        synchronized (list)
        {
            return new ArrayList<>(list);
        }
    }

    public static BackupEntry getBackup(UUID uuid, int slotOneIndexed)
    {
        List<BackupEntry> list = BACKUP_CACHE.get(uuid);
        if (list == null) return null;
        synchronized (list)
        {
            int index = slotOneIndexed - 1;
            if (index < 0 || index >= list.size())
            {
                return null;
            }
            return list.get(index);
        }
    }

    private static synchronized void savePlayerBackups(MinecraftServer server, UUID uuid)
    {
        if (backupDirectory == null) return;
        List<BackupEntry> list = BACKUP_CACHE.get(uuid);
        if (list == null) return;

        File playerFile = new File(backupDirectory, uuid.toString() + ".dat");
        CompoundTag root = new CompoundTag();
        ListTag backupListTag = new ListTag();

        synchronized (list)
        {
            for (BackupEntry entry : list)
            {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("timestamp", entry.timestamp());
                entryTag.putString("dimension", entry.dimension());
                entryTag.putInt("x", entry.x());
                entryTag.putInt("y", entry.y());
                entryTag.putInt("z", entry.z());

                CompoundTag itemsTag = new CompoundTag();
                NonNullList<ItemStack> itemsList = NonNullList.copyOf(entry.items());
                ContainerHelper.saveAllItems(itemsTag, itemsList, server.registryAccess());
                entryTag.put("items", itemsTag);

                if (!entry.curios().isEmpty())
                {
                    CompoundTag curiosTag = new CompoundTag();
                    NonNullList<ItemStack> curiosList = NonNullList.copyOf(entry.curios());
                    ContainerHelper.saveAllItems(curiosTag, curiosList, server.registryAccess());
                    entryTag.put("curios", curiosTag);
                }

                backupListTag.add(entryTag);
            }
        }

        root.put("backups", backupListTag);

        try
        {
            File tempFile = new File(backupDirectory, uuid.toString() + ".tmp");
            NbtIo.writeCompressed(root, tempFile.toPath());
            if (playerFile.exists())
            {
                playerFile.delete();
            }
            tempFile.renameTo(playerFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void loadAll(MinecraftServer server)
    {
        if (backupDirectory == null || !backupDirectory.exists()) return;

        File[] files = backupDirectory.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;

        for (File file : files)
        {
            String name = file.getName();
            String uuidStr = name.substring(0, name.length() - 4);
            try
            {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag root = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                if (root != null && root.contains("backups", Tag.TAG_LIST))
                {
                    ListTag backupListTag = root.getList("backups", Tag.TAG_COMPOUND);
                    List<BackupEntry> entries = new ArrayList<>();
                    for (int i = 0; i < backupListTag.size(); i++)
                    {
                        CompoundTag entryTag = backupListTag.getCompound(i);
                        long ts = entryTag.getLong("timestamp");
                        String dim = entryTag.getString("dimension");
                        int x = entryTag.getInt("x");
                        int y = entryTag.getInt("y");
                        int z = entryTag.getInt("z");

                        CompoundTag itemsTag = entryTag.getCompound("items");
                        ListTag itemsListTag = itemsTag.contains("Items", Tag.TAG_LIST) ? itemsTag.getList("Items", Tag.TAG_COMPOUND) : new ListTag();
                        NonNullList<ItemStack> items = NonNullList.withSize(itemsListTag.size(), ItemStack.EMPTY);
                        ContainerHelper.loadAllItems(itemsTag, items, server.registryAccess());

                        NonNullList<ItemStack> curios = NonNullList.create();
                        if (entryTag.contains("curios", Tag.TAG_COMPOUND))
                        {
                            CompoundTag curiosTag = entryTag.getCompound("curios");
                            ListTag curiosListTag = curiosTag.contains("Items", Tag.TAG_LIST) ? curiosTag.getList("Items", Tag.TAG_COMPOUND) : new ListTag();
                            curios = NonNullList.withSize(curiosListTag.size(), ItemStack.EMPTY);
                            ContainerHelper.loadAllItems(curiosTag, curios, server.registryAccess());
                        }

                        entries.add(new BackupEntry(ts, dim, x, y, z, new ArrayList<>(items), new ArrayList<>(curios)));
                    }
                    BACKUP_CACHE.put(uuid, entries);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
