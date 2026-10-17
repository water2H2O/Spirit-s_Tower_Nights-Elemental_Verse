package com.water2h2o.elementalverse.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义音效。
 *
 * <p>音频文件放在 {@code assets/elemental_verse/sounds/}，映射写在
 * {@code assets/elemental_verse/sounds.json}。**Minecraft 只认 Ogg Vorbis，
 * 并且位置音必须是单声道**（立体声不会被空间衰减，听上去像贴脸播放）。
 *
 * <p>作者给的第一份素材是 Ogg **FLAC**，游戏解不了，已用 PyAV 转成单声道 Vorbis。
 */
public final class EVSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, "elemental_verse");

    /** 音效 id。**不带点**：带点的会被静态自查当成语言键扫出来。 */
    private static final String VAULT_OPEN_ID = "vault_open";

    /** 石制密封仓开启：石盖被拖开的摩擦声（作者提供）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_OPEN =
            SOUNDS.register(VAULT_OPEN_ID,
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath("elemental_verse", VAULT_OPEN_ID)));

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }

    private EVSounds() {
    }
}
