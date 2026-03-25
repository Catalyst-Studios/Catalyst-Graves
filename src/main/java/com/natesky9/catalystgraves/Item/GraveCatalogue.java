package com.natesky9.catalystgraves.Item;

import com.natesky9.catalystgraves.Block.GraveLogic;
import com.natesky9.catalystgraves.Block.SimpleGraveEntity;
import com.natesky9.catalystgraves.Init.CGBlocks;
import com.natesky9.catalystgraves.client.menus.CatalogueMenu;
import com.natesky9.catalystgraves.client.tooltip.CatalogueTooltipData;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Main item for the Catalyst Graves mod.
 * Handles grave teleportation, remote recovery via Ender Chests,
 * and provides access to the Soulbinding interface.
 */
public class GraveCatalogue extends Item implements MenuProvider
{
    private static float shiftProgress = 0.0f;
    private static long lastTime = System.currentTimeMillis();
    private BindingChapter bindingMenu = new BindingChapter();

    public GraveCatalogue(Properties properties)
    {
        super(properties);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.catalogue");
    }

    @SuppressWarnings("null")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {
        // Show "Press SHIFT" prompt when not crouching
        if(!Screen.hasShiftDown())
        {
            tooltipComponents.add(
                Component.translatable(
                             "tooltip.catalystgraves.press_shift",
                             Component.literal("SHIFT").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(@SuppressWarnings("null") ItemStack stack)
    {
        // Provide detailed info data when SHIFT is held
        if(Screen.hasShiftDown())
        {
            @SuppressWarnings("null")
            Component infoText = Component.translatable("tooltip.catalystgraves.catalogue.info")
                                     .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF74611)));
            return Optional.of(new CatalogueTooltipData(infoText));
        }
        return Optional.empty();
    }

    @SuppressWarnings("null")
    @Override
    public Component getName(ItemStack stack)
    {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        if(deltaTime > 0.1f) deltaTime = 0.1f;

        // Transition logic: Shift increases progress, releasing decreases it
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

        // Constant pulse wave for color animation
        double wave = (Math.sin(currentTime / 250.0) + 1.0) / 2.0;

        // Normal State: Red to Orange
        int r1 = 255;
        int g1 = (int)(wave * 128);
        int b1 = 0;

        // Shift State: Dark Blue to Cyan
        int r2 = 0;
        int g2 = (int)(wave * 255);
        int b2 = 170 + (int)(wave * 85);

        // Color interpolation based on shift progress
        int finalR = (int)(r1 + (r2 - r1) * shiftProgress);
        int finalG = (int)(g1 + (g2 - g1) * shiftProgress);
        int finalB = (int)(b1 + (b2 - b1) * shiftProgress);

        int hexColor = (finalR << 16) | (finalG << 8) | finalB;

        return Component.translatable(this.getDescriptionId(stack))
            .withStyle(Style.EMPTY.withColor(hexColor));
    }

    @SuppressWarnings("null")
    InteractionResult failsOnLocateGrave(Player player)
    {
        player.sendSystemMessage(Component.translatable("string.grave.fail.nondeath"));
        if(player.level() instanceof ServerLevel level)
        {
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS);
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    InteractionResult openSoulbindMenu(Player player, ServerLevel level, BlockPos pos)
    {
        player.openMenu(bindingMenu);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, .5f);
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player)
    {
        return new CatalogueMenu(i, inventory);
    }

    @SuppressWarnings("null")
    InteractionResult graveFailLevelNull(Player player)
    {
        player.sendSystemMessage(Component.translatable("string.grave.fail.levelnull"));
        if(player.level() instanceof ServerLevel level)
        {
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_HURT, SoundSource.PLAYERS);
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    InteractionResult teleportSuccesfullyGrave(Player player, ServerLevel level, BlockPos pos)
    {
        DimensionTransition transition = new DimensionTransition(level, pos.relative(player.getNearestViewDirection()).getCenter(), Vec3.ZERO,
                                                                 0f, 0f, DimensionTransition.DO_NOTHING);
        player.changeDimension(transition);
        if(level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX(), pos.getY(), pos.getZ(), 100, 1, 1, 1, 1);
            server.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, .5f, .5f);
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    InteractionResult recoversSuccesfullyGrave(Player player, ServerLevel level, BlockPos deathPos, SimpleGraveEntity grave)
    {
        BlockPos pos = player.blockPosition();
        GraveLogic.RestoreContents(level, player, grave.getItems());
        level.setBlockAndUpdate(deathPos, Blocks.COBBLESTONE_WALL.defaultBlockState());
        if(level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX(), pos.getY(), pos.getZ(), 100, 1, 1, 1, 1);
            server.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS);
            server.playSound(null, deathPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, .5f, .5f);
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("null")
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if(!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;

        // Open Soulbound UI on Enchanting Table
        if(level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE))
        {
            return openSoulbindMenu(player, player.serverLevel(), pos);
        }

        // Remote grave recovery via Ender Chest
        if(level.getBlockState(pos).is(Blocks.ENDER_CHEST))
        {
            if(!(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;

            ServerAdvancementManager manager = serverPlayer.server.getAdvancements();
            AdvancementHolder corporealHolder = manager.get(CGAdvancementProvider.CORPOREAL_RECALL);
            boolean hasCorporeal = corporealHolder != null && serverPlayer.getAdvancements().getOrStartProgress(corporealHolder).isDone();

            if(!hasCorporeal)
            {
                return InteractionResult.PASS;
            }

            Optional<GlobalPos> death = serverPlayer.getLastDeathLocation();
            if(death.isEmpty())
                return failsOnLocateGrave(serverPlayer);

            BlockPos deathPos = death.get().pos();
            ServerLevel target = level.getServer().getLevel(death.get().dimension());

            if(target == null)
                return graveFailLevelNull(serverPlayer);

            if(target.getBlockEntity(deathPos) instanceof SimpleGraveEntity grave)
                return recoversSuccesfullyGrave(serverPlayer, target, deathPos, grave);
        }

        return super.useOn(context);
    }

    @SuppressWarnings("null")
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
            // Logic for Sneak + Right Click (Teleportation)
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

                // Find the first valid grave in the history list
                while(!graves.isEmpty())
                {
                    GlobalPos targetGrave = graves.get(0);
                    BlockPos deathPos = targetGrave.pos();
                    ServerLevel targetLevel = level.getServer().getLevel(targetGrave.dimension());

                    // Remove invalid or destroyed graves from the list
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
            // Logic for Normal Right Click (Open Menu)
            else
            {
                server.openMenu(this);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}