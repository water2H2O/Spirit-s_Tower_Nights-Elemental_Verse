package com.water2h2o.elementalverse.reaction;

import java.util.ArrayList;
import java.util.List;

import com.water2h2o.elementalverse.element.Declaration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 延时施法队列（红石粉修饰）。
 *
 * <p>只存服务端；玩家下线时对应条目自然失效（执行前会检查是否还在线）。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class PendingCasts {

    private record Pending(ServerLevel level, Player player, Declaration declaration, long dueTick) {
    }

    private record Task(long dueTick, Runnable action) {
    }

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    private PendingCasts() {
    }

    public static void schedule(Level level, Player player, Declaration declaration, int delayTicks) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        QUEUE.add(new Pending(server, player, declaration, now + delayTicks));
    }

    /** 通用延时任务（回响重放、火药引信、龙息脉冲都用它）。 */
    public static void runLater(ServerLevel level, int delayTicks, Runnable action) {
        TASKS.add(new Task(now + delayTicks, action));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        now++;
        if (!TASKS.isEmpty()) {
            List<Task> ready = new ArrayList<>();
            TASKS.removeIf(task -> {
                if (task.dueTick() <= now) {
                    ready.add(task);
                    return true;
                }
                return false;
            });
            ready.forEach(task -> task.action().run());
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        List<Pending> due = new ArrayList<>();
        QUEUE.removeIf(entry -> {
            if (entry.dueTick() <= now) {
                due.add(entry);
                return true;
            }
            return false;
        });
        for (Pending entry : due) {
            if (entry.player().isRemoved()) {
                continue;
            }
            SpellCaster.executeNow(entry.level(), entry.player(), entry.declaration());
        }
    }
}
