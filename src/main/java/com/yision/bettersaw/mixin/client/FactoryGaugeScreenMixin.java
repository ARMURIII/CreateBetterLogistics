package com.yision.bettersaw.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.yision.bettersaw.content.FakeCraftingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(value = FactoryPanelScreen.class,remap = false)
public class FactoryGaugeScreenMixin {
    @Shadow
    private List<BigItemStack> inputConfig;

    @Shadow
    private BigItemStack outputConfig;

    @Shadow
    private CraftingRecipe availableCraftingRecipe;
    @Unique
    private BigItemStack bettersaw$currentOrderedInput = null;

    @WrapOperation(method = "sendIt",at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private List<?> bettersaw$coolThing(Stream<ItemStack> instance, Operation<List<ItemStack>> original) {
        if (availableCraftingRecipe instanceof FakeCraftingRecipe fcr) {
            List<ItemStack> pattern = new ArrayList<>();
            var input = bettersaw$currentOrderedInput.stack.copyWithCount(1);

            var compound = new CompoundTag();
            var recipeId = new CompoundTag();

            recipeId.putString("path",fcr.holder.id().getPath());
            recipeId.putString("namespace",fcr.holder.id().getNamespace());

            compound.put("recipeId",recipeId);
            input.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));

            pattern.add(input);
            return pattern;
        }
        return original.call(instance);
    }

    @ModifyConstant(method = "init",constant = @Constant(stringValue = "gui.factory_panel.activate_crafting"))
    private String bettersaw$changeTooltip(String constant) {
        if (availableCraftingRecipe instanceof FakeCraftingRecipe)
            return "gui.factory_panel.activate_processing";

        return constant;
    }

    @Inject(method = "searchForCraftingRecipe",at = @At("TAIL"))
    private void bettersaw$searchForOthers(CallbackInfo ci) {

        bettersaw$currentOrderedInput = null;

        var input = new ArrayList<>(inputConfig);
        input.removeIf(stack -> stack.stack.isEmpty());
        input.removeIf(stack -> stack.count == 0);

        BigItemStack output = outputConfig;

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null)
            return;

        if (input.size() == 1) {

            ArrayList<RecipeHolder<?>> allRecipes = new ArrayList<>(level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.CUTTING.getType()));

            allRecipes.addAll(level.getRecipeManager()
                    .getAllRecipesFor(RecipeType.STONECUTTING));

            allRecipes
                    .parallelStream()
                    .filter(r -> output.stack.getItem() == r.value().getResultItem(level.registryAccess())
                            .getItem())
                    .filter(r -> input.getFirst().stack.getItem() == r.value().getIngredients().getFirst().getItems()[0]
                            .getItem())
                    .findAny()
                    .ifPresent(cutting ->
                        availableCraftingRecipe = new FakeCraftingRecipe(cutting.value(), cutting)
                    );

            bettersaw$currentOrderedInput = input.getFirst();
        }

        if (input.size() == 2) {
            var allRecipes = new ArrayList<>(level.getRecipeManager()
                    .getAllRecipesFor(AllRecipeTypes.DEPLOYING.getType()));
            allRecipes.addAll(level.getRecipeManager()
                    .getAllRecipesFor(AllRecipeTypes.ITEM_APPLICATION.getType()));

                allRecipes
                    .parallelStream()
                    .filter(r -> output.stack.getItem() == r.value().getResultItem(level.registryAccess())
                            .getItem())
                    .filter(r -> {
                                if (((Recipe<?>) r.value()) instanceof ItemApplicationRecipe iar) {
                                    if (input.stream().anyMatch(stack -> iar.getRequiredHeldItem().test(stack.stack)))
                                        return input.stream().anyMatch(stack -> iar.getProcessedItem().test(stack.stack));
                                }
                                return false;
                            }
                    )
                    .findAny()
                    .ifPresent(deploying ->
                        availableCraftingRecipe = new FakeCraftingRecipe(deploying.value(), deploying)
                    );

            if (availableCraftingRecipe instanceof FakeCraftingRecipe fcr && fcr.recipe instanceof ItemApplicationRecipe iap)
                bettersaw$currentOrderedInput =
                        input.stream()
                                .filter(stack -> iap.getRequiredHeldItem().test(stack.stack))
                                .findAny()
                                .orElse(input.getFirst());

        }

        if (bettersaw$currentOrderedInput != null)
            bettersaw$currentOrderedInput.count = 1;
    }
}
