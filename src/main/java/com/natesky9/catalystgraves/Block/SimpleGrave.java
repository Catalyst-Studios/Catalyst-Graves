package com.natesky9.catalystgraves.Block;

import com.natesky9.DataGen.CGAdvancementProvider;
import com.natesky9.catalystgraves.GraveLogic;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SimpleGrave extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty GLOWING = BlockStateProperties.OMINOUS;
    public static IntegerProperty STYLE = BlockStateProperties.LEVEL;
    public static final MapCodec<SimpleGrave> CODEC = simpleCodec(SimpleGrave::new);
    VoxelShape SIMPLE =
            Shapes.join(Block.box(2,0,2,14,1,14),
                    Block.box(4,0,4,14,8,12),BooleanOp.AND);
    VoxelShape BASIC =
            Shapes.join(Block.box(2,0,2,14,2,14),
                    Block.box(3,2,10,13,14,14), BooleanOp.AND);
    //VoxelShape TOMBSTONE = Block.box(1,0,10,15,16,14);
    VoxelShape TOMBSTONE = Shapes.or(
            Block.box(1, 0, 9, 15, 2.5, 15), // Base
            Block.box(2, 2, 10, 14, 19, 14), // Body
            Block.box(2.8, 19, 9.5, 13.3, 21, 14.5), // Top ↓
            Block.box(4.7, 20, 9.5, 11.3, 23.5, 14.5)
            );

    public SimpleGrave(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING,Direction.NORTH)
                .setValue(GLOWING,false)
                .setValue(STYLE, Style(grave.simple)));
    }
    public enum grave{
        simple,
        basic,
        tombstone
    }
    public static int Style(grave style)
    {
        return style.ordinal();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(STYLE))
        {
            //don't recreate a voxelshape every tick, or else things *will* lag.
            //create a static final shape like the ones above
            case 0-> SIMPLE;
            case 1 -> BASIC;
            case 2 -> TOMBSTONE;
            default -> super.getShape(state,level,pos,context);
        };
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        //this prevents dangerous entities like
        //the wither and ender dragon from destroying
        return false;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!(level.getBlockEntity(pos) instanceof SimpleGraveEntity grave))
            return super.onDestroyedByPlayer(state,level,pos,player,willHarvest,fluid);

        //prevent creative players from accidentally breaking a grave,
        //and only allow them to while crouching + give them
        //chests with the contents. Otherwise, prevent players from breaking
        if (player.isCreative())
        {
            if (player.isCrouching())
            {
                GraveLogic.RestoreContents(player.level(),player,grave.getItems());
                return super.onDestroyedByPlayer(state,level,pos,player,willHarvest,fluid);
            }
            else
                if (level instanceof ServerLevel server)
                    ((ServerPlayer)player).sendSystemMessage(Component.literal("Crouch to break chest and get contents")
                            .withStyle(ChatFormatting.BLUE));
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        ServerLevel server = serverPlayer.serverLevel();

        if (level.getBlockEntity(pos) instanceof SimpleGraveEntity grave)
        {
            //region creative testing
            if (serverPlayer.isCreative())
            {

            }
            //endregion creative testing

            if (testPrivateGrave(grave, serverPlayer))
                return InteractionResult.CONSUME;

            takeGraveContents(server,grave,serverPlayer);

            //remove the grave if empty
            if (grave.getItems().isEmpty())
            {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.playSound(null,pos,SoundEvents.STONE_BREAK,SoundSource.BLOCKS);
                server.sendParticles(ParticleTypes.GLOW,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16,0,0,0,.5);
                //only apply vitality effects when the grave is emptied
                processVitality(serverPlayer,server);
            }
        }
        return InteractionResult.CONSUME;
    }
    void processVitality(ServerPlayer player, ServerLevel level)
    {
        //get the advancements and apply effects based on them
        ServerAdvancementManager manager = player.server.getAdvancements();
        AdvancementHolder vitalityHolder = manager.get(CGAdvancementProvider.LESSER_VITALITY);
        boolean hasVitality = vitalityHolder != null && player.getAdvancements().getOrStartProgress(vitalityHolder).isDone();
        AdvancementHolder majorVitalityHolder = manager.get(CGAdvancementProvider.GREATER_VITALITY);
        boolean hasGreaterVitality = majorVitalityHolder != null && player.getAdvancements().getOrStartProgress(majorVitalityHolder).isDone();

        if (hasVitality)
            refillStats(player,level);

        if (hasGreaterVitality)
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    CGConfig.VITALITY_DURATION.get(), CGConfig.VITALITY_AMPLIFIER.get()));
    }
    void refillStats(ServerPlayer player, ServerLevel level)
    {
        //refill health, air, and food for the player, with some particle effects
        player.setHealth(player.getMaxHealth());
        player.setAirSupply(player.getMaxAirSupply());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20);
        level.sendParticles(ParticleTypes.HEART,player.getX(),player.getY(),player.getZ(),8,0,0,0,.5);
        level.playSound(null,player.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS);
    }
    boolean testPrivateGrave(SimpleGraveEntity grave, ServerPlayer player)
    {
        //returns true if the "Private Graves" config is enabled
        //and the grave is not the player's
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
    void takeGraveContents(ServerLevel server, SimpleGraveEntity grave, ServerPlayer player)
    {
        ServerAdvancementManager manager = player.server.getAdvancements();

        AdvancementHolder desecrateHolder = manager.get(CGAdvancementProvider.DESECRATE);
        AdvancementHolder organizedHolder = manager.get(CGAdvancementProvider.ORGANIZATION);
        boolean hasDesecrate = desecrateHolder != null && player.getAdvancements().getOrStartProgress(desecrateHolder).isDone();
        boolean hasOrganized = organizedHolder != null && player.getAdvancements().getOrStartProgress(organizedHolder).isDone();

        List<ItemStack> items = GraveLogic.getSnapshot(player);

        if (hasDesecrate || CGConfig.DESECRATE_COST.get() == 0)
        {
            if (hasOrganized || CGConfig.ORGANIZATION_COST.get() == 0)
            {
                for (int i = 0; i<player.getInventory().getContainerSize(); i++)
                {
                    if (grave.getItems().isEmpty()) break;
                    ItemStack stack = grave.getItems().getFirst();
                    ItemStack expected = items.get(i);
                    if (expected.isEmpty()) continue;
                    if (expected.is(stack.getItem()) && player.getInventory().getFreeSlot() != -1)
                    {
                        ItemStack present = player.getInventory().getItem(i);
                        player.getInventory().setItem(i,grave.remove());
                        if (!present.isEmpty())
                            player.getInventory().add(present);
                    }
                }
            while (!grave.getItems().isEmpty() && player.getInventory().getFreeSlot() != -1)
                {
                player.addItem(grave.remove());
                }
                server.playSound(null,player.blockPosition(),SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
            }
            else
            {
                while (!grave.getItems().isEmpty() && player.getInventory().getFreeSlot() != -1)
                {
                    player.addItem(grave.remove());
                    server.playSound(null,player.blockPosition(),SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
                }
            }
        }
        else
        {
            if (player.addItem(grave.getItems().getFirst()))
            {
                server.playSound(null,player.blockPosition(),
                        SoundEvents.ITEM_PICKUP,SoundSource.PLAYERS);
                grave.remove();
            }
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SimpleGraveEntity(blockPos,blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        //shouldn't be needed for this block, as the player is never placing it by hand,
        //but still good practice to keep it in as reference
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(GLOWING,false)
                .setValue(STYLE,Style(grave.simple));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(GLOWING).add(STYLE);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        //grave logic HAS to be called before super,
        //as super.onRemove clears the entity at this position
        //TODO: Since the command to restore items has been added, this has to be removed to prevent item duping.
        //prevent the block from being broken by normal means to avoid lost items
        //or add in custom logic to handle that
        //if (level.getBlockEntity(pos) instanceof SimpleGraveEntity grave)
        //    grave.dropItems();
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
