package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.block.SealedVaultBlock;

import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册。
 *
 * <p>阶段 01 只有一个方块：死寂物质。它是元素走到尽头的产物，也是一块好用的建材。
 */
public final class EVBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks("elemental_verse");

    /**
     * 死寂物质。
     *
     * <p>注意：**刻意不加 {@code requiresCorrectToolForDrops()}** ——
     * 那需要同时提供 `mineable` 方块标签，否则徒手挖什么都不掉。
     * 阶段 01 先保证「一定能挖到」，工具分级留到后面和配方一起做。
     */
    public static final DeferredBlock<Block> DEAD_MATTER = BLOCKS.register(
            "dead_matter",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.NORMAL)));

    /**
     * 石制密封仓：9 格，仓内元素衰变 ×0.5。
     *
     * <p>同样**不加** {@code requiresCorrectToolForDrops()}，先保证徒手能挖下来。
     */
    public static final DeferredBlock<Block> SEALED_VAULT = BLOCKS.register(
            "sealed_vault",
            () -> new SealedVaultBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(2.5F, 8.0F)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.NORMAL)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private EVBlocks() {
    }
}
