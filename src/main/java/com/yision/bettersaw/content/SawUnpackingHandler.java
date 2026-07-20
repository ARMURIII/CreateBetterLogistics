package com.yision.bettersaw.content;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.yision.bettersaw.logistics.SawOrderContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public enum SawUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    public static void register() {
        UnpackingHandler.REGISTRY.register(AllBlocks.MECHANICAL_SAW.get(), INSTANCE);
    }

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
            @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof SawBlockEntity saw)
                || state.getValue(SawBlock.FACING) != Direction.UP
                || side == Direction.DOWN
                || !saw.inventory.isEmpty()
                || SawBufferManager.hasBufferedInput(saw)) {
            return false;
        }

        ItemStack input = combineInput(items);
        if (input.isEmpty()) {
            return false;
        }

        Optional<ResourceLocation> orderedRecipeId = SawOrderContext.decodeRecipeId(orderContext);
        if (orderedRecipeId.isPresent()) {
            if (!SawRecipeSelection.canProcess(level, orderedRecipeId.get(), input)
                    || !SawOrderContext.getOrderedInput(orderContext)
                        .filter(patternInput -> ItemStack.isSameItemSameComponents(patternInput, input))
                        .isPresent()) {
                return false;
            }
        } else if (orderContext != null && !orderContext.orderedCrafts().isEmpty()) {
            return false;
        }

        if (simulate) {
            return true;
        }

        SawInputBatch batch = SawInputBatch.from(input);
        saw.inventory.setStackInSlot(0, batch.active());
        if (orderedRecipeId.isPresent()
                && !SawRecipeSelection.applyOrderedFilter(saw, orderedRecipeId.get(), batch.active())) {
            saw.inventory.setStackInSlot(0, ItemStack.EMPTY);
            saw.setChanged();
            return false;
        }

        SawBufferManager.storeBufferedInput(saw, batch.buffered(), orderedRecipeId.orElse(null));
        saw.start(batch.active());
        saw.setChanged();
        return true;
    }

    private static ItemStack combineInput(List<ItemStack> items) {
        ItemStack combined = ItemStack.EMPTY;
        int total = 0;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!combined.isEmpty() && !ItemStack.isSameItemSameComponents(combined, stack)) {
                return ItemStack.EMPTY;
            }

            total += stack.getCount();
            if (total > SawBuffer.CAPACITY || total > stack.getMaxStackSize()) {
                return ItemStack.EMPTY;
            }
            if (combined.isEmpty()) {
                combined = stack.copyWithCount(total);
            } else {
                combined.setCount(total);
            }
        }

        return combined;
    }
}
