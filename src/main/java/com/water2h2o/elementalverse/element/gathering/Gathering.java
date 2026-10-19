package com.water2h2o.elementalverse.element.gathering;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementStackMerger;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 采集系统的统一入口。
 *
 * <h2>装填方式：按"水瓶模式"</h2>
 * 一次右键**消耗手里一个空容器 → 得到一个装好的容器**。
 * 产出 N 份的元素（岩浆 5、钻石矿 3）就消耗 N 个空容器；
 * 手里不够就只装够的数量。
 *
 * <p>早先的实现直接对整格堆叠写数据组件，结果 16 个空瓶一次全装满 —— 这是个真 bug。
 */
public final class Gathering {

    /** 伸手距离。原版是 4.5 格。 */
    private static final double REACH = 4.5D;

    /** 岩浆采集的扣血（6 点 = 3 颗心）。 */
    private static final float LAVA_DAMAGE = 6.0F;

    /** 岩浆一次给几份。 */
    private static final int LAVA_YIELD = 5;

    private Gathering() {
    }

    /** 物品 use() 的入口。 */
    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 已经有东西 → 静默（作者定）
        if (!ElementVialItem.getElement(stack).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        ElementCarrier carrier = ElementCarrier.of(stack);
        if (carrier == null) {
            return InteractionResultHolder.pass(stack);
        }

        return switch (carrier) {
            case REFRACTION_PRISM -> gatherLight(level, player, hand, stack);
            case SHADOW_BOTTLE -> gatherShadow(level, player, hand, stack);
            case ELEMENT_VIAL -> gatherWithVial(level, player, hand, stack);
        };
    }

    // ---------------------------------------------------------------- 元素瓶

    private static InteractionResultHolder<ItemStack> gatherWithVial(Level level, Player player,
                                                                     InteractionHand hand, ItemStack stack) {
        BlockHitResult hit = pick(level, player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return gatherWind(level, player, hand, stack);
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // ① 岩浆：扣血、一次 5 份（作者定：不消耗容器，改成扣血）
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return give(level, player, hand, stack, ElementType.FIRE, LAVA_YIELD, null, () -> {
                player.hurt(level.damageSources().lava(), LAVA_DAMAGE);
                player.setRemainingFireTicks(60);
            });
        }

        // ② 水：1 份
        if (state.getFluidState().is(FluidTags.WATER)) {
            return give(level, player, hand, stack, ElementType.WATER, 1, null, null);
        }

        // ③ 营火：给烬；能采 5 次，之后熄灭（作者定）
        if (state.getBlock() instanceof CampfireBlock) {
            if (!BlockUses.canGather(state)) {
                return InteractionResultHolder.pass(stack);
            }
            return give(level, player, hand, stack, ElementType.EMBER, 1,
                    () -> BlockUses.consume(level, pos, state),
                    () -> level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.3F));
        }

        // ④ 火焰方块：不扣血，1 份，火被扑灭
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return give(level, player, hand, stack, ElementType.FIRE, 1,
                    () -> level.removeBlock(pos, false), null);
        }

        // ⑤ 冰类：1 份，方块越来越薄，并带破碎音效与粒子（作者定）
        if (BlockUses.canGather(state) && BlockUses.kindOf(state) == BlockUses.Kind.ICE) {
            return give(level, player, hand, stack, ElementType.ICE, 1,
                    () -> BlockUses.consume(level, pos, state),
                    () -> iceFeedback(level, pos));
        }

        // ⑥ 矿物：按稀有度给 1~3 份（地从矿物取，不从石头取）
        int oreYield = OreYield.of(state);
        if (oreYield > 0) {
            return give(level, player, hand, stack, ElementType.EARTH, oreYield, null,
                    () -> level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.7F, 1.0F));
        }

        // ⑦ 草：草方块变泥土（草会自然蔓延回来 → 可再生）
        if (state.is(Blocks.GRASS_BLOCK)) {
            return give(level, player, hand, stack, ElementType.GRASS, 1,
                    () -> level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState()),
                    () -> level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.6F, 0.9F));
        }
        if (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.SHORT_GRASS) || state.is(BlockTags.FLOWERS)) {
            return give(level, player, hand, stack, ElementType.GRASS, 1,
                    () -> level.removeBlock(pos, false), null);
        }

        // ⑧ 木：原木 / 树苗 / 枯木 / 树叶（作者定）
        //    原木有 5 次储量，采完变去皮木；去皮木不给（否则一棵树能无限刷）
        if (state.is(BlockTags.LOGS)) {
            if (BlockUses.isStripped(state)) {
                return InteractionResultHolder.pass(stack);
            }
            return give(level, player, hand, stack, ElementType.WOOD, 1,
                    () -> BlockUses.consume(level, pos, state),
                    () -> level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 0.9F));
        }
        if (state.is(BlockTags.SAPLINGS) || state.is(BlockTags.LEAVES)
                || state.is(Blocks.DEAD_BUSH)) {
            return give(level, player, hand, stack, ElementType.WOOD, 1,
                    () -> level.removeBlock(pos, false),
                    () -> level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.2F));
        }

        return InteractionResultHolder.pass(stack);
    }

    /** 风：露天 + 高度够，或者雷雨。 */
    private static InteractionResultHolder<ItemStack> gatherWind(Level level, Player player,
                                                                 InteractionHand hand, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        boolean open = level.canSeeSky(pos.above());
        if (!open) {
            return hint(level, player, stack, EVLang.MSG_NO_WIND);
        }
        boolean storm = level.isThundering();
        if (!storm && pos.getY() < EVConfig.getWindMinY()) {
            return hint(level, player, stack, EVLang.MSG_WEAK_WIND);
        }
        return give(level, player, hand, stack, ElementType.WIND, 1, null, null);
    }

    // ---------------------------------------------------------------- 光 / 暗

    private static InteractionResultHolder<ItemStack> gatherLight(Level level, Player player,
                                                                  InteractionHand hand, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        boolean open = level.canSeeSky(pos.above());
        int skyLight = level.getBrightness(LightLayer.SKY, pos);

        if (!open || skyLight < EVConfig.getLightMinSky()) {
            return hint(level, player, stack, EVLang.MSG_NO_LIGHT);
        }
        return give(level, player, hand, stack, ElementType.LIGHT, 1, null, null);
    }

    private static InteractionResultHolder<ItemStack> gatherShadow(Level level, Player player,
                                                                   InteractionHand hand, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        if (level.getMaxLocalRawBrightness(pos) > EVConfig.getShadowMaxBrightness()) {
            return hint(level, player, stack, EVLang.MSG_TOO_BRIGHT);
        }
        return give(level, player, hand, stack, ElementType.SHADOW, 1, null, null);
    }

    // ---------------------------------------------------------------- 公共

    /**
     * 装填（**水瓶模式**）：消耗手里 N 个空容器，得到 N 个装好的容器。
     *
     * @param count      想装几份
     * @param blockEffect 成功后对方块的副作用（降级/变泥土/灭火等），可为 null
     * @param feedback   额外的音效/粒子（冰的破碎效果），可为 null
     */
    private static InteractionResultHolder<ItemStack> give(Level level, Player player, InteractionHand hand,
                                                           ItemStack stack, ElementType type, int count,
                                                           Runnable blockEffect, Runnable feedback) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        // 手里有几个空容器，就最多装几份
        int available = stack.getCount();
        int actual = Math.min(count, available);
        if (actual <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        stack.shrink(actual);

        // 手里那格刚好用完：**先**把第一瓶放回手上，再处理其余。
        // 不能等循环结束再放：其余瓶子会先占掉刚空出来的手牌格，随后被覆盖，少一瓶。
        if (stack.isEmpty()) {
            player.setItemInHand(hand, ElementVialItem.createStack(type));
            actual--;
        }

        for (int i = 0; i < actual; i++) {
            ItemStack filled = ElementVialItem.createStack(type);
            // 其余走"并入同元素堆叠"（时间加权平均，上限 64）
            if (!ElementStackMerger.add(player, filled)) {
                player.drop(filled, false);
            }
        }

        if (blockEffect != null) {
            blockEffect.run();
        }
        if (feedback != null) {
            feedback.run();
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.2F);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /** 冰的破碎音效 + 粒子。 */
    private static void iceFeedback(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.35, 0.35, 0.35, 0.01);
        }
    }

    /** 条件不满足：给动作栏提示（只有这种情况才提示）。 */
    private static InteractionResultHolder<ItemStack> hint(Level level, Player player,
                                                           ItemStack stack, String key) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key), true);
        }
        return InteractionResultHolder.pass(stack);
    }

    /** 自己算射线，避免依赖 {@code Item#getPlayerPOVHitResult}（它是 protected）。 */
    private static BlockHitResult pick(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * REACH, look.y * REACH, look.z * REACH);
        return level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
    }
}
