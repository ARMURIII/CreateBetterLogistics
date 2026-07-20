package com.yision.bettersaw.content;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.yision.bettersaw.BetterSawRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class SawBufferManager {

    private static final float LOCK_RETRY_TIME = 128 + 5;
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE_SAWS = new WeakHashMap<>();

    private SawBufferManager() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SawBufferManager::onLevelTickPre);
        NeoForge.EVENT_BUS.addListener(SawBufferManager::onLevelTickPost);
        NeoForge.EVENT_BUS.addListener(SawBufferManager::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(SawBufferManager::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(SawBufferManager::onLevelUnload);
    }

    static boolean hasBufferedInput(SawBlockEntity saw) {
        return saw.getExistingData(BetterSawRegistries.SAW_BUFFER)
            .filter(buffer -> !buffer.isEmpty())
            .isPresent();
    }

    static void storeBufferedInput(SawBlockEntity saw, ItemStack stack, @Nullable ResourceLocation recipeId) {
        if (stack.isEmpty() && recipeId == null) {
            clearAttachment(saw);
            untrack(saw);
            saw.setChanged();
            return;
        }

        saw.getData(BetterSawRegistries.SAW_BUFFER).replace(stack, recipeId);
        track(saw);
        saw.setChanged();
    }

    static ItemStack copyBufferedInput(SawBlockEntity saw) {
        return saw.getExistingData(BetterSawRegistries.SAW_BUFFER)
            .map(SawBuffer::copyRemaining)
            .orElse(ItemStack.EMPTY);
    }

    static ItemStack extractBufferedInput(SawBlockEntity saw, int amount, boolean simulate) {
        SawBuffer buffer = saw.getExistingData(BetterSawRegistries.SAW_BUFFER).orElse(null);
        if (buffer == null) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = buffer.extract(amount, simulate);
        if (simulate || extracted.isEmpty()) {
            return extracted;
        }

        if (!buffer.hasState()) {
            clearAttachment(saw);
            untrack(saw);
        }
        saw.setChanged();
        return extracted;
    }

    private static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<BlockPos> active = ACTIVE_SAWS.get(level);
        if (active == null || active.isEmpty()) {
            return;
        }

        for (BlockPos pos : active) {
            if (!level.isLoaded(pos)
                    || !(level.getBlockEntity(pos) instanceof SawBlockEntity saw)
                    || saw.inventory.isEmpty()
                    || saw.inventory.appliedRecipe) {
                continue;
            }

            Optional<ResourceLocation> recipeId = saw.getExistingData(BetterSawRegistries.SAW_BUFFER)
                .flatMap(SawBuffer::recipeId);
            if (recipeId.isEmpty()) {
                continue;
            }

            ItemStack activeInput = saw.inventory.getStackInSlot(0);
            if (!SawRecipeSelection.applyOrderedFilter(saw, recipeId.get(), activeInput)) {
                saw.inventory.remainingTime = Math.max(saw.inventory.remainingTime, LOCK_RETRY_TIME);
                saw.inventory.recipeDuration = Math.max(saw.inventory.recipeDuration, LOCK_RETRY_TIME);
                saw.setChanged();
            }
        }
    }

    private static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<BlockPos> active = ACTIVE_SAWS.get(level);
        if (active == null || active.isEmpty()) {
            return;
        }

        Iterator<BlockPos> iterator = active.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!level.isLoaded(pos)) {
                continue;
            }

            if (!(level.getBlockEntity(pos) instanceof SawBlockEntity saw)) {
                iterator.remove();
                continue;
            }

            SawBuffer buffer = saw.getExistingData(BetterSawRegistries.SAW_BUFFER).orElse(null);
            if (buffer == null || !buffer.hasState()) {
                clearAttachment(saw);
                iterator.remove();
                saw.setChanged();
                continue;
            }

            if (!saw.inventory.isEmpty()
                    || !saw.getBlockState().hasProperty(SawBlock.FACING)
                    || saw.getBlockState().getValue(SawBlock.FACING) != Direction.UP) {
                continue;
            }

            if (buffer.isEmpty()) {
                clearAttachment(saw);
                iterator.remove();
                saw.setChanged();
                continue;
            }

            ItemStack next = buffer.copyRemaining().copyWithCount(1);
            Optional<ResourceLocation> recipeId = buffer.recipeId();
            saw.inventory.setStackInSlot(0, next);
            if (recipeId.isPresent()
                    && !SawRecipeSelection.applyOrderedFilter(saw, recipeId.get(), next)) {
                saw.inventory.setStackInSlot(0, ItemStack.EMPTY);
                saw.setChanged();
                continue;
            }

            buffer.takeOne();
            if (!buffer.hasState()) {
                clearAttachment(saw);
                iterator.remove();
            }
            saw.start(next);
            saw.setChanged();
        }

        if (active.isEmpty()) {
            ACTIVE_SAWS.remove(level);
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof SawBlockEntity saw && hasBatchState(saw)) {
                ACTIVE_SAWS.computeIfAbsent(level, ignored -> new HashSet<>())
                    .add(saw.getBlockPos().immutable());
            }
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<BlockPos> active = ACTIVE_SAWS.get(level);
        if (active == null) {
            return;
        }

        int chunkX = event.getChunk().getPos().x;
        int chunkZ = event.getChunk().getPos().z;
        active.removeIf(pos -> pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ);
        if (active.isEmpty()) {
            ACTIVE_SAWS.remove(level);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ACTIVE_SAWS.remove(level);
        }
    }

    private static void track(SawBlockEntity saw) {
        if (saw.getLevel() instanceof ServerLevel level) {
            ACTIVE_SAWS.computeIfAbsent(level, ignored -> new HashSet<>())
                .add(saw.getBlockPos().immutable());
        }
    }

    private static void untrack(SawBlockEntity saw) {
        if (!(saw.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<BlockPos> active = ACTIVE_SAWS.get(level);
        if (active != null) {
            active.remove(saw.getBlockPos());
            if (active.isEmpty()) {
                ACTIVE_SAWS.remove(level);
            }
        }
    }

    private static void clearAttachment(SawBlockEntity saw) {
        saw.removeData(BetterSawRegistries.SAW_BUFFER);
    }

    private static boolean hasBatchState(SawBlockEntity saw) {
        return saw.getExistingData(BetterSawRegistries.SAW_BUFFER)
            .filter(SawBuffer::hasState)
            .isPresent();
    }
}
