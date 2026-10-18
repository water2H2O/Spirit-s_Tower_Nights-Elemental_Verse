package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.effect.VulnerableEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 自创状态注册。 */
public final class EVMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "elemental_verse");

    public static final DeferredHolder<MobEffect, MobEffect> VULNERABLE =
            EFFECTS.register("vulnerable", VulnerableEffect::new);

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    /** 易伤的受伤倍率：1 + 0.15 x (等级+1)，最高 3 级。 */
    public static double damageMultiplier(int amplifier) {
        return 1.0D + VulnerableEffect.PER_LEVEL * Math.min(3, amplifier + 1);
    }

    private EVMobEffects() {
    }
}
