package com.water2h2o.elementalverse.reaction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * 反应表加载。
 *
 * <p>挂在资源重载链上，所以服务端启动和 {@code /reload} 都会重新读
 * {@code data/<namespace>/reaction/*.json}，改完 JSON 不用重启。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class ReactionEvents {

    private ReactionEvents() {
    }

    @SubscribeEvent
    static void onAddReloadListener(AddReloadListenerEvent event) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, event.getRegistryAccess());
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                                  ResourceManager resources,
                                                  ProfilerFiller preparationsProfiler,
                                                  ProfilerFiller reloadProfiler,
                                                  Executor backgroundExecutor,
                                                  Executor gameExecutor) {
                return CompletableFuture
                        .runAsync(() -> ReactionManager.reload(resources, ops), backgroundExecutor)
                        .thenCompose(barrier::wait);
            }
        });
    }

    /**
     * 兜底：如果某种加载顺序导致重载监听没有跑到，服务端启动时再补一次。
     * 已经加载过就不重复读。
     */
    @SubscribeEvent
    static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!ReactionManager.all().isEmpty()) {
            return;
        }
        var server = event.getServer();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        ReactionManager.reload(server.getResourceManager(), ops);
    }
}
