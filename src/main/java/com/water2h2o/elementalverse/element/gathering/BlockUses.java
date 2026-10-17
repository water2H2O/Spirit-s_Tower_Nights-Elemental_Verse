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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * "可被反复采集的方块"的剩余次数。
 *
 * <p>两类方块共用这一份存档数据（键 = 方块种类 + 坐标）：
 *
 * <pre>
 *   冰：蓝冰 ×9 → 浮冰 ×9 → 冰 ×3 → 消失
 *   营火：能采 5 次，之后熄灭（lit=false）
 * </pre>
 *
 * <p>没记录过的位置按该方块的满次数算，所以不需要初始化；
 * 吃满次数后写回"降级后的方块"并清掉记录，下次重新计。
 */
public class BlockUses extends SavedData {

    public static final String DATA_NAME = "elemental_verse_block_uses";

    private static final int BLUE_CAPACITY = 9;
    private static final int PACKED_CAPACITY = 9;
    private static final int ICE_CAPACITY = 3;
    private static final int CAMPFIRE_CAPACITY = 5;

    /** 方块种类：同一套逻辑，不同的容量与"采空后变成什么"。 */
    public enum Kind {
        ICE, CAMPFIRE
    }

    private final Map<String, Integer> remaining = new HashMap<>();

    public static final SavedData.Factory<BlockUses> FACTORY = new SavedData.Factory<>(
            BlockUses::new,
            (tag, registries) -> load(tag, registries),
            null);

    // ---------------------------------------------------------------- 查询

    /** 这个方块的种类；不可采返回 null。 */
    public static Kind kindOf(BlockState state) {
        if (state.is(Blocks.BLUE_ICE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return Kind.ICE;
        }
        if (state.getBlock() instanceof CampfireBlock) {
            return Kind.CAMPFIRE;
        }
        return null;
    }

    /** 这个方块当前还能不能采（熄灭的营火不能再采）。 */
    public static boolean canGather(BlockState state) {
        Kind kind = kindOf(state);
        if (kind == Kind.CAMPFIRE && state.hasProperty(CampfireBlock.LIT) && !state.getValue(CampfireBlock.LIT)) {
            return false;
        }
        return kind != null;
    }

    // ---------------------------------------------------------------- 消耗

    /** 采一次。 */
    public static void consume(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Kind kind = kindOf(state);
        if (kind == null) {
            return;
        }

        BlockUses data = server.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);

        int capacity = capacity(kind, state);
        if (capacity <= 0) {
            return;
        }

        String key = key(kind, pos);
        int left = data.remaining.getOrDefault(key, capacity) - 1;

        if (left > 0) {
            data.remaining.put(key, left);
            data.setDirty();
            return;
        }

        // 采空了：降级 / 熄灭
        data.remaining.remove(key);
        data.setDirty();

        if (kind == Kind.CAMPFIRE) {
            if (state.hasProperty(CampfireBlock.LIT)) {
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            }
            return;
        }

        BlockState next = degradeIce(state);
        if (next == null) {
            level.removeBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, next);
        }
    }

    // ---------------------------------------------------------------- 内部

    private static int capacity(Kind kind, BlockState state) {
        if (kind == Kind.CAMPFIRE) {
            return CAMPFIRE_CAPACITY;
        }
        if (state.is(Blocks.BLUE_ICE)) {
            return BLUE_CAPACITY;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return PACKED_CAPACITY;
        }
        return ICE_CAPACITY;
    }

    private static BlockState degradeIce(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.ICE.defaultBlockState();
        }
        return null;
    }

    private static String key(Kind kind, BlockPos pos) {
        return kind.name() + "@" + pos.asLong();
    }

    private static BlockUses load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockUses data = new BlockUses();
        ListTag list = tag.getList("uses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.remaining.put(entry.getString("key"), entry.getInt("left"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        remaining.forEach((key, left) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", key);
            entry.putInt("left", left);
            list.add(entry);
        });
        tag.put("uses", list);
        return tag;
    }
}
