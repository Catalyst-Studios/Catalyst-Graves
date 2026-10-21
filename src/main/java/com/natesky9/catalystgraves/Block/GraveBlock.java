package com.natesky9.catalystgraves.block;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.compat.curios.CuriosCompat;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.natesky9.catalystgraves.server.GraveLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("null")
public class GraveBlock extends BaseEntityBlock
{
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty GLOWING = BlockStateProperties.OMINOUS;
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 0, 3);
    public static final MapCodec<GraveBlock> CODEC = simpleCodec(GraveBlock::new);

    public GraveBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(GLOWING, false)
            .setValue(STYLE, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        int style = state.getValue(STYLE);
        Direction facing = state.getValue(FACING);
        return GraveStyles.getShape(style, facing);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity)
    {
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave))
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);

        if (player.isCreative())
        {
            if (player.isCrouching())
            {
                GraveLogic.RestoreContents(player.level(), player, grave.getItems(), GlobalPos.of(level.dimension(), pos));
                return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
            }
            else if (level instanceof ServerLevel)
            {
                player.sendSystemMessage(Component.translatable("message.catalystgraves.crouch_to_break")
                                          .withStyle(ChatFormatting.BLUE));
            }
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        ServerLevel server = serverPlayer.serverLevel();

        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave)
        {
            if (testPrivateGrave(grave, serverPlayer))
            {
                return InteractionResult.CONSUME;
            }

            if (!grave.isVitalityClaimed())
            {
                processVitality(serverPlayer, server);
                grave.setVitalityClaimed(true);
            }

            takeGraveContents(server, grave, serverPlayer);

            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS);
            server.sendParticles(ParticleTypes.GLOW, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 16, 0, 0, 0, .5);
        }
        return InteractionResult.CONSUME;
    }

    void processVitality(ServerPlayer player, ServerLevel level)
    {
        boolean hasGreaterVitality = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.GREATER_VITALITY);
        boolean hasLesserVitality = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.LESSER_VITALITY);

        player.setAirSupply(player.getMaxAirSupply());
        if (hasGreaterVitality)
        {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().eat(12, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1));

            int duration = CGConfig.VITALITY_DURATION.get() > 0 ? CGConfig.VITALITY_DURATION.get() : 1200;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0));
        }
        else if (hasLesserVitality)
        {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().eat(6, 0.5F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 0));
        }
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 8, 0, 0, 0, .5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS);
    }

    boolean testPrivateGrave(GraveBlockEntity grave, ServerPlayer player)
    {
        UUID graveUUID = grave.getUuid();
        if (CGConfig.PRIVATE_GRAVES.get() && !player.getUUID().equals(graveUUID))
        {
            player.displayClientMessage(Component.translatable("string.grave.private")
                                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                                        true);
            return true;
        }
        return false;
    }

    void takeGraveContents(ServerLevel server, GraveBlockEntity grave, ServerPlayer player)
    {
        GlobalPos gravePos = GlobalPos.of(server.dimension(), grave.getBlockPos());

        if (GraveLogic.graveXp.containsKey(gravePos))
        {
            int xp = GraveLogic.graveXp.remove(gravePos);
            if (xp > 0)
            {
                player.giveExperiencePoints(xp);
                server.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (GraveLogic.instance != null) GraveLogic.instance.setDirty();
        }

        if(ModList.get().isLoaded("curios"))
        {
            CuriosCompat.restoreCurios(player, grave.getCuriosItems());
            grave.getCuriosItems().clear();
        }

        Inventory playerInv = player.getInventory();
        int invSize = playerInv.getContainerSize();
        NonNullList<ItemStack> items = grave.getItems();

        for (int i = 0; i < items.size(); i++)
        {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            ItemStack copy = stack.copy();
            if (i < invSize && playerInv.getItem(i).isEmpty())
            {
                playerInv.setItem(i, copy);
            }
            else if (!playerInv.add(copy))
            {
                player.drop(copy, true);
            }
            items.set(i, ItemStack.EMPTY);
        }

        grave.setChanged();
        server.setBlockAndUpdate(grave.getBlockPos(), Blocks.AIR.defaultBlockState());
        server.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new GraveBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection())
            .setValue(GLOWING, false)
            .setValue(STYLE, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING).add(GLOWING).add(STYLE);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GraveBlockEntity grave)
            {
                if (grave.getUuid() != null)
                {
                    GraveLogic.removeGrave(grave.getUuid(), GlobalPos.of(level.dimension(), pos));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    {
        return PushReaction.BLOCK;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos)
    {
        return player.isCreative() ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion)
    {
        return false;
    }
}