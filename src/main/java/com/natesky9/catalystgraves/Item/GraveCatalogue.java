package com.natesky9.catalystgraves.item;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.block.GraveBlockEntity;
import com.natesky9.catalystgraves.compat.client.menus.CatalogueMenu;
import com.natesky9.catalystgraves.compat.client.tooltip.CatalogueTooltipData;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.natesky9.catalystgraves.init.CGBlocks;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.server.GraveLogic;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
public class GraveCatalogue extends Item implements MenuProvider
{
    private static float shiftProgress = 0.0f;
    private static long lastTime = System.currentTimeMillis();
    private final BindingChapter bindingMenu = new BindingChapter();

    public GraveCatalogue(Properties properties)
    {
        super(properties);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.catalogue");
    }

    private Component getTransmuteItemName()
    {
        String itemId = CGConfig.TRANSMUTE_ITEM.get();
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        
        if(location != null)
        {
            Item item = BuiltInRegistries.ITEM.get(location);
            if(item != Items.AIR)
            {
                return item.getDefaultInstance().getHoverName();
            }
        }
        return Component.literal(itemId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {

        if(!Screen.hasShiftDown())
        {
            Component itemName = getTransmuteItemName().copy().withStyle(ChatFormatting.YELLOW);

            tooltipComponents.add(
                Component.translatable("tooltip.catalystgraves.obtain_info", itemName)
                        .withStyle(ChatFormatting.GRAY)
            );
            
            tooltipComponents.add(
                Component.translatable(
                            "tooltip.catalystgraves.press_shift",
                            Component.literal("SHIFT").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
    {
        if(Screen.hasShiftDown())
        {
            Component infoText = Component.translatable("tooltip.catalystgraves.catalogue.info")
                                     .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF74611)));
            return Optional.of(new CatalogueTooltipData(infoText));
        }
        return Optional.empty();
    }

    @Override
    public Component getName(ItemStack stack)
    {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        if(deltaTime > 0.1f) deltaTime = 0.1f;

        if(Screen.hasShiftDown())
        {
            shiftProgress += deltaTime * 0.75f;
            if(shiftProgress > 1.0f) shiftProgress = 1.0f;
        }
        else
        {
            shiftProgress -= deltaTime * 2.0f;
            if(shiftProgress < 0.0f) shiftProgress = 0.0f;
        }

        double wave = (Math.sin(currentTime / 250.0) + 1.0) / 2.0;

        int r1 = 255;
        int g1 = (int)(wave * 128);
        int b1 = 0;

        int r2 = 0;
        int g2 = (int)(wave * 255);
        int b2 = 170 + (int)(wave * 85);

        int finalR = (int)(r1 + (r2 - r1) * shiftProgress);
        int finalG = (int)(g1 + (g2 - g1) * shiftProgress);
        int finalB = (int)(b1 + (b2 - b1) * shiftProgress);

        int hexColor = (finalR << 16) | (finalG << 8) | finalB;

        return Component.translatable(this.getDescriptionId(stack))
            .withStyle(Style.EMPTY.withColor(hexColor));
    }

    InteractionResult failsOnLocateGrave(Player player)
    {
        player.sendSystemMessage(Component.translatable("string.grave.fail.nondeath"));
        if(player.level() instanceof ServerLevel level)
        {
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS);
        }
        return InteractionResult.CONSUME;
    }

    InteractionResult openSoulbindMenu(Player player, ServerLevel level, BlockPos pos)
    {
        player.openMenu(bindingMenu);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, .5f);
        return InteractionResult.CONSUME;
    }

    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player)
    {
        return new CatalogueMenu(i, inventory);
    }

    InteractionResult graveFailLevelNull(Player player)
    {
        player.sendSystemMessage(Component.translatable("string.grave.fail.levelnull"));
        if(player.level() instanceof ServerLevel level)
        {
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_HURT, SoundSource.PLAYERS);
        }
        return InteractionResult.CONSUME;
    }

    InteractionResult teleportSuccesfullyGrave(Player player, ServerLevel level, BlockPos pos)
    {
        DimensionTransition transition = new DimensionTransition(
            level,
            pos.relative(player.getNearestViewDirection()).getCenter(),
            Vec3.ZERO,
            0f,
            0f,
            DimensionTransition.DO_NOTHING);
        player.changeDimension(transition);
        if(level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX(), pos.getY(), pos.getZ(), 100, 1, 1, 1, 1);
            server.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, .5f, .5f);
        }
        return InteractionResult.CONSUME;
    }

    private void giveRecoveryChests(Player player, ServerLevel level, GraveBlockEntity grave)
    {
        NonNullList<ItemStack> graveItems = grave.getItems();
        NonNullList<ItemStack> curiosItems = grave.getCuriosItems();

        List<ItemStack> allItems = new ArrayList<>();
        for (ItemStack s : graveItems)
        {
            if (!s.isEmpty()) allItems.add(s.copy());
        }
        if (curiosItems != null)
        {
            for (ItemStack s : curiosItems)
            {
                if (!s.isEmpty()) allItems.add(s.copy());
            }
            curiosItems.clear();
        }
        graveItems.clear();
        grave.setChanged();

        if (allItems.isEmpty()) return;

        int chunkSize = 27;
        int totalChests = (int) Math.ceil((double) allItems.size() / chunkSize);

        for (int c = 0; c < totalChests; c++)
        {
            int start = c * chunkSize;
            int end = Math.min(start + chunkSize, allItems.size());
            List<ItemStack> subList = allItems.subList(start, end);

            NonNullList<ItemStack> chestContents = NonNullList.withSize(27, ItemStack.EMPTY);
            for (int i = 0; i < subList.size(); i++)
            {
                chestContents.set(i, subList.get(i));
            }

            ItemStack chestStack = new ItemStack(Blocks.CHEST);

            Component customName = (totalChests > 1)
                ? Component.translatable("item.catalystgraves.recovery_chest_numbered", player.getScoreboardName(), c + 1, totalChests)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                : Component.translatable("item.catalystgraves.recovery_chest", player.getScoreboardName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);

            chestStack.set(DataComponents.CUSTOM_NAME, customName);

            chestStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chestContents));

            if (!player.getInventory().add(chestStack))
            {
                player.drop(chestStack, false);
            }
        }
    }

    InteractionResult recoversSuccesfullyGrave(Player player, ServerLevel level, BlockPos deathPos, GraveBlockEntity grave)
    {
        BlockPos pos = player.blockPosition();

        giveRecoveryChests(player, level, grave);

        level.setBlockAndUpdate(deathPos, Blocks.COBBLESTONE_WALL.defaultBlockState());
        if(level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX(), pos.getY(), pos.getZ(), 100, 1, 1, 1, 1);
            server.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS);
            server.playSound(null, deathPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, .5f, .5f);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult attemptPortalRecovery(ServerPlayer player, Level level, BlockPos clickedPos, float successChance, int cooldownTicks)
    {
        if (player.getCooldowns().isOnCooldown(this))
        {
            return InteractionResult.FAIL;
        }

        boolean hasCorporeal = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.CORPOREAL_RECALL);
        if (!hasCorporeal)
        {
            player.sendSystemMessage(Component.translatable("string.grave.fail.no_advancement").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        Optional<GlobalPos> death = player.getLastDeathLocation();
        if (death.isEmpty())
            return failsOnLocateGrave(player);

        BlockPos deathPos = death.get().pos();
        ServerLevel target = level.getServer().getLevel(death.get().dimension());

        if (target == null)
            return graveFailLevelNull(player);

        if (!(target.getBlockEntity(deathPos) instanceof GraveBlockEntity grave))
        {
            player.sendSystemMessage(Component.translatable("string.grave.fail"));
            return InteractionResult.CONSUME;
        }

        player.getCooldowns().addCooldown(this, cooldownTicks);

        if (level.random.nextFloat() < successChance)
        {
            player.sendSystemMessage(Component.translatable("string.grave.portal.success").withStyle(ChatFormatting.GREEN));
            return recoversSuccesfullyGrave(player, target, deathPos, grave);
        }
        else
        {
            if (level instanceof ServerLevel server)
            {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, clickedPos.getX() + 0.5, clickedPos.getY() + 0.5, clickedPos.getZ() + 0.5, 25, 0.2, 0.2, 0.2, 0.05);
                server.playSound(null, clickedPos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
            }
            player.sendSystemMessage(Component.translatable("string.grave.portal.failed").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if(!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);

        if(state.is(Blocks.ENCHANTING_TABLE))
        {
            return openSoulbindMenu(player, player.serverLevel(), pos);
        }

        if(state.is(Blocks.ENDER_CHEST))
        {
            boolean hasCorporeal = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.CORPOREAL_RECALL);
            if(!hasCorporeal)
            {
                return InteractionResult.PASS;
            }

            Optional<GlobalPos> death = player.getLastDeathLocation();
            if(death.isEmpty())
                return failsOnLocateGrave(player);

            BlockPos deathPos = death.get().pos();
            ServerLevel target = level.getServer().getLevel(death.get().dimension());

            if(target == null)
                return graveFailLevelNull(player);

            if(target.getBlockEntity(deathPos) instanceof GraveBlockEntity grave)
                return recoversSuccesfullyGrave(player, target, deathPos, grave);
        }

        if(state.is(Blocks.NETHER_PORTAL))
        {
            return attemptPortalRecovery(player, level, pos, 0.25f, 100);
        }

        if(state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.END_GATEWAY))
        {
            return attemptPortalRecovery(player, level, pos, 0.50f, 50);
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack stack = player.getItemInHand(usedHand);

        if(player.isUsingItem())
        {
            return InteractionResultHolder.consume(stack);
        }

        if(player instanceof ServerPlayer server)
        {
            if(server.isShiftKeyDown())
            {
                ResourceLocation advancementId = ResourceLocation.parse("catalystgraves:corporeal_recall");
                AdvancementHolder advancement = server.server.getAdvancements().get(advancementId);

                if(advancement != null)
                {
                    AdvancementProgress progress = server.getAdvancements().getOrStartProgress(advancement);
                    if(!progress.isDone())
                    {
                        server.sendSystemMessage(Component.translatable("string.grave.fail.no_advancement").withStyle(ChatFormatting.RED));
                        return InteractionResultHolder.fail(stack);
                    }
                }

                List<GlobalPos> graves = GraveLogic.activeGraves.get(server.getUUID());

                if(graves == null || graves.isEmpty())
                {
                    failsOnLocateGrave(player);
                    return InteractionResultHolder.fail(stack);
                }

                while(!graves.isEmpty())
                {
                    GlobalPos targetGrave = graves.get(0);
                    BlockPos deathPos = targetGrave.pos();
                    ServerLevel targetLevel = level.getServer().getLevel(targetGrave.dimension());

                    if(targetLevel == null || !targetLevel.getBlockState(deathPos).is(CGBlocks.SIMPLE_GRAVE))
                    {
                        graves.remove(0);
                        continue;
                    }

                    graves.remove(0);

                    if(GraveLogic.instance != null)
                    {
                        GraveLogic.instance.setDirty();
                    }

                    teleportSuccesfullyGrave(player, targetLevel, deathPos);
                    return InteractionResultHolder.consume(stack);
                }

                player.sendSystemMessage(Component.translatable("string.grave.fail"));
                return InteractionResultHolder.consume(stack);
            }
            else
            {
                server.openMenu(this);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}