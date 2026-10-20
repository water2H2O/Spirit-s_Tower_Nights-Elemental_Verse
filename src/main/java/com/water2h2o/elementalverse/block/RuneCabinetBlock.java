package com.water2h2o.elementalverse.block;

import com.mojang.serialization.MapCodec;
import com.water2h2o.elementalverse.element.DeclarationPlateItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 符文柜：27 格，通电时**完全不衰变**。
 *
 * <p>喂法：手持**霜/冰元素瓶**或写好的**霜/冰申明板**右键；其它元素一律拒绝。
 * 申明板按档位放大维持时间（霜 0.5/1/2 日、冰 1/2/4 日）。
 */
public class RuneCabinetBlock extends BaseEntityBlock {

    public static final MapCodec<RuneCabinetBlock> CODEC = simpleCodec(RuneCabinetBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RuneCabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneCabinetBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // 手持冷源右键 → 尝试喂冷却；空瓶/其它物品照常开界面。
        // 冷源可以是元素瓶（霜/冰），也可以是写好的霜/冰申明板。
        if (isCarrierWithElement(stack) || isWrittenColdPlate(stack)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RuneCabinetBlockEntity cabinet) {
                if (cabinet.feed(stack)) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.3F);
                } else {
                    // 把手里那种元素的名字带出来，否则玩家不知道被拒的原因
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    com.water2h2o.elementalverse.util.EVLang.MSG_ONLY_COLD,
                                    chargeElement(stack).displayName()), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private static boolean isCarrierWithElement(ItemStack stack) {
        return com.water2h2o.elementalverse.element.ElementCarrier.isCarrier(stack)
                && !com.water2h2o.elementalverse.element.ElementVialItem.getElement(stack).isEmpty();
    }

    private static boolean isWrittenColdPlate(ItemStack stack) {
        if (!stack.is(com.water2h2o.elementalverse.registry.EVItems.DECLARATION_PLATE.get())) {
            return false;
        }
        var declaration = DeclarationPlateItem.getDeclaration(stack);
        return declaration.isDefined() && RuneCabinetBlockEntity.isCold(declaration.element());
    }

    private static com.water2h2o.elementalverse.element.ElementType chargeElement(ItemStack stack) {
        if (com.water2h2o.elementalverse.element.ElementCarrier.isCarrier(stack)) {
            return com.water2h2o.elementalverse.element.ElementVialItem.getElement(stack);
        }
        return DeclarationPlateItem.getDeclaration(stack).element();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RuneCabinetBlockEntity cabinet) {
            player.openMenu(cabinet, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof RuneCabinetBlockEntity cabinet) {
                cabinet.serverTick();
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof RuneCabinetBlockEntity cabinet) {
                cabinet.dropContents(level, pos);
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
