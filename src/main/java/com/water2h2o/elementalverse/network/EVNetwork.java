package com.water2h2o.elementalverse.network;

import com.water2h2o.elementalverse.menu.RitualAltarMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 网络：把仪式台命名框里的文字发给服务端。 */
@EventBusSubscriber(modid = "elemental_verse", bus = EventBusSubscriber.Bus.MOD)
public final class EVNetwork {

    private EVNetwork() {
    }

    /** 客户端的命名框 → 服务端的仪式台。 */
    public record SetAltarName(String name) implements CustomPacketPayload {

        public static final Type<SetAltarName> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("elemental_verse", "set_altar_name"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SetAltarName> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetAltarName::name, SetAltarName::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SetAltarName.TYPE, SetAltarName.STREAM_CODEC,
                EVNetwork::handleName);
    }

    private static void handleName(SetAltarName payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof RitualAltarMenu menu) {
                menu.setCustomName(payload.name());
            }
        });
    }
}
