package com.water2h2o.elementalverse.heat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 区块余热。
 *
 * <p>原理文档：施法会留下"没被引导的势能"，那就是余热。
 * 这里只做数值，不做可视化 —— 阶段 06 的瘴素会读它。
 *
 * <p>衰减用**时间戳**算，不需要每 tick 遍历所有区块：
 * 每次读或写时，按"距上次更新过了多久"打折。
 */
public class ChunkHeat extends SavedData {

    public static final String DATA_NAME = "elemental_verse_chunk_heat";

    /** 每游戏刻衰减比例（约 20 分钟衰减到 1%）。 */
    private static final double DECAY_PER_TICK = 0.99987D;

    /** 每个块区的余热上限，防止无限堆。 */
    public static final double MAX_HEAT = 1000.0D;

    private final Map<String, Double> heat = new HashMap<>();
    private final Map<String, Long> updatedAt = new HashMap<>();

    public static final SavedData.Factory<ChunkHeat> FACTORY = new SavedData.Factory<>(
            ChunkHeat::new,
            (tag, registries) -> load(tag),
            null);

    // ---------------------------------------------------------------- 读写

    /** 加余热。 */
    public static void add(Level level, BlockPos pos, double amount) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ChunkHeat data = server.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        String key = key(server, pos);
        double current = data.decayed(key, server.getGameTime());
        data.heat.put(key, Math.min(MAX_HEAT, current + amount));
        data.updatedAt.put(key, server.getGameTime());
        data.setDirty();
    }

    /** 读余热（已按时间衰减）。 */
    public static double get(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return 0.0D;
        }
        ChunkHeat data = server.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        return data.decayed(key(server, pos), server.getGameTime());
    }

    /**
     * 余热 key 必须带维度。
     *
     * <p>1.21.1 里所有 ServerLevel 共用主世界的 data storage，只用 chunkPos 会让
     * 主世界和下界同坐标互相串余热。
     */
    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "@" + level.getChunk(pos).getPos().toLong();
    }

    private double decayed(String key, long now) {
        Double value = heat.get(key);
        if (value == null) {
            return 0.0D;
        }
        Long at = updatedAt.get(key);
        long elapsed = at == null ? 0L : Math.max(0L, now - at);
        double result = value * Math.pow(DECAY_PER_TICK, elapsed);
        if (result < 0.01D) {
            heat.remove(key);
            updatedAt.remove(key);
            return 0.0D;
        }
        heat.put(key, result);
        updatedAt.put(key, now);
        return result;
    }

    // ---------------------------------------------------------------- 存档

    private static ChunkHeat load(CompoundTag tag) {
        ChunkHeat data = new ChunkHeat();
        ListTag list = tag.getList("heat", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key;
            if (entry.contains("k", Tag.TAG_STRING)) {
                key = entry.getString("k");
            } else {
                // 旧存档只有 chunkPos。旧数据存在主世界 storage 里，按主世界迁移。
                key = "minecraft:overworld@" + entry.getLong("k");
            }
            data.heat.put(key, entry.getDouble("v"));
            data.updatedAt.put(key, entry.getLong("t"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        heat.forEach((key, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("k", key);
            entry.putDouble("v", value);
            entry.putLong("t", updatedAt.getOrDefault(key, 0L));
            list.add(entry);
        });
        tag.put("heat", list);
        return tag;
    }
}
