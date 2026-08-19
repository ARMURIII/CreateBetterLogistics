package com.yision.bettersaw.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.yision.bettersaw.logistics.ProcessingOrderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public enum DeployingUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    public static final PackageItem.PackageOrderData REJECTED_ORDER_DATA = new PackageItem.PackageOrderData(-11,-11,false,-11,false,Optional.empty());

    public static boolean isRejectedOrder(PackageItem.PackageOrderData order) {
        return order.orderId() == -11 && order.linkIndex() == -11 && order.fragmentIndex() == -11;
    }

    public static void register() {
        UnpackingHandler.REGISTRY.register(AllBlocks.DEPLOYER.get(), INSTANCE);
    }

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
            @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof DeployerBlockEntity deployer))
            return false;

        Optional<ResourceLocation> orderedRecipeId = ProcessingOrderContext.decodeRecipeId(orderContext);
        logistic:
        if (orderedRecipeId.isPresent()) {
            var rejected = new ArrayList<>(items);

            if (orderContext != null && !orderContext.orderedCrafts().isEmpty()) {
                var deployed = orderContext.orderedCrafts().getFirst().pattern().stacks().getFirst();
                rejected.removeIf(stack -> stack.is(deployed.stack.getItem())&&(stack.getCount()==deployed.count || deployed.count == -1));
                UnpackingHandler.DEFAULT.unpack(
                        level,
                        pos,
                        state,
                        side,
                        new ArrayList<>(List.of(deployed.stack.copyWithCount(deployed.count))),
                        null,
                        simulate
                );
            } else
                break logistic;

            if (level.getBlockEntity(pos.relative(side)) instanceof PackagerBlockEntity pbe) {
                pbe.heldBox = PackageItem.containing(rejected);
                pbe.heldBox.set(AllDataComponents.PACKAGE_ORDER_DATA, REJECTED_ORDER_DATA);
                pbe.animationInward = false;

                pbe.notifyUpdate();
            }

            deployer.setChanged();
            return true;

        }

        return UnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, orderContext, simulate);
    }
}
