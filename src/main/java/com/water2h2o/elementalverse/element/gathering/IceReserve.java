package com.water2h2o.elementalverse.element.gathering;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 冰方块的"储量"。
 *
 * <p>作者 2026-10-17：采集会改变方块，越采越薄 ——
 *
 * <pre>
 *   蓝冰 ×9 → 浮冰 ×9 → 冰 ×3 → 消失
 * </pre>
 *
 * <p>所以每个冰方块位置都要记"还剩几次"。用存档数据（{@link SavedData}）按
 * **维度 + 坐标**记录；没记过的位置按对应方块的满储量算，因此不需要初始化。
 *
 * <p>只在服务端生效：客户端拿到 level 也取不到数据，调用方（Gathering）本来也只在服务端跑。
 */
public class IceReserve extends SavedData {

    public static final String DATA_NAME = "elemental_verse_ice_reserve";

    private static final int BLUE_CAPACITY = 9;
    private static final int PACKED_CAPACITY = 9;
    private static final int ICE_CAPACITY = 3;

    /** 键 = BlockPos.asLong()，值 = 剩余可采次数。 */
    private final Map<Long, Integer> remaining = new HashMap<>();

    public static final SavedData.Factory<IceReserve> FACTORY = new SavedData.Factory<>(
            IceReserve::new,
            (tag, registries) -> load(tag, registries),
            null);

    // ---------------------------------------------------------------- 对外

    /** 这个方块是不是冰族；是的话返回 1（表示可采），否则 0。 */
    public static int yieldFor(BlockState state) {
        return capacity(state) > 0 ? 1 : 0;
    }

    /** 采一次：扣一次储量，采空就降级（蓝冰→浮冰→冰→消失）。 */
    public static void consume(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        IceReserve data = server.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);

        int capacity = capacity(state);
        if (capacity <= 0) {
            return;
        }

        Long key = pos.asLong();
        int left = data.remaining.getOrDefault(key, capacity) - 1;

        if (left > 0) {
            data.remaining.put(key, left);
            data.setDirty();
            return;
        }

        // 采空 → 降级
        data.remaining.remove(key);
        data.setDirty();

        BlockState next = degrade(state);
        if (next == null) {
            level.removeBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, next);
        }
    }

    // ---------------------------------------------------------------- 内部

    private static int capacity(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return BLUE_CAPACITY;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return PACKED_CAPACITY;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return ICE_CAPACITY;
        }
        return 0;
    }

    /** 采空之后变成什么；null 表示消失。 */
    private static BlockState degrade(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.ICE.defaultBlockState();
        }
        return null;
    }

    private static IceReserve load(CompoundTag tag, HolderLookup.Provider registries) {
        IceReserve data = new IceReserve();
        ListTag list = tag.getList("reserve", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.remaining.put(entry.getLong("pos"), entry.getInt("left"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        remaining.forEach((pos, left) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos);
            entry.putInt("left", left);
            list.add(entry);
        });
        tag.put("reserve", list);
        return tag;
    }
}
