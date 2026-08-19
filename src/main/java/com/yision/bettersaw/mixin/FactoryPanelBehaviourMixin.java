package com.yision.bettersaw.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.yision.bettersaw.logistics.ProcessingOrderContext;
import com.yision.bettersaw.mixin.accessor.ItemStackAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.Map;

@Mixin(value = FactoryPanelBehaviour.class,remap = false)
@Debug(print = true)
public abstract class FactoryPanelBehaviourMixin extends FilteringBehaviour {

    public FactoryPanelBehaviourMixin(SmartBlockEntity be, ValueBoxTransform slot) {super(be, slot);}

    @Shadow
    public abstract FactoryPanelBlockEntity panelBE();

    @Shadow
    public List<ItemStack> activeCraftingArrangement;

    @ModifyArg(
            method = "tickRequests",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    private Object bettersaw$ModifyRequest(Object o) {
        @SuppressWarnings("unchecked")
        var request = (Multimap<PackagerBlockEntity, PackagingRequest>) o; // idc
        var craftingContext = PackageOrderWithCrafts.singleRecipe(activeCraftingArrangement
                .stream().map(stack -> new BigItemStack(stack,stack.getCount())).toList()
        );

        customRecipe:
        if (craftingContext.orderedCrafts().getFirst().pattern().stacks().stream().anyMatch(stack -> stack.stack.has(DataComponents.CUSTOM_DATA))) {
            BigItemStack recipeStack = null;
            for (BigItemStack stack : craftingContext.orderedCrafts().getFirst().pattern().stacks()) {
                if (stack.stack.has(DataComponents.CUSTOM_DATA)) {
                    recipeStack = stack;
                    break;
                }
            }

            if (recipeStack == null)
                break customRecipe;

            var nbt = ((ItemStackAccessor)(Object)(recipeStack.stack)).bettersaw$getRawComponents()
                    .getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("recipeId");
            var recipeId = ResourceLocation.fromNamespaceAndPath(nbt.getString("namespace"),nbt.getString("path"));

            @SuppressWarnings("DataFlowIssue")
            var holder = panelBE().getLevel().getRecipeManager().byKey(recipeId);
            if (holder.isEmpty())
                return request;

            var newRequest = HashMultimap.<PackagerBlockEntity, PackagingRequest>create();
            for (Map.Entry<PackagerBlockEntity, PackagingRequest> entry : request.entries()) {
                var singleRequest = entry.getValue();

                holder.ifPresent(recipeHolder -> {
                    @SuppressWarnings("DataFlowIssue")
                    var result = new CraftableBigItemStack(
                            recipeHolder.value().getResultItem(panelBE().getLevel().registryAccess()),
                            recipeHolder.value()
                    );

                    result.count = result.stack.getCount()*singleRequest.getCount();

                    newRequest.put(
                            entry.getKey(), new PackagingRequest(
                                    singleRequest.item(),
                                    singleRequest.count(),
                                    singleRequest.address(),
                                    singleRequest.linkIndex(),
                                    singleRequest.finalLink(),
                                    singleRequest.packageCounter(),
                                    singleRequest.orderId(),
                                    ProcessingOrderContext.encodeSelectedRecipe(
                                            singleRequest.context() == null ? new PackageOrderWithCrafts(
                                                    new PackageOrder(
                                                            activeCraftingArrangement.stream()
                                                                     .map(stack -> new BigItemStack(stack,stack.getCount())).toList()
                                                    ),
                                                    craftingContext.orderedCrafts()
                                            ) : singleRequest.context(),
                                            List.of(result),
                                            panelBE().getLevel()
                                    )
                            )
                    );
                });
            }
            return newRequest;
        }
        return request;
    }
}
