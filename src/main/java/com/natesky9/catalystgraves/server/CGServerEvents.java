package com.natesky9.catalystgraves.server;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.block.GraveBlock;
import com.natesky9.catalystgraves.block.GraveBlockEntity;
import com.natesky9.catalystgraves.compat.client.menus.BindingPacket;
import com.natesky9.catalystgraves.compat.client.menus.CataloguePacket;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.natesky9.catalystgraves.datagen.CGDataGenerators;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.init.CGItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = CatalystGraves.MODID)
@SuppressWarnings("null")
public class CGServerEvents
{
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event)
    {
        new CGCommands(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event)
    {
        DimensionDataStorage storage = event.getServer().overworld().getDataStorage();
        storage.computeIfAbsent(new SavedData.Factory<SavedData>(GraveLogic::create, GraveLogic::load), "graves");

        GraveBackupManager.initialize(event.getServer());
    }

    @SubscribeEvent
    public static void sleepEvent(PlayerWakeUpEvent event)
    {
        GraveLogic.giveCatalogue(event);
    }

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
            CataloguePacket.TYPE,
            CataloguePacket.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                CataloguePacket.ClientPayloadHandler::handleData,
                CataloguePacket.ServerPayloadHandler::handleData));

        final PayloadRegistrar bindingRegistrar = event.registrar("1.1");
        bindingRegistrar.playToServer(
            BindingPacket.TYPE,
            BindingPacket.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                BindingPacket.ClientPayloadHandler::handleData,
                BindingPacket.ServerPayloadHandler::handleData));
    }

    @SubscribeEvent
    public static void gatherDataEvent(GatherDataEvent event)
    {
        CGDataGenerators.gatherData(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        GraveLogic.SnapshotInventory(event);

        BlockPos pos = player.blockPosition();
        String dim = player.level().dimension().location().toString();
        boolean hasRecall = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.CORPOREAL_RECALL);

        Style COLOR_HEADER = Style.EMPTY.withColor(TextColor.fromRgb(0xFF4757)).withBold(true);
        Style COLOR_LABEL = Style.EMPTY.withColor(TextColor.fromRgb(0xA4B0BE));
        Style COLOR_COORDS = Style.EMPTY.withColor(TextColor.fromRgb(0x70A1FF));
        Style COLOR_DIM = Style.EMPTY.withColor(TextColor.fromRgb(0xA29BFE));

        Style COLOR_TP_BTN = Style.EMPTY.withColor(TextColor.fromRgb(0x2ED573)).withBold(true).withUnderlined(true);
        Style COLOR_COPY_BTN = Style.EMPTY.withColor(TextColor.fromRgb(0x1E90FF)).withUnderlined(true);

        Style COLOR_HOVER = Style.EMPTY.withColor(TextColor.fromRgb(0xE2E8F0)).withItalic(true);

        Component header = Component.literal("\n")
                               .append(Component.translatable("message.catalystgraves.death.header").withStyle(COLOR_HEADER));

        Component coordsInfo = Component.literal("  Location: ")
                                   .withStyle(COLOR_LABEL)
                                   .append(Component.literal(String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ())).withStyle(COLOR_COORDS))
                                   .append(Component.literal(" in ").withStyle(COLOR_LABEL))
                                   .append(Component.literal(dim).withStyle(COLOR_DIM));

        Component actionComponent;
        if(hasRecall)
        {
            String tpCmd = "/grave tp 0";
            actionComponent = Component.literal("  ")
                                  .append(Component.translatable("message.catalystgraves.death.btn_tp")
                                              .withStyle(COLOR_TP_BTN
                                                             .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCmd))
                                                             .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                                            Component.translatable("message.catalystgraves.death.hover_tp").withStyle(COLOR_HOVER)))));
        }
        else
        {
            String tpSuggest = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
            actionComponent = Component.literal("  ")
                                  .append(Component.translatable("message.catalystgraves.death.btn_coords")
                                              .withStyle(COLOR_COPY_BTN
                                                             .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpSuggest))
                                                             .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                                            Component.translatable("message.catalystgraves.death.hover_coords").withStyle(COLOR_HOVER)))));
        }

        player.sendSystemMessage(header);
        player.sendSystemMessage(coordsInfo);
        player.sendSystemMessage(actionComponent);
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        GraveLogic.LivingDropsEvent(event, player);
    }

    @SubscribeEvent
    public static void livingSpawn(PlayerEvent.Clone event)
    {
        GraveLogic.LivingRestoreEvent(event);
    }

    @SubscribeEvent
    public static void playerSpawn(EntityJoinLevelEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean loaded = event.loadedFromDisk();
        if(!loaded && CGConfig.SPAWN_WITH_BOOK.get())
        {
            if(!player.getInventory().hasAnyMatching(item -> item.is(CGItems.GRAVE_CATALOGUE.asItem())))
                player.addItem(CGItems.GRAVE_CATALOGUE.toStack());
        }

        if(!loaded && CGConfig.FREEBIE.get())
        {
            AdvancementLogic.grant(player, CGAdvancementProvider.ADVANCEMENT_ROOT);
            AdvancementLogic.grant(player, CGAdvancementProvider.SIMPLE_GRAVE);
            AdvancementLogic.grant(player, CGAdvancementProvider.DESECRATE);
            AdvancementLogic.grant(player, CGAdvancementProvider.ORGANIZATION);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event)
    {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)
        {
            if(event.getEntity() instanceof FakePlayer)
            {
                event.setCanceled(true);
                return;
            }

            ItemStack stack = event.getItemStack();
            String itemName = stack.getItem().toString();
            if(itemName.contains("wrench") || itemName.contains("configurator") || itemName.contains("hammer"))
            {
                event.setCanceled(true);
                return;
            }

            Player player = event.getEntity();
            boolean isOwner = grave.getUuid() != null && grave.getUuid().equals(player.getUUID());
            boolean isAuthorized = isOwner || player.isCreative() || player.hasPermissions(2);

            if(isAuthorized)
            {
                if(event.isCanceled())
                {
                    event.setCanceled(false);
                }
                event.setUseBlock(TriState.TRUE);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if(event.getState().getBlock() instanceof GraveBlock)
        {
            if(!(event.getPlayer() instanceof ServerPlayer) || !event.getPlayer().isCreative())
            {
                if(!(event.getPlayer() instanceof FakePlayer))
                {
                    event.setCanceled(true);
                }
            }
        }
    }
}