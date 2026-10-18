package com.water2h2o.elementalverse.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 易伤：受到的所有伤害提高。
 *
 * <p>这是模组第一个自创状态。倍率写在 {@link com.water2h2o.elementalverse.registry.EVMobEffects#damageMultiplier}：
 * 每级 +15%，最高 3 级。
 */
public class VulnerableEffect extends MobEffect {

    public static final double PER_LEVEL = 0.15D;

    public VulnerableEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B1A1A);
    }
}
