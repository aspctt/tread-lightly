// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.config.Variator;
import com.aspctt.treadlightly.sound.acoustic.AcousticLibrary;
import com.aspctt.treadlightly.sound.generator.Locomotion;
import com.aspctt.treadlightly.world.BiomeVarianceLookup.BiomeVariance;

/**
 * Everything the enabled packs loaded, in one place.
 * <p>
 * Held and replaced wholesale on resource reload rather than mutated, so a reload happening
 * while the client is mid-frame swaps one complete set for another instead of being seen
 * half-applied.
 *
 * @param variator     stride lengths and timings the generator works from
 * @param locomotions  which gait each entity type walks with
 * @param heuristics   name-based guesses for modded blocks nothing else matched
 * @param golems       sounds for standing on entities
 * @param globalBlocks the block map every entity uses unless overridden
 * @param blocks       per-entity-type block maps, layered over the global one
 * @param biomes       per-biome volume and pitch trim
 * @param primitives   fallback by vanilla sound type, which covers most modded blocks
 * @param acoustics    the named acoustics all of the above resolve to
 */
public record Lookups(
        Variator variator,
        Index<Entity, Locomotion> locomotions,
        HeuristicStateLookup heuristics,
        Lookup<EntityType<?>> golems,
        Lookup<BlockState> globalBlocks,
        Map<EntityType<?>, Lookup<BlockState>> blocks,
        Index<ResourceLocation, BiomeVariance> biomes,
        Lookup<SoundEvent> primitives,
        AcousticLibrary acoustics
) {
    /**
     * The block map to use for a given entity.
     * <p>
     * Players always use the global map. A pack overriding sounds for, say, a creeper should
     * not quietly change what the player hears underfoot.
     */
    public Lookup<BlockState> blocksFor(EntityType<?> sourceType) {
        if (sourceType == EntityType.PLAYER) {
            return globalBlocks;
        }
        return blocks.getOrDefault(sourceType, globalBlocks);
    }
}
