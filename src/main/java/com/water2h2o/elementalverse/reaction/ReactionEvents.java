package com.water2h2o.elementalverse.reaction;

import com.mojang.serialization.JsonOps;

import net.minecraft.resources.RegistryOps;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** 服务端启动时加载反应表。 */
@EventBusSubscriber(modid = "elemental_verse")
public final class ReactionEvents {

    private ReactionEvents() {
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();
        RegistryOps<com.google.gson.JsonElement> ops =
                RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        ReactionManager.reload(server.getResourceManager(), ops);
    }
}
