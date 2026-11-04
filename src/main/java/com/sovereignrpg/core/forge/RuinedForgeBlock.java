package com.sovereignrpg.core.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RuinedForgeBlock extends HorizontalDirectionalBlock {
    public RuinedForgeBlock() {
        super(Properties.of().mapColor(MapColor.STONE).strength(3.5f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    // an anvil with a coal basin on the plate; X-long when facing north/south, Z-long when facing east/west
    private static final VoxelShape BASIN = Shapes.or(Block.box(3, 16, 5, 13, 18, 11));
    private static final VoxelShape X_SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(3, 4, 4, 13, 5, 12),
        Block.box(4, 5, 6, 12, 10, 10), Block.box(0, 10, 3, 16, 16, 13), BASIN);
    private static final VoxelShape Z_SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 3, 12, 5, 13),
        Block.box(6, 5, 4, 10, 10, 12), Block.box(3, 10, 0, 13, 16, 16), Block.box(5, 16, 3, 11, 18, 13));

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        net.minecraft.core.Direction d = s.getValue(FACING);
        return d == net.minecraft.core.Direction.NORTH || d == net.minecraft.core.Direction.SOUTH ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult r) {
        if (l.isClientSide) return InteractionResult.SUCCESS;
        pl.openMenu(s.getMenuProvider(l, p));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState s, Level l, BlockPos p) {
        return new SimpleMenuProvider((id, inv, pl) -> new ForgeMenu(id, inv, ContainerLevelAccess.create(l, p)), Component.literal("Ruined Forge"));
    }
}
