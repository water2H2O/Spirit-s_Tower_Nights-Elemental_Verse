package com.water2h2o.elementalverse.reaction;

import java.util.Optional;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementValue;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 直接释放：拿着写好的申明板右键，把申明变成现象。
 *
 * <h2>流程</h2>
 * <ol>
 *   <li>看板子上写了什么（料 + 档位）</li>
 *   <li>按档位从背包扣对应数量的元素容器 —— 不够就不放</li>
 *   <li>拿"料"的元素值去反应表里匹配（先精确配方，再主成分兜底）</li>
 *   <li>执行现象</li>
 * </ol>
 *
 * <p>作者的要求：**底层复杂，玩家感受简单**。所以扣材料时只说"元素不够"，
 * 不报元素值；命中时只看得到火/水/伤害，看不到任何数字。
 */
public final class SpellCaster {

    private static final double RANGE = 6.0D;

    private SpellCaster() {
    }

    /**
     * @return 真的放出来了返回 true
     */
    public static boolean cast(Level level, Player player, ItemStack plate) {
        Declaration declaration = com.water2h2o.elementalverse.element.DeclarationPlateItem
                .getDeclaration(plate);
        if (!declaration.isDefined()) {
            return false;
        }

        // 1) 材料够不够
        int needed = declaration.investCount();
        if (countVials(player, declaration.element()) < needed) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable(EVLang.MSG_NOT_ENOUGH, declaration.element().displayName()),
                        true);
            }
            return false;
        }

        if (level.isClientSide()) {
            return true;
        }

        // 2) 扣材料
        consumeVials(player, declaration.element(), needed);

        // 3) 匹配反应
        ElementValue input = declaration.value();
        Optional<Reaction> matched = ReactionManager.match(input);
        if (matched.isPresent()) {
            apply(level, player, matched.get().phenomenon());
        } else {
            applyFallback(level, player, input);
        }

        // 4) 表现
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    12, 0.4, 0.6, 0.4, 0.05);
        }
        return true;
    }

    // ---------------------------------------------------------------- 现象

    private static void apply(Level level, Player player, Phenomenon phenomenon) {
        HitResult hit = player.pick(RANGE, 1.0F, false);

        switch (phenomenon.type()) {
            case "ignite" -> ignite(level, hit, phenomenon);
            case "douse" -> douse(level, hit);
            case "sustained_element_damage" -> strike(level, player, phenomenon);
            default -> {
                // 未实现的现象类型：至少给个反馈，别让玩家以为卡住了
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.5F, 1.6F);
            }
        }
    }

    /** 点燃：命中的实体着火，或者命中方块的面上一格起火。 */
    private static void ignite(Level level, HitResult hit, Phenomenon phenomenon) {
        if (hit instanceof EntityHitResult entityHit) {
            entityHit.getEntity().setRemainingFireTicks(phenomenon.duration());
            return;
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos place = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (level.getBlockState(place).isAir()) {
                level.setBlockAndUpdate(place, BaseFireBlock.getState(level, place));
            }
        }
    }

    /** 灭火：把命中点附近的火清掉。 */
    private static void douse(Level level, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            living.clearFire();
            return;
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos center = blockHit.getBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                    level.removeBlock(pos.immutable(), false);
                }
            }
        }
    }

    /**
     * 元素伤害。
     *
     * <p>本期简化为**一次结算**（duration 用来说明"持续感"，还没有逐 tick 结算）。
     * 逐段结算等有了持续型法术框架再补 —— 现在先把闭环跑通。
     */
    private static void strike(Level level, Player player, Phenomenon phenomenon) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof LivingEntity living) {
                living.hurt(level.damageSources().magic(), (float) phenomenon.amount());
                living.setRemainingFireTicks(phenomenon.duration());
            }
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockPos place = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (level.getBlockState(place).isAir()) {
                level.setBlockAndUpdate(place, BaseFireBlock.getState(level, place));
            }
        }
    }

    /** 没有精确配方 → 按主成分律给一个通用现象（保证"扔一堆火"总有反应）。 */
    private static void applyFallback(Level level, Player player, ElementValue input) {
        var axis = input.dominant();
        if (axis == null) {
            return;
        }
        // 通用兜底：正的火 → 点燃；正的水 → 灭火；其余暂时只给粒子提示
        switch (axis) {
            case FIRE -> ignite(level, player.pick(RANGE, 1.0F, false),
                    new Phenomenon("ignite", axis, 1.0, 40, 0.0));
            case WATER -> douse(level, player.pick(RANGE, 1.0F, false));
            default -> level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    // ---------------------------------------------------------------- 材料

    private static int countVials(Player player, ElementType element) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!ElementCarrier.isCarrier(stack)) {
                continue;
            }
            if (ElementVialItem.getElement(stack) == element) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeVials(Player player, ElementType element, int needed) {
        int left = needed;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && left > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!ElementCarrier.isCarrier(stack) || ElementVialItem.getElement(stack) != element) {
                continue;
            }
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        player.getInventory().setChanged();
    }

    /** 给"没瞄准任何东西"留个空实现，避免调用方判空。 */
    public static boolean hasTarget(Player player) {
        return player.pick(RANGE, 1.0F, false).getType() != HitResult.Type.MISS;
    }

}
