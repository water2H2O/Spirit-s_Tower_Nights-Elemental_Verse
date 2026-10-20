package com.water2h2o.elementalverse.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 缚手：双手被水压住，抬不起来。
 *
 * <p>中招期间不能攻击、不能使用物品、不能丢弃物品。
 * 对玩家是"手忙脚乱"，对生物就是"拿不出东西还手"（骷髅拉不开弓、僵尸拍不出伤害）。
 *
 * <p>名字取"缚"，和元素之诗里水系的封印意象一致；注册名 {@code bound_hands}。
 */
public class BoundHandsEffect extends MobEffect {

    public BoundHandsEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E6FA8);
    }
}
