package com.yision.bettersaw.content;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

public final class SawBufferItemHandler implements IItemHandler {
    @Nullable
    private final IItemHandler processingInventory;
    private final Supplier<ItemStack> bufferedStack;
    private final BufferExtractor bufferExtractor;

    SawBufferItemHandler(
            @Nullable IItemHandler processingInventory,
            Supplier<ItemStack> bufferedStack,
            BufferExtractor bufferExtractor) {
        this.processingInventory = processingInventory;
        this.bufferedStack = bufferedStack;
        this.bufferExtractor = bufferExtractor;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            AllBlockEntityTypes.SAW.get(),
            SawBufferItemHandler::forSaw
        );
    }

    static SawBufferItemHandler forSaw(SawBlockEntity saw, @Nullable Direction side) {
        return forSide(
            saw.inventory,
            side,
            () -> SawBufferManager.copyBufferedInput(saw),
            (amount, simulate) -> SawBufferManager.extractBufferedInput(saw, amount, simulate)
        );
    }

    static SawBufferItemHandler forSide(
            IItemHandler processingInventory,
            @Nullable Direction side,
            Supplier<ItemStack> bufferedStack,
            BufferExtractor bufferExtractor) {
        return new SawBufferItemHandler(
            side == Direction.DOWN ? null : processingInventory,
            bufferedStack,
            bufferExtractor
        );
    }

    @Override
    public int getSlots() {
        return processingSlots() + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        if (slot == bufferSlot()) {
            return bufferedStack.get();
        }
        return processingInventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (slot == bufferSlot()) {
            return stack;
        }
        return processingInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        if (slot == bufferSlot()) {
            return bufferExtractor.extract(amount, simulate);
        }
        return processingInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlot(slot);
        if (slot == bufferSlot()) {
            return SawBuffer.CAPACITY;
        }
        return processingInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlot(slot);
        if (slot == bufferSlot()) {
            return false;
        }
        return processingInventory.isItemValid(slot, stack);
    }

    private int processingSlots() {
        return processingInventory == null ? 0 : processingInventory.getSlots();
    }

    private int bufferSlot() {
        return processingSlots();
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range [0," + getSlots() + ")");
        }
    }

    @FunctionalInterface
    interface BufferExtractor {
        ItemStack extract(int amount, boolean simulate);
    }
}
