package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.Block.SimpleGraveEntity;
import com.natesky9.catalystgraves.GraveLogic;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class CGCommands {
    enum category
    {
        ROOT,
        FIND,
        RESTORE,
        RECOVER
    }
    public CGCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("grave")
                .requires(player -> player.hasPermission(Commands.LEVEL_MODERATORS))
                .executes(context -> help(context.getSource(), category.ROOT))
                        .then(Commands.literal("help")
                                .executes(player -> help(player.getSource(), category.ROOT))
                                .then(Commands.literal("find")
                                        .executes(player -> help(player.getSource(),category.FIND)))
                                .then(Commands.literal("restore")
                                        .executes(player -> help(player.getSource(),category.RESTORE)))
                                .then(Commands.literal("recover")
                                        .executes(player -> help(player.getSource(),category.RECOVER))))

                .then(Commands.literal("find")
                        .requires(player -> player.hasPermission(Commands.LEVEL_MODERATORS))
                        .executes(context -> find(context.getSource(),context.getSource().getPlayer()))
                        .then(Commands.argument("player",EntityArgument.player())
                                .executes(context -> find(context.getSource(),EntityArgument.getPlayer(context,"player")))))

                .then(Commands.literal("restore")
                        .requires(player -> player.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> help(context.getSource(),category.RESTORE))
                        .then(Commands.argument("player",EntityArgument.player())
                                .executes(context -> restore(context.getSource(),EntityArgument.getPlayer(context,"player")))))
                .then(Commands.literal("recover")
                        .requires(player -> player.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> help(context.getSource(),category.RECOVER))
                        .then(Commands.argument("player",EntityArgument.player())
                                .executes(context -> recover(context.getSource(),EntityArgument.getPlayer(context,"player")))))
        );

        //dispatcher.register(Commands.literal("grave")
        //                .requires(player -> player.hasPermission(Commands.LEVEL_GAMEMASTERS))
        //                .
        //        .then(Commands.literal("restore")
        //                        .executes((context) -> test(context.getSource()))));
    }
    private int help(CommandSourceStack source, category root)
    {
        switch (root)
        {
            case category.ROOT:
            {
                source.sendSystemMessage(Component.translatable("string.command.help.root")
                        .withStyle(ChatFormatting.AQUA));
                return 1;
            }
            case category.FIND:
                source.sendSystemMessage(Component.translatable("string.command.help.find")
                        .withStyle(ChatFormatting.GREEN));
                return 1;
            case RESTORE:
                source.sendSystemMessage(Component.translatable("string.command.help.restore")
                        .withStyle(ChatFormatting.GREEN));
                return 1;
            case RECOVER:
                source.sendSystemMessage(Component.translatable("string.command.help.recover")
                        .withStyle(ChatFormatting.GREEN));
                return 1;
            default:
                source.sendFailure(Component.literal("No such command!"));
        }
        return -1;
    }
    private int recover(CommandSourceStack source, ServerPlayer player)
    {
        if (!source.isPlayer()) return -1;
        Player target = source.getPlayer();
        if (target == null) return -1;
        List<ItemStack> items = GraveLogic.getSnapshot(player);
        if (items.isEmpty())
        {
            source.sendSystemMessage(Component.literal("Player's snapshot was either empty, did not save, or was overwritten!"));
            return -1;
        }
        GraveLogic.RestoreContents(source.getLevel(),player,items);
        return 1;
    }
    private int restore(CommandSourceStack source, ServerPlayer player)
    {
        if (!source.isPlayer()) return 0;
        if (player.getLastDeathLocation().isEmpty())
        {
            source.sendFailure(Component.translatable("string.command.find.fail",player.getScoreboardName()));
            return -1;
        }
        BlockPos pos = player.getLastDeathLocation().get().pos();
        ResourceKey<Level> dimension = player.getLastDeathLocation().get().dimension();
        ServerLevel level = source.getServer().getLevel(dimension);
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SimpleGraveEntity grave))
        {
            source.sendFailure(Component.translatable("string.command.restore.fail")
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                            "/grave recover " + player.getScoreboardName())).withHoverEvent(
                                    new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("string.command.recover.suggest",player.getScoreboardName())))));
            return -1;
        }
        SimpleContainer inventory = new SimpleContainer(grave.getItems().size());
        for (ItemStack item:grave.getItems())
            inventory.addItem(item);
        Containers.dropContents(player.level(),player.blockPosition(),inventory);
        level.setBlockAndUpdate(pos, Blocks.COBBLESTONE_WALL.defaultBlockState());
        source.sendSuccess(() -> Component.translatable("string.command.restore.success"),true);
        return 1;
    }
    private int find(CommandSourceStack source, ServerPlayer player)
    {
        if (!source.isPlayer()) return 0;
        if (player.getLastDeathLocation().isEmpty())
        {
            source.sendFailure(Component.translatable("string.command.find.fail",player.getScoreboardName()));
            return -1;
        }
        BlockPos pos = player.getLastDeathLocation().get().pos();
        ResourceKey<Level> dimension = player.getLastDeathLocation().get().dimension();
        source.sendSystemMessage(Component.translatable("string.command.find.success",player.getScoreboardName(),
                        dimension.location().getPath() + " - " + pos)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/execute in " + dimension.location() + " run tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                        .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("chat.coordinates.tooltip"))))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return 1;
    }
}
