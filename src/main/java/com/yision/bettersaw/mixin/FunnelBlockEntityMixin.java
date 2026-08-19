package com.yision.bettersaw.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FunnelBlockEntity.class,remap = false)
public class FunnelBlockEntityMixin {

    @Inject(method = "handleDirectBeltInput",at = @At("HEAD"), cancellable = true)
    private void bettersaw$handlesLogisticBulk(TransportedItemStack stack, Direction side, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("bulkLocked"))
            cir.setReturnValue(stack.stack);
    }
}
