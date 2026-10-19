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
 *   <li><b>预算 + 轮转 + 时间补偿</b>：不扫全部，每次只处理 {@code decay.chunksPerSweep} 个，
 *       游标一路往前走；但每个区块按“距离上次被扫过了多少 tick”推进，
 *       所以轮转不会让箱子里的东西整体变慢。</li>
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

    /** 每个维度、每个区块上一次被扫描的 gameTime。轮转间隔要用它补偿，否则大世界会整体变慢。 */
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Long>> LAST_PROCESSED = new HashMap<>();

    // ------------------------------------------------------------ 区块跟踪

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ResourceKey<Level> dimension = level.dimension();
            ChunkPos pos = event.getChunk().getPos();
            LOADED.computeIfAbsent(dimension, key -> new LinkedHashSet<>()).add(pos);
            // 卸载期间不推进；刚加载时从现在开始算，避免一次补上几小时的衰变。
            LAST_PROCESSED.computeIfAbsent(dimension, key -> new HashMap<>())
                    .put(pos, level.getGameTime());
        }
    }

    @SubscribeEvent
    static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ResourceKey<Level> dimension = level.dimension();
            ChunkPos pos = event.getChunk().getPos();
            Set<ChunkPos> chunks = LOADED.get(dimension);
            if (chunks != null) {
                chunks.remove(pos);
            }
            Map<ChunkPos, Long> last = LAST_PROCESSED.get(dimension);
            if (last != null) {
                last.remove(pos);
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
        long now = level.getGameTime();
        Map<ChunkPos, Long> last = LAST_PROCESSED.computeIfAbsent(dimension, key -> new HashMap<>());

        for (int i = 0; i < budget; i++) {
            processChunk(level, snapshot.get((start + i) % snapshot.size()), last, now);
        }

        CURSOR.put(dimension, (start + budget) % snapshot.size());
    }

    private static void processChunk(ServerLevel level, ChunkPos pos,
                                     Map<ChunkPos, Long> lastProcessed, long now) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            // 区块在集合里但实际没加载（刚好卸载）——跳过，下次再说
            return;
        }

        // 这个区块可能每 N 次扫描才轮到一次；把真正过去的时间补上，否则大基地整体变慢。
        long last = lastProcessed.getOrDefault(pos, now - SWEEP_INTERVAL);
        int elapsed = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, now - last));
        lastProcessed.put(pos, now);
        if (elapsed <= 0) {
            return;
        }

        for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            BlockEntity be = chunk.getBlockEntity(blockPos);
            if (be instanceof Container container) {
                tickContainer(level, blockPos, container, be, elapsed);
            }
        }
    }

    private static void tickContainer(ServerLevel level, BlockPos pos, Container container, BlockEntity be,
                                      int elapsed) {
        double multiplier = be instanceof SealedVaultBlockEntity vault ? vault.decayMultiplier() : 1.0D;

        // 符文柜通电时完全不衰变
        if (container instanceof com.water2h2o.elementalverse.block.RuneCabinetBlockEntity cabinet
                && cabinet.isCharged()) {
            return;
        }

        boolean touched = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            // 容器里也可能放着小包 —— 包里的东西按小包自己的规则推进；界面打开期间暂停
            if (stack.is(com.water2h2o.elementalverse.registry.EVItems.ELEMENT_POUCH.get())) {
                if (!com.water2h2o.elementalverse.menu.PouchMenu.isOpen(stack)) {
                    PouchDecay.tick(stack, level, pos, elapsed);
                }
                touched = true;
                continue;
            }

            if (!ElementCarrier.isCarrier(stack)) {
                continue;
            }

            // 刚放进去的东西（timer 还是 0）最多只吃一个扫描周期的“补时”，
            // 避免大世界里轮转间隔很长时，新放的元素一进箱子就掉一大截。
            int stackElapsed = ElementVialItem.getTimer(stack) == 0
                    ? Math.min(elapsed, SWEEP_INTERVAL)
                    : elapsed;
            ItemStack replacement = DecayAdvance.advance(stack, level, pos, stackElapsed, multiplier);
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
