package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.entity.SpellProjectile;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册。
 *
 * <p>目前只有一种：法术弹射物（{@link SpellProjectile}）。
 * 它是"形"里的发射形态 —— 法术不再贴脸生效，而是**飞出去**再命中。
 */
public final class EVEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "elemental_verse");

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE =
            ENTITIES.register("spell_projectile",
                    () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build("spell_projectile"));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }

    private EVEntities() {
    }
}
