package com.natesky9.catalystgraves.Block;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.compact.CuriosCompat;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("null")
public class SimpleGrave extends BaseEntityBlock
{
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty GLOWING = BlockStateProperties.OMINOUS;
    public static IntegerProperty STYLE = BlockStateProperties.LEVEL;
    public static final MapCodec<SimpleGrave> CODEC = simpleCodec(SimpleGrave::new);

    VoxelShape SIMPLE = Shapes.or(Block.box(2, 0, 2, 14, 1, 14),
                                  Block.box(4, 0, 10, 14, 8, 12));

    VoxelShape BASIC =
        Shapes.or(Block.box(1, 0, 10, 15, 12, 14),
                  Block.box(2, 12, 10, 14, 14, 14),
                  Block.box(5, 14, 10, 11, 16, 14));

    VoxelShape TOMBSTONE = Shapes.or(
        Block.box(1, 0, 9, 15, 2.5, 15), // Base
        Block.box(2, 2, 10, 14, 19, 14), // Body
        Block.box(2.8, 19, 9.5, 13.3, 21, 14.5), // Top ↓
        Block.box(4.7, 20, 9.5, 11.3, 23.5, 14.5));

    public SimpleGrave(Properties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(GLOWING, false).setValue(STYLE, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {

        return switch(state.getValue(STYLE))
        {
            case 0 -> SIMPLE;
            case 1 -> BASIC;
            case 2 -> TOMBSTONE;
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity)
    {
        // this prevents dangerous entities like
        // the wither and ender dragon from destroying
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        if(!(level.getBlockEntity(pos) instanceof SimpleGraveEntity grave))
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);

        // prevent creative players from accidentally breaking a grave,
        // and only allow them to while crouching + give them
        // chests with the contents. Otherwise, prevent players from breaking
        if(player.isCreative())
        {
            if(player.isCrouching())
            {
                GraveLogic.RestoreContents(player.level(), player, grave.getItems());
                return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
            }
            else if(level instanceof ServerLevel)
                player.sendSystemMessage(Component.translatable("message.catalystgraves.crouch_to_break")
                                  .withStyle(ChatFormatting.BLUE));
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if(!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        ServerLevel server = serverPlayer.serverLevel();

        if(level.getBlockEntity(pos) instanceof SimpleGraveEntity grave)
        {
            if(testPrivateGrave(grave, serverPlayer))
                return InteractionResult.CONSUME;

            takeGraveContents(server, grave, serverPlayer);

            if(!grave.isVitalityClaimed())
            {
                processVitality(serverPlayer, server);
                grave.setVitalityClaimed(true);
            }

            // remove the grave if empty
            if(grave.getItems().isEmpty())
            {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS);
                server.sendParticles(ParticleTypes.GLOW, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 16, 0, 0, 0, .5);
            }
        }
        return InteractionResult.CONSUME;
    }

    void processVitality(ServerPlayer player, ServerLevel level)
    {
        boolean hasGreaterVitality = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.GREATER_VITALITY);
        boolean hasLesserVitality = AdvancementLogic.hasAdvancement(player, CGAdvancementProvider.LESSER_VITALITY);

        player.setAirSupply(player.getMaxAirSupply());
        if(hasGreaterVitality)
        {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().eat(12, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1));

            int duration = CGConfig.VITALITY_DURATION.get() > 0 ? CGConfig.VITALITY_DURATION.get() : 1200;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0));
        }
        else if(hasLesserVitality)
        {
            player.setHealth(player.getMaxHealth());

            player.getFoodData().eat(6, 0.5F);

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 0));
        }
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(), player.getZ(), 8, 0, 0, 0, .5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS);
    }

    boolean testPrivateGrave(SimpleGraveEntity grave, ServerPlayer player)
    {
        // returns true if the "Private Graves" config is enabled
        // and the grave is not the player's
        UUID graveUUID = grave.getUuid();
        if(CGConfig.PRIVATE_GRAVES.get() && !player.getUUID().equals(graveUUID))
        {
            player.displayClientMessage(Component.translatable("string.grave.private")
                                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                                        true);
            return true;
        }
        return false;
    }

    void takeGraveContents(ServerLevel server, SimpleGraveEntity grave, ServerPlayer player)
    {
        if(CuriosCompat.isLoaded())
        {
            CuriosCompat.restoreCurios(player, grave.getCuriosItems());
            grave.getCuriosItems().clear();
        }

        Inventory playerInv = player.getInventory();
        int size = Math.min(playerInv.getContainerSize(), grave.getItems().size());

        for(int i = 0; i < size; i++)
        {
            ItemStack stacksInGrave = grave.getItems().get(i);

            if(!stacksInGrave.isEmpty())
            {
                if(playerInv.getItem(i).isEmpty())
                {
                    playerInv.setItem(i, stacksInGrave.copy());
                }
                else
                {
                    if(!playerInv.add(stacksInGrave.copy()))
                    {
                        player.drop(stacksInGrave.copy(), true);
                    }
                }
                grave.getItems().set(i, ItemStack.EMPTY);
            }
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
        return new SimpleGraveEntity(blockPos, blockState);
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
        // shouldn't be needed for this block, as the player is never placing it by hand,
        // but still good practice to keep it in as reference
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
        if(!state.is(newState.getBlock()))
        {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if(blockEntity instanceof SimpleGraveEntity grave)
            {
                if(grave.getUuid() != null)
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
