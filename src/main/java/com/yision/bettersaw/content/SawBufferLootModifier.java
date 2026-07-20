package com.yision.bettersaw.content;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.yision.bettersaw.BetterSawRegistries;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class SawBufferLootModifier extends LootModifier {
    public static final MapCodec<SawBufferLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance).apply(instance, SawBufferLootModifier::new)
    );

    public SawBufferLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof SawBlockEntity saw)) {
            return generatedLoot;
        }

        ItemStack buffered = SawBufferManager.copyBufferedInput(saw);
        if (!buffered.isEmpty()) {
            generatedLoot.add(buffered);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return BetterSawRegistries.SAW_BUFFER_LOOT_MODIFIER.get();
    }
}
