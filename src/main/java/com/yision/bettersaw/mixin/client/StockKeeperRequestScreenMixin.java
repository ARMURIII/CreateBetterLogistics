package com.yision.bettersaw.mixin.client;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.yision.bettersaw.logistics.ProcessingOrderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

    @ModifyVariable(
            method = "sendIt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
                    shift = At.Shift.BEFORE,
                    remap = false
            ),
            name = "order"
    )
    private PackageOrderWithCrafts bettersaw$DeployerAPICompat(PackageOrderWithCrafts order) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return order;
        }

        StockKeeperRequestScreen screen = (StockKeeperRequestScreen) (Object) this;
        return ProcessingOrderContext.encodeSelectedRecipe(order, screen.recipesToOrder, level);
    }
}
