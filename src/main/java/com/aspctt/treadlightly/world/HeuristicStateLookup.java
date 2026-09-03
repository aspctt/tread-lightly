// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;

/**
 * A last resort for modded blocks nothing else matched.
 * <p>
 * Blocks that use vanilla's grass step sound are a mixed bag: real grass, but also leaves,
 * saplings, and crops, which should not all sound alike. Where the block's name suggests a
 * leaf, this finds the vanilla leaf block it is named after so the block map entry for that
 * can stand in. A modded {@code cherry_leaves} resolves through {@code minecraft:cherry_leaves}.
 * <p>
 * Answers are memoised because the name splitting and registry probing are far too expensive
 * to repeat per footstep, and cannot change without a reload.
 */
public class HeuristicStateLookup {
    private final Function<Block, Optional<Block>> leafBlockCache = Util.memoize(block -> {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return Stream.of(id.getPath())
            .flatMap(path -> Arrays.stream(path.split("_")))
            .map(part -> ResourceLocation.tryParse(part + "_leaves"))
            .filter(candidate -> candidate != null)
            .flatMap(candidate -> BuiltInRegistries.BLOCK.getOptional(candidate).stream())
            .findFirst();
    });

    /**
     * @return a vanilla block that should sound the same, or null if nothing sensible applies
     */
    @Nullable
    @SuppressWarnings("deprecation") // See below.
    public Block getMostSimilar(Block block) {
        // NeoForge's getSoundType(LevelReader, BlockPos, Entity) is the call to prefer wherever
        // a position is in hand, since blocks may vary their sound by where they are. This asks
        // a question about a block type in the abstract, with no position to offer, so the raw
        // accessor is the correct one rather than a shortcut around the better API.
        if (block.defaultBlockState().getSoundType().getStepSound() == SoundEvents.GRASS_STEP) {
            return leafBlockCache.apply(block).orElse(null);
        }
        return null;
    }
}
