package com.water2h2o.elementalverse.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.water2h2o.elementalverse.block.SealedVaultBlockEntity;
import com.water2h2o.elementalverse.config.EVConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 让**容器里**的元素也会衰变。
 *
 * <p>阶段 01 只扫了玩家背包和掉落物，所以"放进箱子的元素永远不坏" —— 那是个 bug。
 * 这里补上：跟踪已加载的区块，每 {@value #SWEEP_INTERVAL} tick 扫一批。
 *
 * <h2>为什么这么设计</h2>
 * <ul>
 *   <li><b>只扫已加载区块</b>：与"世界停下，东西就停下"的语义一致；
 *       卸载的区块自然不推进，不需要额外判断。</li>
 *   <li><b>预算 + 轮转</b>：不扫全部，每次只处理 {@code decay.chunksPerSweep} 个，
 *       游标一路往前走。大基地也不会卡。</li>
 *   <li><b>不在方块实体里各自 tick</b>：那样"箱子里也会坏"会有 N 份实现，
 *       而且普通箱子我们改不了。</li>
 * </ul>
 *
 * <h2>衰减系数</h2>
 * 普通容器 ×1.0；{@link SealedVaultBlockEntity} 用自己的 ×0.5。
 * 总纲的口径是乘算，所以这里只取容器的系数。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class ContainerDecayTicker {

    /** 多久扫一次（tick）。100 tick = 5 秒。 */
    private static final int SWEEP_INTERVAL = 100;

    /** 每个维度：已加载的区块。 */
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> LOADED = new HashMap<>();

    /** 每个维度的轮转游标。 */
    private static final Map<ResourceKey<Level>, Integer> CURSOR = new HashMap<>();

    // ------------------------------------------------------------ 区块跟踪

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>())
                    .add(event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Set<ChunkPos> chunks = LOADED.get(level.dimension());
            if (chunks != null) {
                chunks.remove(event.getChunk().getPos());
            }
        }
    }

    // ------------------------------------------------------------ 扫描

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!EVConfig.isDecayEnabled()) {
            return;
        }
        if (level.getGameTime() % SWEEP_INTERVAL != 0L) {
            return;
        }
        sweep(level);
    }

    private static void sweep(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        Set<ChunkPos> chunks = LOADED.get(dimension);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<ChunkPos> snapshot = new ArrayList<>(chunks);
        int budget = Math.min(EVConfig.getChunksPerSweep(), snapshot.size());
        int start = Math.floorMod(CURSOR.getOrDefault(dimension, 0), snapshot.size());

        for (int i = 0; i < budget; i++) {
            processChunk(level, snapshot.get((start + i) % snapshot.size()));
        }

        CURSOR.put(dimension, (start + budget) % snapshot.size());
    }

    private static void processChunk(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            // 区块在集合里但实际没加载（刚好卸载）——跳过，下次再说
            return;
        }

        for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            BlockEntity be = chunk.getBlockEntity(blockPos);
            if (be instanceof Container container) {
                tickContainer(level, blockPos, container, be);
            }
        }
    }

    private static void tickContainer(ServerLevel level, BlockPos pos, Container container, BlockEntity be) {
        double multiplier = be instanceof SealedVaultBlockEntity vault ? vault.decayMultiplier() : 1.0D;

        // 符文柜通电时完全不衰变
        if (container instanceof com.water2h2o.elementalverse.block.RuneCabinetBlockEntity cabinet
                && cabinet.isCharged()) {
            return;
        }

        boolean touched = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            // 容器里也可能放着小包 —— 包里的东西按小包自己的规则推进
            if (stack.is(com.water2h2o.elementalverse.registry.EVItems.ELEMENT_POUCH.get())) {
                PouchDecay.tick(stack, level, pos, SWEEP_INTERVAL);
                touched = true;
                continue;
            }

            if (!ElementCarrier.isCarrier(stack)) {
                continue;
            }

            ItemStack replacement = DecayAdvance.advance(stack, level, pos, SWEEP_INTERVAL, multiplier);
            if (replacement != null) {
                container.setItem(slot, replacement);
            }
            touched = true;
        }

        if (touched) {
            // 形态可能只是就地改了组件，这里统一标脏保证存档与客户端同步
            container.setChanged();
        }
    }

    private ContainerDecayTicker() {
    }
}
