package com.yision.bettersaw.content;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ProcessingBuffer {
    public static final int CAPACITY = 64;

    public static final Codec<ProcessingBuffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.fieldOf("remaining").forGetter(ProcessingBuffer::copyRemaining),
        ResourceLocation.CODEC.optionalFieldOf("recipe_id").forGetter(ProcessingBuffer::recipeId)
    ).apply(instance, (remaining, recipeId) -> new ProcessingBuffer(remaining, recipeId.orElse(null))));

    private ItemStack remaining;
    @Nullable
    private ResourceLocation recipeId;

    public ProcessingBuffer() {
        this(ItemStack.EMPTY, null);
    }

    private ProcessingBuffer(ItemStack remaining, @Nullable ResourceLocation recipeId) {
        this.remaining = remaining.copy();
        this.recipeId = recipeId;
    }

    public boolean isEmpty() {
        return remaining.isEmpty();
    }

    public boolean hasState() {
        return !remaining.isEmpty() || recipeId != null;
    }

    public void replace(ItemStack stack) {
        replace(stack, null);
    }

    public void replace(ItemStack stack, @Nullable ResourceLocation recipeId) {
        remaining = stack.copy();
        this.recipeId = recipeId;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ItemStack takeOne() {
        return extract(1, false);
    }

    ItemStack extract(int amount, boolean simulate) {
        if (remaining.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = remaining.copy();
        extracted.setCount(Math.min(amount, remaining.getCount()));
        if (!simulate) {
            remaining.shrink(extracted.getCount());
        }
        return extracted;
    }

    public ItemStack copyRemaining() {
        return remaining.copy();
    }

    public Optional<ResourceLocation> recipeId() {
        return Optional.ofNullable(recipeId);
    }
}
