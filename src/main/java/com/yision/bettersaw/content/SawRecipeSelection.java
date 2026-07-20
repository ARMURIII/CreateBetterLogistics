package com.yision.bettersaw.content;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.yision.bettersaw.CreateBetterSaw;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

public final class SawRecipeSelection {
    private static RecipeManager indexedRecipeManager;
    private static Map<Recipe<?>, ResourceLocation> recipeIds = Map.of();

    private SawRecipeSelection() {
    }

    public static Optional<ResourceLocation> findSupportedRecipeId(Level level, Recipe<?> recipe) {
        RecipeManager recipeManager = level.getRecipeManager();
        if (recipeManager != indexedRecipeManager) {
            return Optional.empty();
        }

        ResourceLocation recipeId = recipeIds.get(recipe);
        if (recipeId == null) {
            return Optional.empty();
        }

        return recipeManager.byKey(recipeId)
            .filter(holder -> holder.value() == recipe)
            .filter(SawRecipeSelection::isSupported)
            .map(RecipeHolder::id);
    }

    static boolean canProcess(Level level, ResourceLocation recipeId, ItemStack input) {
        return resolve(level, recipeId, input).isPresent();
    }

    static boolean applyOrderedFilter(SawBlockEntity saw, ResourceLocation recipeId, ItemStack input) {
        Level level = saw.getLevel();
        if (level == null) {
            return false;
        }

        Optional<ResolvedRecipe> resolved = resolve(level, recipeId, input);
        FilteringBehaviour filtering = saw.getBehaviour(FilteringBehaviour.TYPE);
        if (resolved.isEmpty() || filtering == null) {
            return false;
        }

        ItemStack orderedFilter = resolved.get().result().copyWithCount(1);
        if (ItemStack.isSameItemSameComponents(filtering.getFilter(), orderedFilter)) {
            return true;
        }
        return filtering.setFilter(orderedFilter);
    }

    private static Optional<ResolvedRecipe> resolve(Level level, ResourceLocation recipeId, ItemStack input) {
        Optional<? extends RecipeHolder<?>> holder = level.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty() || !isSupported(holder.get())) {
            return Optional.empty();
        }

        Recipe<?> recipe = holder.get().value();
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.size() != 1 || !ingredients.getFirst().test(input)) {
            return Optional.empty();
        }

        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedRecipe(result));
    }

    private static boolean isSupported(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
            return false;
        }
        if (recipe instanceof CuttingRecipe) {
            return recipe.getType() == AllRecipeTypes.CUTTING.getType();
        }
        return recipe instanceof StonecutterRecipe
            && AllConfigs.server().recipes.allowStonecuttingOnSaw.get();
    }

    private static void refreshRecipeIds(RecipeManager recipeManager) {
        IdentityHashMap<Recipe<?>, ResourceLocation> refreshed = new IdentityHashMap<>();
        for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(AllRecipeTypes.CUTTING.getType())) {
            refreshed.put(holder.value(), holder.id());
        }
        for (RecipeHolder<?> holder : recipeManager.getAllRecipesFor(RecipeType.STONECUTTING)) {
            refreshed.put(holder.value(), holder.id());
        }
        recipeIds = refreshed;
        indexedRecipeManager = recipeManager;
    }

    @EventBusSubscriber(modid = CreateBetterSaw.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        static void onRecipesUpdated(RecipesUpdatedEvent event) {
            refreshRecipeIds(event.getRecipeManager());
        }
    }

    private record ResolvedRecipe(ItemStack result) {
    }
}
