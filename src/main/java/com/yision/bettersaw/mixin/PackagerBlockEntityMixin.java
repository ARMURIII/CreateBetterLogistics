package com.yision.bettersaw.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.yision.bettersaw.content.DeployingUnpackingHandler;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PackagerBlockEntity.class,remap = false)
public class PackagerBlockEntityMixin {
    @Shadow
    public ItemStack heldBox;

    @WrapOperation(method = "getRenderedBox",at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemStack;EMPTY:Lnet/minecraft/world/item/ItemStack;", opcode = Opcodes.GETSTATIC))
    private ItemStack bettersaw$rejectedBoxKeeps(Operation<ItemStack> original) {
        var data = this.heldBox.get(AllDataComponents.PACKAGE_ORDER_DATA);
        if (data != null && DeployingUnpackingHandler.isRejectedOrder(data))
            return this.heldBox;
        return original.call();
    }
}
