package com.water2h2o.elementalverse.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 石制密封仓：9 格容器，里面的元素衰变速度 ×0.5。
 *
 * <p>定位是"主力仓储"——便宜、能盖一片，但要占地方。
 * 界面直接复用原版发射器的 3×3 布局与贴图（作者要求"和发射器一样"）。
 */
public class SealedVaultBlock extends BaseEntityBlock {

    public static final MapCodec<SealedVaultBlock> CODEC = simpleCodec(SealedVaultBlock::new);

    public SealedVaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 普通方块外形，不需要额外的方块实体渲染器
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SealedVaultBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SealedVaultBlockEntity vault) {
                // 把坐标传给客户端，界面那侧才能找到这个方块实体
                player.openMenu(vault, buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container container) {
                Containers.dropContents(level, pos, container);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
