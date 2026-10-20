package com.water2h2o.elementalverse.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.DeclarationPlateItem;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 锻造台升级：把申明**刻到别的物品上**。
 *
 * <p>作者 2026-10-21 定的三形态：
 * <ul>
 *   <li>申明板直接使用 → <b>自身</b></li>
 *   <li>锻造台：板 + 剑 + 粘液球 → <b>接触</b>（砍谁生效在谁身上）</li>
 *   <li>锻造台：板 + 书 + 粘液球 → <b>发射</b>（飞出去命中才生效）</li>
 * </ul>
 *
 * <p>用自定义序列化器而不是原版 {@code smithing_transform}，是因为原版配方
 * **没法把模板上的组件拷到产物上** —— 而申明恰恰存在组件里。
 *
 * <p>注意 {@link #getType()} 必须返回 {@code RecipeType.SMITHING}，
 * 否则原版锻造台在 {@code getRecipeFor(SMITHING, ...)} 时找不到这条配方。
 */
public class DeclarationUpgradeRecipe implements SmithingRecipe {

    public static final MapCodec<DeclarationUpgradeRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
                    Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
                    Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition),
                    Codec.STRING.optionalFieldOf("form", "touch").forGetter(r -> r.form)
            ).apply(instance, DeclarationUpgradeRecipe::new));

    /**
     * 流编解码器：**必须真的编解码**。
     *
     * <p>踩过一次：以为 isSpecial() 就不会同步，用了占位 codec，结果客户端在
     * {@code clientbound/minecraft:update_recipes} 上直接断线。
     * 配方表是会整包同步的，占位 codec 编出来的包解不开。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DeclarationUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
                    Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, r -> r.form,
                    DeclarationUpgradeRecipe::new);

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final String form;

    public DeclarationUpgradeRecipe(Ingredient template, Ingredient base, Ingredient addition,
                                    String form) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.form = form;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return template.test(input.template())
                && base.test(input.base())
                && addition.test(input.addition())
                && DeclarationPlateItem.getDeclaration(input.template()).isDefined();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().copy();
        out.setCount(1);
        Declaration declaration = DeclarationPlateItem.getDeclaration(input.template()).withForm(form);
        out.set(EVDataComponents.DECLARATION.get(), declaration);
        return out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack[] items = base.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EVRecipes.DECLARATION_UPGRADE.get();
    }

    /** 必须是 SMITHING，否则原版锻造台查不到这条配方。 */
    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }
}
