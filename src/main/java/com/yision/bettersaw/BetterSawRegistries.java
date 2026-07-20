package com.yision.bettersaw;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.yision.bettersaw.content.SawBuffer;
import com.yision.bettersaw.content.SawBufferLootModifier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BetterSawRegistries {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateBetterSaw.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CreateBetterSaw.MOD_ID);

    public static final Supplier<AttachmentType<SawBuffer>> SAW_BUFFER = ATTACHMENT_TYPES.register(
        "saw_buffer",
        () -> AttachmentType.builder(SawBuffer::new)
            .serialize(SawBuffer.CODEC, SawBuffer::hasState)
            .build()
    );

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> SAW_BUFFER_LOOT_MODIFIER =
        LOOT_MODIFIER_SERIALIZERS.register("saw_buffer", () -> SawBufferLootModifier.CODEC);

    private BetterSawRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
