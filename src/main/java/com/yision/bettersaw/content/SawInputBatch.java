package com.yision.bettersaw.content;

import net.minecraft.world.item.ItemStack;

record SawInputBatch(ItemStack active, ItemStack buffered) {
    static SawInputBatch from(ItemStack input) {
        if (input.isEmpty()) {
            return new SawInputBatch(ItemStack.EMPTY, ItemStack.EMPTY);
        }

        ItemStack active = input.copyWithCount(1);
        ItemStack buffered = input.copy();
        buffered.shrink(1);
        return new SawInputBatch(active, buffered);
    }
}
