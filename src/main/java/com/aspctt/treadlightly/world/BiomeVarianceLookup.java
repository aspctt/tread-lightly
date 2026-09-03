// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Per-biome volume and pitch trim, so the same block can read differently in a cave than in a
 * meadow. Values are eased towards rather than snapped to as an entity crosses a border.
 */
public class BiomeVarianceLookup implements Index<ResourceLocation, BiomeVarianceLookup.BiomeVariance> {
    private final Map<ResourceLocation, BiomeVariance> entries = new Object2ObjectOpenHashMap<>();

    @Override
    public BiomeVariance lookup(ResourceLocation key) {
        return entries.getOrDefault(key, BiomeVariance.DEFAULT);
    }

    @Override
    public boolean contains(ResourceLocation key) {
        return entries.containsKey(key);
    }

    @Override
    public void add(String key, JsonElement value) {
        BiomeVariance.CODEC.decode(JsonOps.INSTANCE, value)
                .result()
                .map(Pair::getFirst)
                .ifPresent(variance -> entries.put(ResourceLocation.parse(key), variance));
    }

    public record BiomeVariance(float volume, float pitch) {
        public static final BiomeVariance DEFAULT = new BiomeVariance(1, 1);

        static final Codec<BiomeVariance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("volume").forGetter(BiomeVariance::volume),
                Codec.FLOAT.fieldOf("pitch").forGetter(BiomeVariance::pitch)
        ).apply(instance, BiomeVariance::new));
    }
}
