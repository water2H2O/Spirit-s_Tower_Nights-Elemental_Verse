package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.recipe.DeclarationUpgradeRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 配方序列化器注册。 */
public final class EVRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "elemental_verse");

    /** 锻造台：把申明刻到剑 / 书上。 */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DeclarationUpgradeRecipe>>
            DECLARATION_UPGRADE = SERIALIZERS.register("declaration_upgrade",
                    () -> new RecipeSerializer<DeclarationUpgradeRecipe>() {
                        @Override
                        public com.mojang.serialization.MapCodec<DeclarationUpgradeRecipe> codec() {
                            return DeclarationUpgradeRecipe.CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<
                                net.minecraft.network.RegistryFriendlyByteBuf,
                                DeclarationUpgradeRecipe> streamCodec() {
                            return DeclarationUpgradeRecipe.STREAM_CODEC;
                        }
                    });

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private EVRecipes() {
    }
}
