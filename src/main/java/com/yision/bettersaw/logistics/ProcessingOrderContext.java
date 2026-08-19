package com.yision.bettersaw.logistics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.yision.bettersaw.content.ProcessingRecipeSelection;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class ProcessingOrderContext {
    private static final int MAGIC = 0x42534157;
    private static final int VERSION = 1;
    private static final int MAX_RECIPE_ID_BYTES = 256;

    private ProcessingOrderContext() {
    }

    public static PackageOrderWithCrafts encodeSelectedRecipe(PackageOrderWithCrafts original,
                                                              List<CraftableBigItemStack> recipesToOrder, Level level) {
        if (recipesToOrder.size() != 1) {
            return original;
        }

        CraftableBigItemStack craftable = recipesToOrder.getFirst();
        Recipe<?> recipe = craftable.recipe;

        ItemStack result = recipe.getResultItem(level.registryAccess());
        int outputCount = result.getCount();
        if (result.isEmpty() || outputCount <= 0 || craftable.count <= 0 || craftable.count % outputCount != 0) {
            return original;
        }

        int craftCount = craftable.count / outputCount;
        if (original.stacks().size() != 1 && (recipe.getType() == AllRecipeTypes.CUTTING.getType() || recipe.getType() == RecipeType.STONECUTTING)) {
            return original;
        }

        BigItemStack orderedInput = original.stacks().getFirst();
        if (
                orderedInput.count != craftCount ||
                orderedInput.stack.isEmpty() ||
                recipe.getIngredients().stream().noneMatch(ingr -> ingr.test(orderedInput.stack))
        ) {
            return original;
        }

        Optional<ResourceLocation> recipeId = ProcessingRecipeSelection.findSupportedRecipeId(level, recipe);
        return recipeId.map(resourceLocation -> attachRecipeId(original, resourceLocation, recipe, original.stacks())).orElse(original);

    }

    public static Optional<ResourceLocation> decodeRecipeId(PackageOrderWithCrafts context) {
        if (context == null) {
            return Optional.empty();
        }

        Optional<List<BigItemStack>> craftingPattern = getCraftingPattern(context);
        if (craftingPattern.isEmpty()) {
            return Optional.empty();
        }
        return decodeRecipeId(craftingPattern.get());
    }

    private static Optional<ResourceLocation> decodeRecipeId(List<BigItemStack> pattern) {
        for (int index = 0; index + 3 <= pattern.size(); index++) {
            if (!isEmptyMetadata(pattern.get(index), MAGIC)
                    || !isEmptyMetadata(pattern.get(index + 1), VERSION)) {
                continue;
            }

            BigItemStack lengthEntry = pattern.get(index + 2);
            if (!lengthEntry.stack.isEmpty()) {
                return Optional.empty();
            }

            int length = lengthEntry.count;
            if (length <= 0 || length > MAX_RECIPE_ID_BYTES || index + 3 + length != pattern.size()) {
                return Optional.empty();
            }

            byte[] bytes = new byte[length];
            for (int byteIndex = 0; byteIndex < length; byteIndex++) {
                BigItemStack encoded = pattern.get(index + 3 + byteIndex);
                if (!encoded.stack.isEmpty() || encoded.count < 1 || encoded.count > 256) {
                    return Optional.empty();
                }
                bytes[byteIndex] = (byte) (encoded.count - 1);
            }

            return Optional.ofNullable(ResourceLocation.tryParse(new String(bytes, StandardCharsets.UTF_8)));
        }

        return Optional.empty();
    }

    public static Optional<ItemStack> getOrderedInput(PackageOrderWithCrafts context) {
        if (context == null) {
            return Optional.empty();
        }

        Optional<List<BigItemStack>> craftingPattern = getCraftingPattern(context);
        if (craftingPattern.isEmpty()) {
            return Optional.empty();
        }

        ItemStack input = ItemStack.EMPTY;
        for (BigItemStack entry : craftingPattern.get()) {
            if (entry.stack.isEmpty()) {
                continue;
            }
            if (!input.isEmpty()) {
                return Optional.empty();
            }
            input = entry.stack;
        }
        return input.isEmpty() ? Optional.empty() : Optional.of(input);
    }

    static PackageOrderWithCrafts attachRecipeId(PackageOrderWithCrafts original, ResourceLocation recipeId, Recipe<?> recipe, List<BigItemStack> stacks) {
        if (!original.orderedCrafts().isEmpty()) {
            if (original.orderedCrafts().getFirst().pattern().stacks().stream().noneMatch(stack -> stack.stack.has(DataComponents.CUSTOM_DATA)))
                return original;
        }

        BigItemStack orderedInput = original.orderedStacks().stacks().getFirst();
        if (recipe instanceof ItemApplicationRecipe iap)
            orderedInput = stacks.stream().filter(bis -> iap.getRequiredHeldItem().test(bis.stack)).toList().getFirst();

        if (orderedInput.stack.isEmpty() || orderedInput.count <= 0)
            return original;

        List<BigItemStack> pattern = new ArrayList<>();
        pattern.add(orderedInput);
        appendRecipeId(pattern, recipeId);
        CraftingEntry craftingEntry = new CraftingEntry(new PackageOrder(pattern), orderedInput.count);
        return new PackageOrderWithCrafts(original.orderedStacks(), List.of(craftingEntry));
    }

    public static void appendRecipeId(List<BigItemStack> pattern, ResourceLocation recipeId) {
        byte[] bytes = recipeId.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_RECIPE_ID_BYTES) {
            throw new IllegalArgumentException("Recipe id is too long to encode: " + recipeId);
        }

        pattern.add(new BigItemStack(ItemStack.EMPTY, MAGIC));
        pattern.add(new BigItemStack(ItemStack.EMPTY, VERSION));
        pattern.add(new BigItemStack(ItemStack.EMPTY, bytes.length));
        for (byte value : bytes) {
            pattern.add(new BigItemStack(ItemStack.EMPTY, Byte.toUnsignedInt(value) + 1));
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isEmptyMetadata(BigItemStack entry, int value) {
        return entry.stack.isEmpty() && entry.count == value;
    }

    private static Optional<List<BigItemStack>> getCraftingPattern(PackageOrderWithCrafts context) {
        if (context.orderedCrafts().size() != 1 || context.orderedCrafts().getFirst().count() <= 0) {
            return Optional.empty();
        }
        return Optional.of(context.orderedCrafts().getFirst().pattern().stacks());
    }
}
