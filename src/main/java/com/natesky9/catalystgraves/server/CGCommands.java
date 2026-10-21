package com.natesky9.catalystgraves.server;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.block.GraveBlockEntity;
import com.natesky9.catalystgraves.compat.curios.CuriosCompat;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Commands for Catalyst Graves:
 * /grave find [player]
 * /grave tp <index> (requires Corporeal Recall advancement, permission 0)
 * /grave restore [player] [index]
 * /grave recover [player]
 * /grave backup list [player]
 * /grave backup restore <slot> [player]
 */
@SuppressWarnings("null")
public class CGCommands
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CGCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("grave")
                                .then(Commands.literal("find")
                                          .executes(c -> find(c.getSource(), c.getSource().getPlayerOrException()))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .requires(s -> s.hasPermission(2))
                                                    .executes(c -> find(c.getSource(), EntityArgument.getPlayer(c, "player")))))

                                .then(Commands.literal("tp")
                                          .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                    .executes(c -> tpToGrave(c.getSource(), IntegerArgumentType.getInteger(c, "index")))))

                                .then(Commands.literal("restore")
                                          .requires(s -> s.hasPermission(2))
                                          .executes(c -> restore(c.getSource(), c.getSource().getPlayerOrException(), -1))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(c -> restore(c.getSource(), EntityArgument.getPlayer(c, "player"), -1))
                                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                              .executes(c -> restore(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "index"))))))

                                .then(Commands.literal("recover")
                                          .requires(s -> s.hasPermission(2))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(c -> recover(c.getSource(), EntityArgument.getPlayer(c, "player")))))

                                .then(Commands.literal("backup")
                                          .requires(s -> s.hasPermission(2))
                                          .then(Commands.literal("list")
                                                    .executes(c -> listBackups(c.getSource(), c.getSource().getPlayerOrException()))
                                                    .then(Commands.argument("player", EntityArgument.player())
                                                              .executes(c -> listBackups(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                                          .then(Commands.literal("restore")
                                                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 10))
                                                              .executes(c -> restoreBackup(c.getSource(), c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "slot")))
                                                              .then(Commands.argument("player", EntityArgument.player())
                                                                        .executes(c -> restoreBackup(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "slot"))))))));
    }

    private int find(CommandSourceStack source, ServerPlayer targetPlayer)
    {
        List<GlobalPos> graves = GraveLogic.activeGraves.get(targetPlayer.getUUID());

        if(graves == null || graves.isEmpty())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.find.empty", targetPlayer.getScoreboardName()));
            return -1;
        }

        ServerPlayer sourcePlayer = source.getPlayer();
        if(sourcePlayer == null) return 0;

        boolean hasRecall = AdvancementLogic.hasAdvancement(sourcePlayer, CGAdvancementProvider.CORPOREAL_RECALL);

        source.sendSystemMessage(
            Component.literal("")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.translatable("commands.catalystgraves.find.header", targetPlayer.getScoreboardName())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        for(int i = 0; i < graves.size(); i++)
        {
            GlobalPos gPos = graves.get(i);
            BlockPos p = gPos.pos();
            String dim = gPos.dimension().location().toString();

            Component entryText = Component.literal(String.format("  [%d] ", i))
                                      .withStyle(ChatFormatting.DARK_GRAY)
                                      .append(Component.literal(dim).withStyle(ChatFormatting.AQUA))
                                      .append(Component.literal(String.format(" (%d, %d, %d) ", p.getX(), p.getY(), p.getZ()))
                                                  .withStyle(ChatFormatting.WHITE));

            if(hasRecall)
            {
                String tpCmd = "/grave tp " + i;
                Component tpBtn = Component.translatable("commands.catalystgraves.find.btn_tp")
                                      .withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.catalystgraves.find.hover.tp").withStyle(ChatFormatting.GREEN))));
                entryText = entryText.copy().append(tpBtn);
            }
            else
            {
                String suggestCmd = String.format("/tp @s %d %d %d", p.getX(), p.getY(), p.getZ());
                Component suggestBtn = Component.translatable("commands.catalystgraves.find.btn_coords")
                                           .withStyle(style -> style.withColor(ChatFormatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCmd)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.catalystgraves.find.hover.suggest").withStyle(ChatFormatting.RED))));
                entryText = entryText.copy().append(suggestBtn);
            }

            source.sendSystemMessage(entryText);
        }
        return 1;
    }

    private int tpToGrave(CommandSourceStack source, int index)
    {
        ServerPlayer player = source.getPlayer();
        if(player == null)
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.tp.only_players"));
            return -1;
        }

        if(!AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.CORPOREAL_RECALL))
        {
            source.sendFailure(Component.translatable("string.grave.fail.no_advancement").withStyle(ChatFormatting.RED));
            return -1;
        }

        List<GlobalPos> graves = GraveLogic.activeGraves.get(player.getUUID());
        if(graves == null || index < 0 || index >= graves.size())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.restore.invalid_index").withStyle(ChatFormatting.RED));
            return -1;
        }

        GlobalPos targetPos = graves.get(index);
        ServerLevel targetLevel = player.getServer().getLevel(targetPos.dimension());
        if(targetLevel == null)
        {
            source.sendFailure(Component.translatable("string.grave.fail.levelnull").withStyle(ChatFormatting.RED));
            return -1;
        }

        BlockPos pos = targetPos.pos();
        DimensionTransition transition = new DimensionTransition(
            targetLevel,
            pos.above().getCenter(),
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            DimensionTransition.DO_NOTHING);
        player.changeDimension(transition);
        targetLevel.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7f, 1.0f);

        source.sendSuccess(() -> Component.literal("").withStyle(ChatFormatting.DARK_PURPLE).append(Component.translatable("commands.catalystgraves.tp.success", index).withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private int restore(CommandSourceStack source, ServerPlayer player, int index)
    {
        List<GlobalPos> graves = GraveLogic.activeGraves.get(player.getUUID());

        if(graves == null || graves.isEmpty())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.restore.no_graves"));
            return -1;
        }

        int targetIndex = (index == -1) ? graves.size() - 1 : index;
        if(targetIndex < 0 || targetIndex >= graves.size())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.restore.invalid_index"));
            return -1;
        }

        GlobalPos targetPos = graves.get(targetIndex);
        ServerLevel level = source.getServer().getLevel(targetPos.dimension());

        if(level != null && level.getBlockEntity(targetPos.pos()) instanceof GraveBlockEntity grave)
        {
            if(ModList.get().isLoaded("curios"))
            {
                CuriosCompat.restoreCurios(player, grave.getCuriosItems());
            }

            distributeItemsToPlayer(player, grave.getItems());
            level.setBlockAndUpdate(targetPos.pos(), Blocks.COBBLESTONE_WALL.defaultBlockState());

            source.sendSuccess(() -> Component.translatable("commands.catalystgraves.restore.success"), true);
            return 1;
        }

        return -1;
    }

    private int recover(CommandSourceStack source, ServerPlayer player)
    {
        List<ItemStack> items = GraveLogic.getSnapshot(player);
        if(items.isEmpty())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.recover.no_snapshot", player.getScoreboardName()));
            return -1;
        }

        distributeItemsToPlayer(player, items);
        source.sendSuccess(() -> Component.translatable("commands.catalystgraves.recover.success", player.getScoreboardName()), true);
        return 1;
    }

    private int listBackups(CommandSourceStack source, ServerPlayer player)
    {
        List<GraveBackupManager.BackupEntry> backups = GraveBackupManager.getBackups(player.getUUID());
        if(backups.isEmpty())
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.backup.list.empty", player.getScoreboardName()));
            return 0;
        }

        source.sendSystemMessage(
            Component.literal("")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.translatable("commands.catalystgraves.backup.list.header", player.getScoreboardName())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        for(int i = 0; i < backups.size(); i++)
        {
            int slot = i + 1;
            GraveBackupManager.BackupEntry entry = backups.get(i);
            String dateStr = DATE_FORMAT.format(new Date(entry.timestamp()));
            long totalItems = entry.items().stream().filter(s -> !s.isEmpty()).count();

            String restoreCmd = String.format("/grave backup restore %d %s", slot, player.getScoreboardName());

            Component line = Component.literal(String.format("  [%d] ", slot))
                                 .withStyle(ChatFormatting.YELLOW)
                                 .append(Component.literal(dateStr + " ").withStyle(ChatFormatting.GRAY))
                                 .append(Component.translatable("commands.catalystgraves.backup.list.entry_info", entry.dimension(), totalItems).withStyle(ChatFormatting.WHITE))
                                 .append(Component.translatable("commands.catalystgraves.backup.list.btn_restore")
                                             .withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, restoreCmd)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("commands.catalystgraves.backup.list.hover.restore", slot)))));

            source.sendSystemMessage(line);
        }
        return 1;
    }

    private int restoreBackup(CommandSourceStack source, ServerPlayer player, int slot)
    {
        GraveBackupManager.BackupEntry entry = GraveBackupManager.getBackup(player.getUUID(), slot);
        if(entry == null)
        {
            source.sendFailure(Component.translatable("commands.catalystgraves.backup.restore.invalid_slot", slot, player.getScoreboardName()));
            return -1;
        }

        if(ModList.get().isLoaded("curios") && !entry.curios().isEmpty())
        {
            NonNullList<ItemStack> curiosList = NonNullList.copyOf(entry.curios());
            CuriosCompat.restoreCurios(player, curiosList);
        }

        distributeItemsToPlayer(player, entry.items());

        source.sendSuccess(() -> Component.literal("").withStyle(ChatFormatting.DARK_PURPLE).append(Component.translatable("commands.catalystgraves.backup.restore.success", slot, player.getScoreboardName()).withStyle(ChatFormatting.GREEN)), true);

        return 1;
    }

    private void distributeItemsToPlayer(ServerPlayer player, List<ItemStack> items)
    {
        for(ItemStack stack : items)
        {
            if(stack.isEmpty()) continue;
            if(!player.getInventory().add(stack.copy()))
            {
                player.drop(stack.copy(), false);
            }
        }
    }
}