// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.config.Variator;
import com.aspctt.treadlightly.sound.AcousticVolumes;
import com.aspctt.treadlightly.sound.acoustic.AcousticLibrary;
import com.aspctt.treadlightly.sound.acoustic.AcousticsFile;
import com.aspctt.treadlightly.sound.acoustic.AcousticsPlayer;
import com.aspctt.treadlightly.sound.generator.Locomotion;
import com.aspctt.treadlightly.sound.player.SoundPlayer;
import com.aspctt.treadlightly.util.ResourceUtils;
import com.aspctt.treadlightly.world.BiomeVarianceLookup.BiomeVariance;

/**
 * Everything the enabled packs loaded. Replaced wholesale on reload rather than mutated, so a
 * reload mid-frame swaps one complete set for another instead of being seen half-applied.
 *
 * @param hasData      whether any pack supplied anything at all
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
        boolean hasData,
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
    private static final ResourceLocation BLOCK_MAP = TreadLightly.id("config/blockmap.json");
    private static final ResourceLocation BIOME_MAP = TreadLightly.id("config/biomevariancemap.json");
    private static final ResourceLocation GOLEM_MAP = TreadLightly.id("config/golemmap.json");
    private static final ResourceLocation LOCOMOTION_MAP = TreadLightly.id("config/locomotionmap.json");
    private static final ResourceLocation PRIMITIVE_MAP = TreadLightly.id("config/primitivemap.json");
    private static final ResourceLocation ACOUSTICS = TreadLightly.id("config/acoustics.json");
    private static final ResourceLocation VARIATOR = TreadLightly.id("config/variator.json");

    /** Per-entity block maps: one file per entity type, named after it. */
    private static final FileToIdConverter ENTITY_BLOCK_MAPS = FileToIdConverter.json("config/blockmaps/entity");

    /** Nothing loaded, for before the first reload. */
    public static Lookups empty(SoundPlayer soundPlayer, AcousticVolumes volumes, Supplier<Locomotion> playerStance) {
        return new Lookups(false, new Variator(), new LocomotionLookup(playerStance), new HeuristicStateLookup(),
                new Lookup<>(), new Lookup<>(), Map.of(), new BiomeVarianceLookup(), new Lookup<>(),
                new AcousticsPlayer(soundPlayer, volumes));
    }

    /**
     * Reads every config file from every enabled pack. Each map loads independently, so a pack
     * supplying only a block map works and a file that fails to parse costs only itself.
     */
    public static Lookups load(ResourceManager manager, SoundPlayer soundPlayer,
                               AcousticVolumes volumes, Supplier<Locomotion> playerStance) {
        Variator variator = new Variator();
        LocomotionLookup locomotions = new LocomotionLookup(playerStance);
        Lookup<EntityType<?>> golems = new Lookup<>();
        Lookup<BlockState> globalBlocks = new Lookup<>();
        BiomeVarianceLookup biomes = new BiomeVarianceLookup();
        Lookup<SoundEvent> primitives = new Lookup<>();
        AcousticsPlayer acoustics = new AcousticsPlayer(soundPlayer, volumes);

        boolean hasData = globalBlocks.load(ResourceUtils.load(BLOCK_MAP, manager, StateLookup::new));

        // Layered over the global map, so a per-entity file need only state its differences.
        Map<EntityType<?>, Lookup<BlockState>> blocks = ResourceUtils.loadDir(
                ENTITY_BLOCK_MAPS, manager, StateLookup::new,
                id -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null),
                entries -> {
                    Lookup<BlockState> lookup = new Lookup<>();
                    return lookup.load(entries, globalBlocks) ? lookup : null;
                });

        hasData |= !blocks.isEmpty();
        hasData |= ResourceUtils.forEach(BIOME_MAP, manager, biomes::load);
        hasData |= golems.load(ResourceUtils.load(GOLEM_MAP, manager, GolemLookup::new));
        hasData |= primitives.load(ResourceUtils.load(PRIMITIVE_MAP, manager, PrimitiveLookup::new));
        hasData |= ResourceUtils.forEach(LOCOMOTION_MAP, manager, locomotions::load);
        hasData |= ResourceUtils.forEach(ACOUSTICS, manager,
                reader -> AcousticsFile.read(reader, acoustics::addAcoustic, false));
        hasData |= ResourceUtils.forEach(VARIATOR, manager, variator::load);

        return new Lookups(hasData, variator, locomotions, new HeuristicStateLookup(),
                golems, globalBlocks, blocks, biomes, primitives, acoustics);
    }

    /**
     * The block map to use for a given entity. Players always use the global one: a pack
     * overriding sounds for, say, a creeper should not change what the player hears underfoot.
     */
    public Lookup<BlockState> blocksFor(EntityType<?> sourceType) {
        if (sourceType == EntityType.PLAYER) {
            return globalBlocks;
        }
        return blocks.getOrDefault(sourceType, globalBlocks);
    }
}
