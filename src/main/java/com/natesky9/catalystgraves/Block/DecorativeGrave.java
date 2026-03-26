package com.natesky9.catalystgraves.Block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class DecorativeGrave extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final int style;

    private static final VoxelShape SIMPLE = Shapes.or(Block.box(2, 0, 2, 14, 1, 14),
                                                       Block.box(4, 0, 10, 14, 8, 12));

    private static final VoxelShape BASIC =
        Shapes.or(Block.box(1, 0, 10, 15, 12, 14),
                  Block.box(2, 12, 10, 14, 14, 14),
                  Block.box(5, 14, 10, 11, 16, 14));

    private static final VoxelShape TOMBSTONE = Shapes.or(
        Block.box(1, 0, 9, 15, 2.5, 15), // Base
        Block.box(2, 2, 10, 14, 19, 14), // Body
        Block.box(2.8, 19, 9.5, 13.3, 21, 14.5), // Top ↓
        Block.box(4.7, 20, 9.5, 11.3, 23.5, 14.5));

    private static final VoxelShape ROYALGRAVE = Shapes.or(
        Block.box(4, 0, 4, 12, 2, 12),
        Block.box(5, 2, 5, 11, 5, 11),
        Block.box(4, 5, 5, 12, 9, 14),
        Block.box(4, 7, 3, 12, 15, 6),
        Block.box(4, 4.9, 5.0, 12, 7.9, 7.0),
        Block.box(5, 7, 7, 11, 9.1, 13),
        Block.box(9.9, 12.3, 5.7, 11.9, 12.5, 6.3),
        Block.box(4, 9.1, 6, 12, 9.2, 14));

    public DecorativeGrave(Properties properties, int style)
    {
        super(properties);
        this.style = style;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch(this.style)
        {
            case 0 -> SIMPLE;
            case 1 -> BASIC;
            case 2 -> TOMBSTONE;
            case 3 -> ROYALGRAVE;
            default -> SIMPLE;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }
}