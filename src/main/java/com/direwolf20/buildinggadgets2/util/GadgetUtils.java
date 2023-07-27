package com.direwolf20.buildinggadgets2.util;

import com.direwolf20.buildinggadgets2.common.blocks.RenderBlock;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.datagen.BG2BlockTags;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.UUID;

public class GadgetUtils {
    private static final ImmutableList<Property<?>> ALLOWED_PROPERTIES = ImmutableList.of(
            BlockStateProperties.FACING, BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.CHEST_TYPE
    );

    public static boolean isValidBlockState(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.is(BG2BlockTags.BG2DENY)) return false;
        if (blockState.getDestroySpeed(level, blockPos) < 0) return false;
        return true;
    }

    public static boolean isValidDestroyBlockState(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.isAir()) return false;
        if (blockState.getDestroySpeed(level, blockPos) < 0) return false;
        if (blockState.getBlock() instanceof RenderBlock) return false;
        return true;
    }

    public static boolean setBlockState(ItemStack gadget, BlockState blockState) {
        BlockState placeState = cleanBlockState(blockState);
        GadgetNBT.setGadgetBlockState(gadget, placeState);
        return true;
    }

    public static ItemStack getItemForBlock(BlockState blockState) {
        return new ItemStack(blockState.getBlock().asItem(), 1);
    }

    public static BlockState cleanBlockState(BlockState sourceState) {
        BlockState placeState = sourceState.getBlock().defaultBlockState();
        for (Property<?> prop : sourceState.getProperties()) {
            if (ALLOWED_PROPERTIES.contains(prop)) {
                placeState = applyProperty(placeState, sourceState, prop);
            }
        }
        return placeState;
    }

    public static void addToUndoList(Level level, ItemStack gadget, ArrayList<StatePos> buildList) {
        BG2Data bg2Data = BG2Data.get(level.getServer().overworld()); //TODO NPE?
        UUID uuid = UUID.randomUUID();
        bg2Data.addToUndoList(uuid, buildList, level);
        GadgetNBT.addToUndoList(gadget, uuid);
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, BlockState from, Property<T> prop) {
        return state.setValue(prop, from.getValue(prop));
    }

    public static AABB getSquareArea(BlockPos pos, Direction face, int range) {
        switch (face) {
            case UP:
            case DOWN:
                // If you're looking up or down, the area will extend east-west and north-south
                return new AABB(pos.getX() - range, pos.getY(), pos.getZ() - range, pos.getX() + range, pos.getY(), pos.getZ() + range);
            case NORTH:
            case SOUTH:
                // If you're looking north or south, the area will extend up-down and east-west
                return new AABB(pos.getX() - range, pos.getY() - range, pos.getZ(), pos.getX() + range, pos.getY() + range, pos.getZ());
            case EAST:
            case WEST:
                // If you're looking east or west, the area will extend up-down and north-south
                return new AABB(pos.getX(), pos.getY() - range, pos.getZ() - range, pos.getX(), pos.getY() + range, pos.getZ() + range);
            default:
                throw new IllegalStateException("Unexpected value: " + face);
        }
    }

    public static ArrayList<StatePos> getDestructionArea(Level level, BlockPos pos, Direction face, Player player, ItemStack gadget) {
        int depth = GadgetNBT.getToolValue(gadget, "depth");

        if (gadget.isEmpty() || depth == 0 || !player.mayBuild())
            return new ArrayList<>();

        boolean vertical = face.getAxis().isVertical();
        Direction up = vertical ? player.getDirection() : Direction.UP;
        Direction down = up.getOpposite();
        Direction right = vertical ? up.getClockWise() : face.getCounterClockWise();
        Direction left = right.getOpposite();

        BlockPos first = pos.relative(left, GadgetNBT.getToolValue(gadget, "left")).relative(up, GadgetNBT.getToolValue(gadget, "up"));
        BlockPos second = pos.relative(right, GadgetNBT.getToolValue(gadget, "right"))
                .relative(down, GadgetNBT.getToolValue(gadget, "down"))
                .relative(face.getOpposite(), depth - 1);

        //boolean isFluidOnly = getIsFluidOnly(gadget); //Todo
        AABB box = new AABB(first, second);
        ArrayList<StatePos> returnList = new ArrayList<>();
        BlockPos.betweenClosedStream(box).map(BlockPos::immutable).forEach(blockPos -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.hasBlockEntity() && !GadgetNBT.getSetting(gadget, "affecttiles"))
                return;
            if (isValidDestroyBlockState(blockState, level, blockPos)) //Todo more validations?
                returnList.add(new StatePos(blockState, blockPos));
        });
        return returnList;
    }

    //Because contains doesn't use <= just <
    public static boolean direContains(AABB aabb, double x, double y, double z) {
        return x >= aabb.minX && x <= aabb.maxX && y >= aabb.minY && y <= aabb.maxY && z >= aabb.minZ && z <= aabb.maxZ;
    }

    public static boolean direContains(AABB aabb, BlockPos pos) {
        return direContains(aabb, pos.getX(), pos.getY(), pos.getZ());
    }
}
