package com.water2h2o.elementalverse.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 物品栏封锁：洪流的副产物。
 *
 * <p>中招期间不能使用物品、不能丢弃物品。对玩家而言这是"手忙脚乱"；
 * 对生物而言它也拿不出东西来还手（掠夺者用不上弩）。
 */
public class InventoryLockEffect extends MobEffect {

    public InventoryLockEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E6FA8);
    }
}
