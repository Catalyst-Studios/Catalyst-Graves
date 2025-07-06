package com.example.catalystgraves;

import com.example.DataGen.DataGenerators;
import com.example.DataGen.ModAdvancementProvider;
import com.example.catalystgraves.Block.SimpleGraveRenderer;
import com.example.catalystgraves.Init.ModBlockEntities;
import com.example.catalystgraves.Init.ModConfig;
import com.example.catalystgraves.Init.ModItems;
import com.example.catalystgraves.Init.ModMenus;
import com.example.catalystgraves.Menus.BindingPacket;
import com.example.catalystgraves.Menus.CataloguePacket;
import com.example.catalystgraves.Screen.BindingScreen;
import com.example.catalystgraves.Screen.CatalogueScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = CatalystGraves.MODID)
public class Events {
    //This class listens for the respective events that fire,
    //allowing you to intercept events and run your custom logic
    //each class HAS to be annotated with @SubscribeEvent
    //and must be `public static void` to work. The name doesn't matter
    //but the argument must be the Event object as the parameter
    @SubscribeEvent
    public static void RegisterCommands(RegisterCommandsEvent event)
    {
        new GraveCommands(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event)
    {
        //create our SavedData handler, which saves snapshots and soulbound items
        DimensionDataStorage storage = event.getServer().overworld().getDataStorage();
        storage.computeIfAbsent(new SavedData.Factory<SavedData>(GraveLogic::create,GraveLogic::load),"graves");
    }
    @SubscribeEvent
    public static void sleepEvent(PlayerWakeUpEvent event)
    {
        GraveLogic.giveCatalogue(event);
    }
    @SubscribeEvent
    public static void useItemOn(UseItemOnBlockEvent event)
    {
        //this is to cancel a block right click
        //Player player = event.getPlayer();
        //ItemStack slot = event.getUseOnContext().getItemInHand();
        //Level level = event.getUseOnContext().getLevel();
        //BlockPos pos = event.getUseOnContext().getClickedPos();
        ////only process our item, when used on a crafting table
        //if (!slot.is(ModItems.GRAVE_CATALOGUE)) return;
        //if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) return;
        //event.cancelWithResult(ItemInteractionResult.SUCCESS);
        //event.setCancellationResult(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
    }
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModMenus.CATALOGUE.get(), CatalogueScreen::new);
        event.register(ModMenus.BINDING.get(), BindingScreen::new);
    }
    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event)
    {
        //packet register for the catalogue
        //takes in a string for the advancement, and a cost
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(//make sure this doesn't need to be bidirectional
                CataloguePacket.TYPE,
                CataloguePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        CataloguePacket.ClientPayloadHandler::handleData,
                        CataloguePacket.ServerPayloadHandler::handleData
                ));
        final PayloadRegistrar bindingRegistrar = event.registrar("1.1");
        bindingRegistrar.playToServer(
                BindingPacket.TYPE,
                BindingPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        BindingPacket.ClientPayloadHandler::handleData,
                        BindingPacket.ServerPayloadHandler::handleData
                ));
    }
    @SubscribeEvent
    public static void gatherDataEvent(GatherDataEvent event)
    {
        DataGenerators.gatherData(event);
    }
    @SubscribeEvent
    public static void LivingDeathEvent(LivingDeathEvent event)
    {
        //only process players
        if (!(event.getEntity() instanceof Player player)) return;
        GraveLogic.SnapshotInventory(event);
        //TODO: save the inventory state to be loaded exactly as it was
    }
    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event)
    {
        GraveLogic.LivingDropsEvent(event);
    }
    @SubscribeEvent
    public static void livingSpawn(PlayerEvent.Clone event)
    {
        //this is also for copying over the persistent data from the previous player entity
        GraveLogic.LivingRestoreEvent(event);
    }
    @SubscribeEvent
    public static void playerSpawn(EntityJoinLevelEvent event)
    {
        //event to add a book to new players,
        //if the config option is enabled
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean loaded = event.loadedFromDisk();
        if (!loaded && ModConfig.SPAWN_WITH_BOOK.get())
        {
            //until I find the correct event that fires only on first join, this will just always spawn the player
            //with a book in their inventory, unless they have one
            if (!player.getInventory().hasAnyMatching(item -> item.is(ModItems.GRAVE_CATALOGUE.asItem())))
                player.addItem(ModItems.GRAVE_CATALOGUE.toStack());
        }
        if (!loaded && ModConfig.FREEBIE.get())
        {
            AdvancementLogic.grant(player, ModAdvancementProvider.ADVANCEMENT_ROOT);
            AdvancementLogic.grant(player, ModAdvancementProvider.SIMPLE_GRAVE);
            AdvancementLogic.grant(player, ModAdvancementProvider.DESECRATE);
            AdvancementLogic.grant(player, ModAdvancementProvider.ORGANIZATION);
        }
    }
    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlockEntities.SIMPLE_GRAVE.get(), SimpleGraveRenderer::new);
    }
}
