package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.block.SealedVaultBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块实体注册。 */
public final class EVBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "elemental_verse");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealedVaultBlockEntity>> SEALED_VAULT =
            BLOCK_ENTITIES.register("sealed_vault",
                    () -> BlockEntityType.Builder
                            .of(SealedVaultBlockEntity::new, EVBlocks.SEALED_VAULT.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.water2h2o.elementalverse.block.RitualAltarBlockEntity>> RITUAL_ALTAR =
            BLOCK_ENTITIES.register("ritual_altar",
                    () -> BlockEntityType.Builder
                            .of(com.water2h2o.elementalverse.block.RitualAltarBlockEntity::new,
                                    EVBlocks.RITUAL_ALTAR.get())
                            .build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private EVBlockEntities() {
    }
}
