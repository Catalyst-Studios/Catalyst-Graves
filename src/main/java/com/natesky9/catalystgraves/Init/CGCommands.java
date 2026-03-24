package com.natesky9.catalystgraves.Init;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.Block.GraveLogic;
import com.natesky9.catalystgraves.Block.SimpleGraveEntity;
import com.natesky9.catalystgraves.compact.CuriosCompat;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@SuppressWarnings("null")
public class CGCommands
{

    public CGCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("grave")
                                .then(Commands.literal("find")
                                          .executes(c -> find(c.getSource(), c.getSource().getPlayerOrException()))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .requires(s -> s.hasPermission(2))
                                                    .executes(c -> find(c.getSource(), EntityArgument.getPlayer(c, "player")))))

                                .then(Commands.literal("restore")
                                          .requires(s -> s.hasPermission(2))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(c -> restore(c.getSource(), EntityArgument.getPlayer(c, "player"), -1))))

                                .then(Commands.literal("recover")
                                          .requires(s -> s.hasPermission(2))
                                          .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(c -> recover(c.getSource(), EntityArgument.getPlayer(c, "player"))))));
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

        source.sendSystemMessage(Component.translatable("commands.catalystgraves.find.header", targetPlayer.getScoreboardName())
                                     .withStyle(ChatFormatting.GOLD));

        for(int i = 0; i < graves.size(); i++)
        {
            GlobalPos gPos = graves.get(i);
            BlockPos p = gPos.pos();
            String dim = gPos.dimension().location().toString();

            String tpCommand = "/execute in " + dim + " run tp @s " + p.getX() + " " + p.getY() + " " + p.getZ();

            ClickEvent.Action action = hasRecall ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;

            Component hoverText = hasRecall ? Component.translatable("commands.catalystgraves.find.hover.tp").withStyle(ChatFormatting.GREEN) : Component.translatable("commands.catalystgraves.find.hover.suggest").withStyle(ChatFormatting.RED);

            Component line = Component.translatable("commands.catalystgraves.find.entry", i, p.getX(), p.getY(), p.getZ())
                                 .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withClickEvent(new ClickEvent(action, tpCommand)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));

            source.sendSystemMessage(line);
        }
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

        if(level != null && level.getBlockEntity(targetPos.pos()) instanceof SimpleGraveEntity grave)
        {
            if(CuriosCompat.isLoaded())
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

    private void distributeItemsToPlayer(ServerPlayer player, List<ItemStack> items)
    {
        for(ItemStack stack : items)
        {
            if(stack.isEmpty()) continue;
            if(!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
        }
    }
}