package com.yision.bettersaw.content;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

@SuppressWarnings("ALL")
public class FakeCraftingRecipe implements CraftingRecipe {

    public final Recipe<?> recipe;
    public final RecipeHolder<?> holder;

    public FakeCraftingRecipe(Recipe<?> recipe, RecipeHolder<?> holder) {
        this.recipe = recipe;
        this.holder = holder;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return recipe.canCraftInDimensions(i,i1);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return recipe.getResultItem(provider);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return recipe.getSerializer();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipe.getIngredients();
    }
}
