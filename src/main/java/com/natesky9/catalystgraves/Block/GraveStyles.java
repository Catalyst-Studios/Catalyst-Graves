package com.natesky9.catalystgraves.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class GraveStyles
{
    public record StyleDefinition(
        int id,
        String name,
        String translationKey,
        Map<Direction, VoxelShape> shapes
    ) {}

    private static final List<StyleDefinition> STYLES = new ArrayList<>();

    public static final StyleDefinition SIMPLE = registerStyle("simple", "block.catalystgraves.decorative_grave_simple",
        Shapes.or(
            Block.box(2, 0, 2, 14, 1, 14),
            Block.box(4, 0, 10, 14, 8, 12)
        )
    );

    public static final StyleDefinition BASIC = registerStyle("basic", "block.catalystgraves.decorative_grave_basic",
        Shapes.or(
            Block.box(1, 0, 10, 15, 12, 14),
            Block.box(2, 12, 10, 14, 14, 14),
            Block.box(5, 14, 10, 11, 16, 14)
        )
    );

    public static final StyleDefinition TOMBSTONE = registerStyle("tombstone", "block.catalystgraves.decorative_grave_tombstone",
        Shapes.or(
            Block.box(1, 0, 9, 15, 2.5, 15),
            Block.box(2, 2, 10, 14, 19, 14),
            Block.box(2.8, 19, 9.5, 13.3, 21, 14.5),
            Block.box(4.7, 20, 9.5, 11.3, 23.5, 14.5)
        )
    );

    public static final StyleDefinition ROYAL = registerStyle("royal", "block.catalystgraves.decorative_grave_royal",
        Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),
            Block.box(5, 2, 5, 11, 5, 11),
            Block.box(4, 5, 5, 12, 9, 14),
            Block.box(4, 7, 3, 12, 15, 6),
            Block.box(4, 4.9, 5.0, 12, 7.9, 7.0),
            Block.box(5, 7, 7, 11, 9.1, 13),
            Block.box(9.9, 12.3, 5.7, 11.9, 12.5, 6.3),
            Block.box(4, 9.1, 6, 12, 9.2, 14)
        )
    );

    public static synchronized StyleDefinition registerStyle(String name, String translationKey, VoxelShape northShape)
    {
        int id = STYLES.size();
        Map<Direction, VoxelShape> shapes = buildRotatedShapes(northShape);
        StyleDefinition def = new StyleDefinition(id, name, translationKey, shapes);
        STYLES.add(def);
        return def;
    }

    public static int getStyleCount()
    {
        return STYLES.size();
    }

    public static List<StyleDefinition> getStyles()
    {
        return Collections.unmodifiableList(STYLES);
    }

    public static StyleDefinition getStyle(int id)
    {
        if (id < 0 || id >= STYLES.size())
        {
            return STYLES.get(0);
        }
        return STYLES.get(id);
    }

    public static VoxelShape getShape(int styleId, Direction facing)
    {
        StyleDefinition style = getStyle(styleId);
        Direction dir = (facing == null || facing.getAxis() == Direction.Axis.Y) ? Direction.NORTH : facing;
        return style.shapes().getOrDefault(dir, style.shapes().get(Direction.NORTH));
    }

    public static Map<Direction, VoxelShape> buildRotatedShapes(VoxelShape north)
    {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, rotateShape(north, Direction.SOUTH));
        map.put(Direction.WEST, rotateShape(north, Direction.WEST));
        map.put(Direction.EAST, rotateShape(north, Direction.EAST));
        return Collections.unmodifiableMap(map);
    }

    public static VoxelShape rotateShape(VoxelShape source, Direction target)
    {
        if (target == Direction.NORTH) return source;
        final VoxelShape[] result = new VoxelShape[]{Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape rotatedBox = switch (target)
            {
                case SOUTH -> Shapes.box(1.0 - maxX, minY, 1.0 - maxZ, 1.0 - minX, maxY, 1.0 - minZ);
                case WEST  -> Shapes.box(minZ, minY, 1.0 - maxX, maxZ, maxY, 1.0 - minX);
                case EAST  -> Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX);
                default    -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            result[0] = Shapes.joinUnoptimized(result[0], rotatedBox, BooleanOp.OR);
        });
        return result[0].optimize();
    }
}
