package com.yision.bettersaw.mixin;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.yision.bettersaw.content.DeployingUnpackingHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FrogportBlockEntity.class,remap = false)
public class FrogportBlockEntityMixin {
    @Inject(method = "tryPullingFrom",at = @At("HEAD"), cancellable = true)
    private void bettersaw$doNotPullRejected(IItemHandler handler, CallbackInfoReturnable<Boolean> cir) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            var stack = handler.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            var data = stack.get(AllDataComponents.PACKAGE_ORDER_DATA);
            if (data != null && DeployingUnpackingHandler.isRejectedOrder(data))
                cir.setReturnValue(false);
            break;
        }
    }
}
