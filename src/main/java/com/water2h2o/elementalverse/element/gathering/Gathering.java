package com.water2h2o.elementalverse.element.gathering;

import java.util.Map;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * <p>规则见 `docs/分阶段/03-采集系统.md`。要点：
 * <ul>
 *   <li>手持**空**容器右键目标 → 装进去；容器本身不消耗</li>
 *   <li>容器里已有东西 → **静默**（作者定：不刷屏）</li>
 *   <li>没有冷却、没有耐久、没有概率</li>
 *   <li>只有"条件不满足"才给提示（太暗/太亮/风进不来/风太弱）</li>
 * </ul>
 *
 * <h2>为什么集中在一个类里</h2>
 * 七种元素的判定条件差别很大，但它们共享同一套"先取目标、再判定、再装填"的流程。
 * 拆成七个类反而让公共部分散开；这里按元素分私有方法，公共流程只有一份。
 */
public final class Gathering {

    /** 伸手距离。原版是 4.5 格，这里直接写死，避免依赖版本差异的 API。 */
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
            // 没打到方块 → 试试"对空气采风"
            return gatherWind(level, player, hand, stack);
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // ① 岩浆：扣血、一次 5 份（作者定：不消耗容器，改成扣血）
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return give(level, player, hand, stack, ElementType.FIRE, LAVA_YIELD, () -> {
                player.hurt(level.damageSources().lava(), LAVA_DAMAGE);
                player.setRemainingFireTicks(60);
            });
        }

        // ② 水：一次 1 份
        if (state.getFluidState().is(FluidTags.WATER)) {
            return give(level, player, hand, stack, ElementType.WATER, 1, null);
        }

        // ③ 营火：温度不够，只能得烬（作者定）
        if (state.getBlock() instanceof CampfireBlock) {
            return give(level, player, hand, stack, ElementType.EMBER, 1, null);
        }

        // ④ 火焰方块：不扣血，1 份，火被扑灭
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return give(level, player, hand, stack, ElementType.FIRE, 1,
                    () -> level.removeBlock(pos, false));
        }

        // ⑤ 冰类：1 份，并且方块越来越薄（蓝冰→浮冰→冰→消失）
        int iceYield = IceReserve.yieldFor(state);
        if (iceYield > 0) {
            return give(level, player, hand, stack, ElementType.ICE, 1,
                    () -> IceReserve.consume(level, pos, state));
        }

        // ⑥ 矿物：按稀有度给 1~3 份（作者定：地从矿物取，不从石头取）
        int oreYield = OreYield.of(state);
        if (oreYield > 0) {
            return give(level, player, hand, stack, ElementType.EARTH, oreYield, null);
        }

        // ⑦ 草：草方块变泥土（草会自然蔓延回来 → 可再生）
        if (state.is(Blocks.GRASS_BLOCK)) {
            return give(level, player, hand, stack, ElementType.GRASS, 1,
                    () -> level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState()));
        }
        if (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.SHORT_GRASS) || state.is(BlockTags.FLOWERS)) {
            return give(level, player, hand, stack, ElementType.GRASS, 1,
                    () -> level.removeBlock(pos, false));
        }

        // 打到了方块但什么都不产出 → 静默
        return InteractionResultHolder.pass(stack);
    }

    /** 风：露天 + 高度够，或者雷雨。 */
    private static InteractionResultHolder<ItemStack> gatherWind(Level level, Player player,
                                                                 InteractionHand hand, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        if (!level.canSeeSky(pos.above())) {
            return hint(level, player, stack, EVLang.MSG_NO_WIND);
        }
        boolean storm = level.isThundering() && level.canSeeSky(pos.above());
        if (!storm && pos.getY() < EVConfig.getWindMinY()) {
            return hint(level, player, stack, EVLang.MSG_WEAK_WIND);
        }
        return give(level, player, hand, stack, ElementType.WIND, 1, null);
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
        return give(level, player, hand, stack, ElementType.LIGHT, 1, null);
    }

    private static InteractionResultHolder<ItemStack> gatherShadow(Level level, Player player,
                                                                   InteractionHand hand, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        if (level.getMaxLocalRawBrightness(pos) > EVConfig.getShadowMaxBrightness()) {
            return hint(level, player, stack, EVLang.MSG_TOO_BRIGHT);
        }
        return give(level, player, hand, stack, ElementType.SHADOW, 1, null);
    }

    // ---------------------------------------------------------------- 公共

    /** 装填：成功则在服务端写数据组件、播音效；客户端只回成功。 */
    private static InteractionResultHolder<ItemStack> give(Level level, Player player, InteractionHand hand,
                                                           ItemStack stack, ElementType type, int count,
                                                           Runnable sideEffect) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ElementVialItem.setElement(stack, type, 0);
        if (count > 1) {
            // 多份的情况：直接把数量记在瓶子的"份数"上暂时不支持，改成给多个容器太啰嗦，
            // 这里用同一格堆叠多次装填解决不了，所以走"补满到 count"的语义
            gatherExtra(player, type, count - 1);
        }

        if (sideEffect != null) {
            sideEffect.run();
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.2F);
        return InteractionResultHolder.success(stack);
    }

    /** 岩浆那种"一次给 5 份"：另外 4 个直接塞进背包。 */
    private static void gatherExtra(Player player, ElementType type, int extra) {
        for (int i = 0; i < extra; i++) {
            ItemStack bonus = ElementVialItem.createStack(type);
            if (!player.getInventory().add(bonus)) {
                player.drop(bonus, false);
            }
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
