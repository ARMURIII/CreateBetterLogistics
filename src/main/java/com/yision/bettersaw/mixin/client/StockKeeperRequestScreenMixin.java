package com.yision.bettersaw.mixin.client;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderRequestPacket;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.yision.bettersaw.logistics.SawOrderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {
    @ModifyArg(
        method = "sendIt",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderRequestPacket;<init>(Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Ljava/lang/String;Z)V",
            remap = false
        ),
        index = 1,
        remap = false
    )
    private PackageOrderWithCrafts bettersaw$encodeSawRecipe(PackageOrderWithCrafts original) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return original;
        }

        StockKeeperRequestScreen screen = (StockKeeperRequestScreen) (Object) this;
        return SawOrderContext.encodeSelectedSawRecipe(original, screen.recipesToOrder, level);
    }
}
